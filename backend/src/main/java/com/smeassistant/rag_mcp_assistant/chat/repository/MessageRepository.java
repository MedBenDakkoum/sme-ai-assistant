package com.smeassistant.rag_mcp_assistant.chat.repository;

import com.smeassistant.rag_mcp_assistant.chat.model.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    // traverse conversation.id -> conversation_id
    List<MessageEntity> findByConversationIdOrderByCreatedAtAsc(String conversationId);
}
