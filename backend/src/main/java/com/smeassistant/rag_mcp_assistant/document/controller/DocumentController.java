package com.smeassistant.rag_mcp_assistant.document.controller;

import com.smeassistant.rag_mcp_assistant.document.model.DocumentEntity;
import com.smeassistant.rag_mcp_assistant.document.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            DocumentEntity saved = documentService.uploadAndIndex(file);
            return ResponseEntity.ok(Map.of(
                    "id", saved.getId(),
                    "filename", saved.getFilename(),
                    "indexed", saved.isIndexed(),
                    "message", "Document uploadé et indexé avec succès"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping
    public List<DocumentEntity> list() {
        return documentService.findAll();
    }
}
