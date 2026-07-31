package com.smeassistant.rag_mcp_assistant.knowledge.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final VectorStore vectorStore;
    private final double similarityThreshold;

    public SearchService(VectorStore vectorStore,
                         @Value("${app.rag.similarity-threshold:0.75}") double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.similarityThreshold = similarityThreshold;
    }

    /**
     * Recherche les chunks les plus similaires à la question.
     * Filtre les chunks dont le score de similarité est inférieur au seuil configuré.
     *
     * @param query  La question de l'utilisateur
     * @param topK   Nombre maximum de chunks à retourner (avant filtrage)
     * @return Liste des contenus des chunks pertinents (peut être vide si aucun ne passe le seuil)
     */
    public List<String> search(String query, int topK) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .build()
        );

        // Filtre par seuil de pertinence (cosine distance : 0 = identique, 1 = opposé)
        // Spring AI PgVectorStore utilise COSINE_DISTANCE → on filtre les chunks
        // dont la distance est > (1 - threshold), c-à-d similarité < threshold
        double maxDistance = 1.0 - similarityThreshold;

        return results.stream()
                .filter(doc -> {
                    Object distObj = doc.getMetadata().get("distance");
                    if (distObj == null) return true; // pas de score dispo → on garde
                    double distance;
                    if (distObj instanceof Number n) {
                        distance = n.doubleValue();
                    } else if (distObj instanceof String s) {
                        try {
                            distance = Double.parseDouble(s);
                        } catch (NumberFormatException e) {
                            return true; // format inconnu → on garde
                        }
                    } else {
                        return true; // type inconnu → on garde (safe default)
                    }
                    return distance <= maxDistance;
                })
                .map(Document::getText)
                .collect(Collectors.toList());
    }
}
