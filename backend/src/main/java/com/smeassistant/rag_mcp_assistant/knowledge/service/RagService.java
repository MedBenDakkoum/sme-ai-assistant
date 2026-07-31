package com.smeassistant.rag_mcp_assistant.knowledge.service;

import com.smeassistant.rag_mcp_assistant.llm.LlmClient;
import com.smeassistant.rag_mcp_assistant.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    // Phrases clés d'incompréhension en plusieurs langues (lowercase, trimmed)
    private static final Set<String> UNKNOWN_ANSWERS = Set.of(
            // Français
            "je ne sais pas", "je ne sais pas.", "je sais pas", "je sais pas.",
            "je n'ai pas d'information", "je n'ai pas d'informations",
            "je n'ai pas la réponse", "je n'ai pas les informations",
            "je ne dispose pas", "information non disponible",
            "aucune information", "aucune information pertinente",
            "aucune information disponible", "je ne peux pas répondre",
            "désolé, je ne sais pas", "malheureusement, je ne sais pas",
            // Anglais
            "i don't know", "i don't know.", "i do not know",
            "i don't have information", "i don't have any information",
            "i don't have the information", "i don't have the answer",
            "no information", "no relevant information", "not available",
            "information not available", "i'm unable to answer",
            "i cannot answer", "i can't answer", "sorry, i don't know",
            "unfortunately, i don't know", "i'm not sure", "i am not sure"
    );

    private final SearchService searchService;
    private final LlmClient llmClient;
    private final NotificationService notificationService;
    private final LanguageDetectionService languageDetectionService;

    public RagService(SearchService searchService, LlmClient llmClient, NotificationService notificationService,
                      LanguageDetectionService languageDetectionService) {
        this.searchService = searchService;
        this.llmClient = llmClient;
        this.notificationService = notificationService;
        this.languageDetectionService = languageDetectionService;
    }

    public String ask(String question) {
        return ask(question, null, null);
    }

    public String ask(String question, String userEmail, String sessionId) {
        // 1. Récupérer les chunks pertinents
        List<String> relevantChunks = searchService.search(question, 4);

        // ===== DIAGNOSTIC TEMPORAIRE =====
        log.info("=== DIAGNOSTIC CHUNKS pour question: \"{}\" ===", question);
        log.info("Nombre de chunks récupérés: {}", relevantChunks.size());
        for (int i = 0; i < relevantChunks.size(); i++) {
            log.info("--- CHUNK #{} (rang {} sur {}) ---\n{}\n--- FIN CHUNK #{} ---",
                    i + 1, i + 1, relevantChunks.size(), relevantChunks.get(i), i + 1);
        }
        log.info("=== FIN DIAGNOSTIC CHUNKS ===");
        // =================================

        if (relevantChunks.isEmpty()) {
            // Cas 1 : aucun chunk trouvé → escalade immédiate
            log.info("Aucun chunk trouvé pour la question : {}", question);
            notificationService.notifyNoAnswer(question, userEmail, sessionId);
            return "Je n'ai trouvé aucune information pertinente dans la base de connaissances pour répondre à cette question.";
        }

        // 2. Construire le contexte
        String context = relevantChunks.stream()
                .map(chunk -> "- " + chunk)
                .collect(Collectors.joining("\n\n"));

        // 3. Détection de la langue AVANT construction du prompt
        String languageCode = languageDetectionService.detect(question);
        String targetLanguage = "en".equals(languageCode) ? "ENGLISH" : "FRENCH";

        // System prompt strict (neutralisé en anglais pour éviter le biais de langue initial)
        String systemPrompt = """
                You are an AI assistant for a SME.
                You must answer ONLY using the provided reference documents.
                If the information is not present in the documents, clearly state that you do not know.
                Never invent information.
                Answer clearly, concisely, and professionally.

                === RULE #1 (ABSOLUTE PRIORITY): LANGUAGE OF RESPONSE ===
                The user's question has been pre-detected as %s language.
                You MUST write your entire response in %s.
                This instruction overrides any default language behavior. No exceptions.
                Keep numbers, dates, proper names, acronyms, and technical codes exactly as they appear in the source documents.
                """.formatted(targetLanguage, targetLanguage);

        // 4. User prompt avec rappel critique placé à l'extrême fin (Recency Bias)
        String languageReminder = "en".equals(languageCode)
                ? "[CRITICAL REMINDER: Write your entire response in ENGLISH. Detected language of the question: en.]"
                : "[CRITICAL REMINDER: Write your entire response in FRENCH. Langue détectée de la question : fr.]";

        String userPrompt = """
                Here are the reference documents:

                %s

                ---

                User Question: %s

                %s
                """.formatted(context, question, languageReminder);

        // 5. Appel au LLM
        String answer = llmClient.generate(systemPrompt, userPrompt);

        // 6. Détection multilingue "Je ne sais pas" → escalade email
        if (isUnknownAnswer(answer)) {
            log.info("Le LLM a indiqué ne pas savoir pour la question : {}", question);
            notificationService.notifyNoAnswer(question, userEmail, sessionId);
        }

        return answer;
    }

    /**
     * Détecte si le LLM a répondu qu'il ne savait pas, dans n'importe quelle langue supportée.
     * Compare en lowercase, trim, et matching exact ou par préfixe.
     */
    private boolean isUnknownAnswer(String answer) {
        if (answer == null || answer.isBlank()) return true;
        String normalized = answer.toLowerCase(Locale.ROOT).trim();

        // Match exact
        if (UNKNOWN_ANSWERS.contains(normalized)) return true;

        // Match par préfixe (au cas où le LLM ajoute une ponctuation ou un mot après)
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
}