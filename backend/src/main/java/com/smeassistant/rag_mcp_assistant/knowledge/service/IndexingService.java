package com.smeassistant.rag_mcp_assistant.knowledge.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IndexingService {

    private final ChunkingService chunkingService;
    private final VectorStore vectorStore;

    public IndexingService(ChunkingService chunkingService, VectorStore vectorStore) {
        this.chunkingService = chunkingService;
        this.vectorStore = vectorStore;
    }

    /**
     * Indexe un document : chunking → embedding → stockage dans pgvector
     *
     * @param documentId   ID du document en base
     * @param filename     Nom du fichier (pour les métadonnées)
     * @param content      Contenu texte complet
     */
    public void indexDocument(Long documentId, String filename, String content) {
        // 1. Découpage
        List<String> chunks = chunkingService.chunk(content);

        // 2. Transformation en Documents Spring AI
        List<Document> documents = chunks.stream()
                .map(chunk -> new Document(
                        chunk,
                        Map.of(
                                "documentId", documentId.toString(),
                                "filename", filename
                        )
                ))
                .collect(Collectors.toList());

        // 3. Stockage dans pgvector (Spring AI gère l'embedding automatiquement)
        vectorStore.add(documents);

        System.out.println("→ Document '" + filename + "' indexé avec " + chunks.size() + " chunks");
    }

    /**
     * Supprime les vecteurs d'un document du store pgvector.
     *
     * @param documentId ID du document en base
     */
    public void deleteDocumentVectors(Long documentId) {
        FilterExpressionBuilder filter = new FilterExpressionBuilder();
        vectorStore.delete(filter.eq("documentId", documentId.toString()).build());
        System.out.println("→ Vecteurs du document " + documentId + " supprimés de pgvector");
    }
}
