package com.smeassistant.rag_mcp_assistant.knowledge.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final VectorStore vectorStore;

    public SearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Recherche les chunks les plus similaires à la question.
     *
     * @param query  La question de l'utilisateur
     * @param topK   Nombre de chunks à retourner
     * @return Liste des contenus des chunks trouvés
     */
    public List<String> search(String query, int topK) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .build()
        );

        return results.stream()
                .map(Document::getText)
                .collect(Collectors.toList());
    }
}
