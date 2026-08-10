package com.smeassistant.rag_mcp_assistant.chat.service;

import com.smeassistant.rag_mcp_assistant.chat.model.ConversationEntity;
import com.smeassistant.rag_mcp_assistant.chat.model.MessageEntity;
import com.smeassistant.rag_mcp_assistant.chat.repository.ConversationRepository;
import com.smeassistant.rag_mcp_assistant.chat.repository.MessageRepository;
import com.smeassistant.rag_mcp_assistant.knowledge.service.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final String DEFAULT_CHANNEL = "web";

    private final RagService ragService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ChatService(RagService ragService,
                       ConversationRepository conversationRepository,
                       MessageRepository messageRepository) {
        this.ragService = ragService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    public String ask(String question, String userEmail, String sessionId) {
        ConversationEntity conversation = findOrCreateConversation(sessionId, userEmail);

        saveMessage(conversation, "user", question);

        String answer = ragService.ask(question, userEmail, sessionId);

        saveMessage(conversation, "assistant", answer);

        return answer;
    }

    public List<Map<String, Object>> getHistory(String sessionId) {
        try {
            return messageRepository.findByConversationIdOrderByCreatedAtAsc(sessionId)
                    .stream()
                    .map(m -> Map.<String, Object>of(
                            "role", m.getRole(),
                            "content", m.getContent(),
                            "timestamp", m.getCreatedAt()))
                    .toList();
        } catch (Exception e) {
            log.error("Echec lecture historique conversation {} : {}", sessionId, e.getMessage());
            return List.of();
        }
    }

    private ConversationEntity findOrCreateConversation(String sessionId, String userEmail) {
        try {
            return conversationRepository.findById(sessionId)
                    .orElseGet(() -> {
                        ConversationEntity conversation = new ConversationEntity();
                        conversation.setId(sessionId);
                        conversation.setUserEmail(userEmail);
                        conversation.setChannel(DEFAULT_CHANNEL);
                        return conversationRepository.save(conversation);
                    });
        } catch (Exception e) {
            log.error("Echec persistance conversation {} : {}", sessionId, e.getMessage());
            return null; // ne bloque jamais la reponse
        }
    }

    private void saveMessage(ConversationEntity conversation, String role, String content) {
        try {
            MessageEntity message = new MessageEntity();
            message.setConversation(conversation);
            message.setRole(role);
            message.setContent(content);
            messageRepository.save(message);
        } catch (Exception e) {
            log.error("Echec persistance message ({}) conversation {} : {}",
                    role, conversation != null ? conversation.getId() : "?", e.getMessage());
        }
    }
}
