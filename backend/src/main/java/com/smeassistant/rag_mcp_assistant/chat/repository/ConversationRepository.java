package com.smeassistant.rag_mcp_assistant.chat.repository;

import com.smeassistant.rag_mcp_assistant.chat.model.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<ConversationEntity, String> {
}
