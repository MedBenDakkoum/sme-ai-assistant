package com.smeassistant.rag_mcp_assistant.chat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "conversations")
@Getter
@Setter
public class ConversationEntity {

    @Id
    private String id;                    // = sessionId

    private String userEmail;

    @Column(nullable = false)
    private String channel = "web";       // web | email | whatsapp

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
