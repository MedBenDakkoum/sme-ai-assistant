package com.smeassistant.rag_mcp_assistant.knowledge.service;

import com.smeassistant.rag_mcp_assistant.llm.LlmClient;
import com.smeassistant.rag_mcp_assistant.notification.service.NotificationService;
import io.modelcontextprotocol.client.McpSyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    // Phrases cles d'incomprehension en plusieurs langues (lowercase, trimmed)
    private static final Set<String> UNKNOWN_ANSWERS = Set.of(
            // Francais
            "je ne sais pas", "je ne sais pas.", "je sais pas", "je sais pas.",
            "je n'ai pas d'information", "je n'ai pas d'informations",
            "je n'ai pas la r\u00e9ponse", "je n'ai pas les informations",
            "je ne dispose pas", "information non disponible",
            "aucune information", "aucune information pertinente",
            "aucune information disponible", "je ne peux pas r\u00e9pondre",
            "d\u00e9sol\u00e9, je ne sais pas", "malheureusement, je ne sais pas",
            // Anglais
            "i don't know", "i don't know.", "i do not know",
            "i don't have information", "i don't have any information",
            "i don't have the information", "i don't have the answer",
            "no information", "no relevant information", "not available",
            "information not available", "i'm unable to answer",
            "i cannot answer", "i can't answer", "sorry, i don't know",
            "unfortunately, i don't know", "i'm not sure", "i am not sure"
    );

    private final LlmClient llmClient;
    private final NotificationService notificationService;
    private final LanguageDetectionService languageDetectionService;
    private final McpSyncClient mcpSyncClient;

    public RagService(LlmClient llmClient,
                      NotificationService notificationService,
                      LanguageDetectionService languageDetectionService,
                      // @Lazy : le client MCP est initialise seulement a la premiere requete,
                      // une fois que le serveur HTTP embarque (qui heberge le MCP server)
                      // a demarre. Sans ce Lazy, le client tente de se connecter pendant
                      // le refresh du contexte, avant que Tomcat ne soit a l'ecoute.
                      @Lazy McpSyncClient mcpSyncClient) {
        this.llmClient = llmClient;
        this.notificationService = notificationService;
        this.languageDetectionService = languageDetectionService;
        this.mcpSyncClient = mcpSyncClient;
    }

    public String ask(String question) {
        return ask(question, null, null);
    }

    public String ask(String question, String userEmail, String sessionId) {
        // 1. Detection de la langue AVANT construction du prompt
        String languageCode = languageDetectionService.detect(question);
        String targetLanguage = "en".equals(languageCode) ? "ENGLISH" : "FRENCH";

        // 2. Recuperation des tool callbacks du serveur MCP local
        //    (search_knowledge_base via Streamable HTTP).
        //    Le LLM deciderra lui-meme s'il doit appeler le tool et avec quels arguments.
        ToolCallback[] tools;
        try {
            SyncMcpToolCallbackProvider provider = new SyncMcpToolCallbackProvider(List.of(mcpSyncClient));
            ToolCallback[] rawTools = provider.getToolCallbacks();
            // Decorate les callbacks avec un logger pur : trace uniquement,
            // s'applique seulement aux callbacks issus du client MCP
            // (McpSyncClient / SyncMcpToolCallbackProvider).
            tools = Arrays.stream(rawTools)
                    .map(LoggingToolCallback::new)
                    .toArray(ToolCallback[]::new);
            log.info("MCP tools exposes au LLM pour cette requete : {}", tools.length);
        } catch (Exception e) {
            log.warn("Failed to get MCP tools, LLM will be called without tools: {}", e.getMessage());
            tools = new ToolCallback[0];
        }

        // 3. Court-circuit anti-hallucination : si aucun tool MCP n'est disponible
        //    (echec du client MCP), on NE doit JAMAIS appeler le LLM sans tool —
        //    il pourrait halluciner un faux tool-call. Reponse par defaut + escalade.
        if (tools.length == 0) {
            log.info("[MCP_UNAVAILABLE] Aucun tool disponible, court-circuit vers reponse par defaut + escalade");
            notificationService.notifyNoAnswer(question, userEmail, sessionId);
            return "Je ne sais pas.";
        }

        // 4. System prompt strict — anti-hallucination renforcee par l'usage obligatoire du tool
        String systemPrompt = """
                You are an AI assistant for a SME.

                === RULE #0 (ABSOLUTE PRIORITY): GROUNDING IN TOOL RESULTS ===
                You MUST answer ONLY using information returned by the search_knowledge_base tool.
                If the tool returns no results, or the results do not contain the information needed
                to answer the user's question, you MUST reply exactly with "Je ne sais pas" (or the
                English equivalent "I don't know" if the question is in English).
                NEVER invent, infer, or supplement information that is not present in the tool's output.
                NEVER use your general knowledge. Your training data is irrelevant here.

                === HOW TO USE THE TOOL ===
                For every factual question, you MUST first invoke the search_knowledge_base tool
                with the user's question (or a reformulated version of it) as the `query` parameter.
                Do not answer until you have received the tool's response.
                If the tool returns an empty list, that means the knowledge base contains no relevant
                information — you must answer "Je ne sais pas" / "I don't know".

                === RULE #1 (ABSOLUTE PRIORITY): LANGUAGE OF RESPONSE ===
                The user's question has been pre-detected as %s language.
                You MUST write your entire response in %s.
                This instruction overrides any default language behavior. No exceptions.
                Keep numbers, dates, proper names, acronyms, and technical codes exactly as they appear
                in the tool results.

                === STYLE ===
                Answer clearly, concisely, and professionally.
                """.formatted(targetLanguage, targetLanguage);

        // 5. Rappel critique place a l'extreme fin du user prompt (Recency Bias)
        String languageReminder = "en".equals(languageCode)
                ? "[CRITICAL REMINDER: Write your entire response in ENGLISH. Detected language of the question: en.]"
                : "[CRITICAL REMINDER: Write your entire response in FRENCH. Langue d\u00e9tect\u00e9e de la question : fr.]";

        String userPrompt = """
                %s

                User Question: %s
                """.formatted(languageReminder, question);

        // 6. Appel au LLM — il decide d'appeler search_knowledge_base via les MCP tools fournis
        log.info("[RAG PATH] LLM call WITH {} MCP tool(s) — search_knowledge_base is available", tools.length);
        String answer = llmClient.generate(systemPrompt, userPrompt, tools);

        log.info("Reponse LLM pour la question \"{}\" : {}", question, answer);

        // 7. Detection multilingue "Je ne sais pas" → escalade email
        if (isUnknownAnswer(answer)) {
            log.info("Le LLM a indique ne pas savoir pour la question : {}", question);
            notificationService.notifyNoAnswer(question, userEmail, sessionId);
        }

        return answer;
    }

    /**
     * Detecte si le LLM a repondu qu'il ne savait pas, dans n'importe quelle langue supportee.
     * Compare en lowercase, trim, et matching exact ou par prefixe.
     */
    private boolean isUnknownAnswer(String answer) {
        if (answer == null || answer.isBlank()) return true;
        String normalized = answer.toLowerCase(Locale.ROOT).trim();

        // Match exact
        if (UNKNOWN_ANSWERS.contains(normalized)) return true;

        // Match par prefixe (au cas ou le LLM ajoute une ponctuation ou un mot apres)
        for (String phrase : UNKNOWN_ANSWERS) {
            if (normalized.startsWith(phrase)) {
                if (normalized.length() == phrase.length()) return true;
                char nextChar = normalized.charAt(phrase.length());
                if (Character.isWhitespace(nextChar) || nextChar == '.' || nextChar == ',' || nextChar == '!' || nextChar == '?') {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Decorate de logging pur (aucune logique metier modifiee) : il entoure un
     * {@link ToolCallback} issu de {@code SyncMcpToolCallbackProvider} et trace a
     * niveau DEBUG chaque invocation du tool COTE CLIENT MCP — c'est-a-dire quand le
     * LLM declenche reellement l'appel qui transite par
     * {@code McpSyncClient}/{@code SyncMcpToolCallbackProvider}.
     */
    private static final class LoggingToolCallback implements ToolCallback {

        private final ToolCallback delegate;

        LoggingToolCallback(ToolCallback delegate) {
            this.delegate = delegate;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        @Override
        public String call(String toolInput) {
            return invoke(toolInput, null);
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            return invoke(toolInput, toolContext);
        }

        private String invoke(String toolInput, ToolContext toolContext) {
            String toolName = delegate.getToolDefinition().name();
            log.debug("[MCP_INVOCATION] LLM invoked MCP tool '{}' | args: {}", toolName, truncate(toolInput, 300));
            try {
                String result = (toolContext != null)
                        ? delegate.call(toolInput, toolContext)
                        : delegate.call(toolInput);
                log.debug("[MCP_INVOCATION] MCP tool '{}' returned: {}", toolName, truncate(result, 300));
                return result;
            } catch (Exception e) {
                log.warn("[MCP_INVOCATION] MCP tool '{}' FAILED: {}", toolName, e.getMessage());
                throw e;
            }
        }

        private static String truncate(String text, int max) {
            if (text == null) return "null";
            return text.length() <= max ? text : text.substring(0, max) + "...";
        }
    }
}
