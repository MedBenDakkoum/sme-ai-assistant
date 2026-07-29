package com.smeassistant.rag_mcp_assistant.knowledge.service;

import com.smeassistant.rag_mcp_assistant.llm.LlmClient;
import com.smeassistant.rag_mcp_assistant.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final SearchService searchService;
    private final LlmClient llmClient;
    private final NotificationService notificationService;

    public RagService(SearchService searchService, LlmClient llmClient, NotificationService notificationService) {
        this.searchService = searchService;
        this.llmClient = llmClient;
        this.notificationService = notificationService;
    }

    public String ask(String question) {
        return ask(question, null, null);
    }

    public String ask(String question, String userEmail, String sessionId) {
        // 1. Récupérer les chunks pertinents
        List<String> relevantChunks = searchService.search(question, 4);

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

        // 3. System prompt strict (réduction d'hallucinations)
        String systemPrompt = """
                Tu es un assistant IA pour une PME.
                Tu dois répondre UNIQUEMENT à partir des documents fournis ci-dessous.
                Si l'information n'est pas présente dans les documents, dis clairement que tu ne sais pas.
                N'invente jamais d'information.
                Réponds de manière claire, concise et professionnelle.
                """;

        // 4. User prompt
        String userPrompt = """
                Voici les documents de référence :

                %s

                ---

                Question de l'utilisateur : %s
                """.formatted(context, question);

        // 5. Appel au LLM
        String answer = llmClient.generate(systemPrompt, userPrompt);

        // 6. Détection "Je ne sais pas" renvoyé par le LLM → escalade email
        if (isUnknownAnswer(answer)) {
            log.info("Le LLM a indiqué ne pas savoir pour la question : {}", question);
            notificationService.notifyNoAnswer(question, userEmail, sessionId);
        }

        return answer;
    }

    /**
     * Détecte si le LLM a répondu qu'il ne savait pas.
     * Compare en lowercase et tolère les variations courantes.
     */
    private boolean isUnknownAnswer(String answer) {
        if (answer == null) return true;
        String normalized = answer.toLowerCase(Locale.ROOT).trim();
        return normalized.startsWith("je ne sais pas")
                || normalized.startsWith("je n'ai pas d'information")
                || normalized.startsWith("je n'ai pas d'informations")
                || normalized.startsWith("je n'ai pas la réponse")
                || normalized.contains("je ne dispose pas")
                || normalized.contains("information non disponible")
                || normalized.equals("je ne sais pas.")
                || normalized.equals("je ne sais pas");
    }
}