package com.smeassistant.rag_mcp_assistant.notification.model;

import java.time.Instant;

public record EscalationEvent(
        String question,
        String userEmail,
        Instant timestamp,
        String sessionId
) {
}