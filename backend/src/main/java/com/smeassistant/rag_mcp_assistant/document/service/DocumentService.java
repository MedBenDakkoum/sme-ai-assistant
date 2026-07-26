package com.smeassistant.rag_mcp_assistant.document.service;

import com.smeassistant.rag_mcp_assistant.document.model.DocumentEntity;
import com.smeassistant.rag_mcp_assistant.document.repository.DocumentRepository;
import com.smeassistant.rag_mcp_assistant.knowledge.service.IndexingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final IndexingService indexingService;

    public DocumentService(DocumentRepository documentRepository, IndexingService indexingService) {
        this.documentRepository = documentRepository;
        this.indexingService = indexingService;
    }

    @Transactional
    public DocumentEntity uploadAndIndex(MultipartFile file) throws IOException {
        // 1. Lire le contenu du fichier
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);

        // 2. Sauvegarder en base
        DocumentEntity document = new DocumentEntity();
        document.setFilename(file.getOriginalFilename());
        document.setContentType(file.getContentType() != null ? file.getContentType() : "text/plain");
        document.setContent(content);
        document.setIndexed(false);

        document = documentRepository.save(document);

        // 3. Indexer (chunking + embedding + pgvector)
        indexingService.indexDocument(document.getId(), document.getFilename(), content);

        // 4. Marquer comme indexé
        document.setIndexed(true);
        return documentRepository.save(document);
    }

    public List<DocumentEntity> findAll() {
        return documentRepository.findAll();
    }
}
