package com.smeassistant.rag_mcp_assistant.document.repository;

import com.smeassistant.rag_mcp_assistant.document.model.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
}
