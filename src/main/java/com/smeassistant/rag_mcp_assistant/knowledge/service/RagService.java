package com.smeassistant.rag_mcp_assistant.knowledge.service;

import com.smeassistant.rag_mcp_assistant.llm.LlmClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final SearchService searchService;
    private final LlmClient llmClient;

    public RagService(SearchService searchService, LlmClient llmClient) {
        this.searchService = searchService;
        this.llmClient = llmClient;
    }

    public String ask(String question) {
        // 1. Récupérer les chunks pertinents
        List<String> relevantChunks = searchService.search(question, 4);

        if (relevantChunks.isEmpty()) {
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
                Ne invente jamais d'information.
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
        return llmClient.generate(systemPrompt, userPrompt);
    }
}
