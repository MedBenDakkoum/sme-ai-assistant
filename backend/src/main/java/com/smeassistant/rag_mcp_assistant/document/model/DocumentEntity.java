package com.smeassistant.rag_mcp_assistant.document.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter
@Setter
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String contentType;

    @Column(columnDefinition = "TEXT")
    private String content;          // contenu texte extrait

    private LocalDateTime uploadedAt = LocalDateTime.now();

    private boolean indexed = false; // true une fois vectorisé
}
