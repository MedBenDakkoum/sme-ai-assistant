package com.smeassistant.rag_mcp_assistant.knowledge.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

    private static final int CHUNK_SIZE = 800;      // caractères par chunk
    private static final int CHUNK_OVERLAP = 150;    // chevauchement

    /**
     * Découpe un texte en chunks de taille fixe avec overlap.
     */
    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        int start = 0;
        int textLength = text.length();

        while (start < textLength) {
            int end = Math.min(start + CHUNK_SIZE, textLength);
            String chunk = text.substring(start, end).trim();

            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // Avance en tenant compte de l'overlap
            start += (CHUNK_SIZE - CHUNK_OVERLAP);

            // Sécurité anti-boucle infinie
            if (start >= textLength) {
                break;
            }
        }

        return chunks;
    }
}
