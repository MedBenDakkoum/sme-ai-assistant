package com.smeassistant.rag_mcp_assistant.document.service;

import com.smeassistant.rag_mcp_assistant.document.model.DocumentEntity;
import com.smeassistant.rag_mcp_assistant.document.repository.DocumentRepository;
import com.smeassistant.rag_mcp_assistant.knowledge.service.IndexingService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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
        // 1. Extraire le contenu selon le type de fichier (txt brut / pdf via PDFBox)
        String content = extractContent(file);

        // 2. Sauvegarder en base
        DocumentEntity document = new DocumentEntity();
        document.setFilename(file.getOriginalFilename());
        document.setContentType(resolveContentType(file));
        document.setContent(content);
        document.setIndexed(false);

        document = documentRepository.save(document);

        // 3. Indexer (chunking + embedding + pgvector)
        indexingService.indexDocument(document.getId(), document.getFilename(), content);

        // 4. Marquer comme indexé
        document.setIndexed(true);
        return documentRepository.save(document);
    }

    private String extractContent(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        if (isPdf(file)) {
            String text;
            try (PDDocument pdf = Loader.loadPDF(bytes)) {
                text = new PDFTextStripper().getText(pdf);
            } catch (IOException | RuntimeException e) {
                throw new IOException("Impossible de lire le fichier PDF '" + file.getOriginalFilename()
                        + "'. Vérifiez qu'il n'est pas corrompu ou scanné sans texte extractible.", e);
            }
            if (text == null || text.isBlank()) {
                throw new IOException("Le PDF '" + file.getOriginalFilename()
                        + "' ne contient pas de texte extractible (PDF scanné ?).");
            }
            return text;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private boolean isPdf(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name != null && name.toLowerCase().endsWith(".pdf")) {
            return true;
        }
        return "application/pdf".equalsIgnoreCase(file.getContentType());
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        return isPdf(file) ? "application/pdf" : "text/plain";
    }

    public List<DocumentEntity> findAll() {
        return documentRepository.findAll();
    }

    @Transactional
    public boolean deleteDocument(Long id) {
        if (!documentRepository.existsById(id)) {
            return false;
        }
        indexingService.deleteDocumentVectors(id);
        documentRepository.deleteById(id);
        return true;
    }
}
