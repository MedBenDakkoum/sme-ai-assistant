package com.smeassistant.rag_mcp_assistant.chat.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "messages")
@Getter
@Setter
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @Column(nullable = false)
    private String role;                  // "user" | "assistant"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;               // pas de limite Varchar

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
