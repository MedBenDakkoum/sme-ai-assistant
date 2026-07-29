package com.smeassistant.rag_mcp_assistant.chat.controller;

import com.smeassistant.rag_mcp_assistant.knowledge.service.RagService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String userEmail = request.get("userEmail");
        String sessionId = request.get("sessionId");

        if (question == null || question.isBlank()) {
            return Map.of("answer", "La question ne peut pas être vide.");
        }

        // Générer un sessionId si non fourni
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        String answer = ragService.ask(question, userEmail, sessionId);
        return Map.of("answer", answer, "sessionId", sessionId);
    }
}