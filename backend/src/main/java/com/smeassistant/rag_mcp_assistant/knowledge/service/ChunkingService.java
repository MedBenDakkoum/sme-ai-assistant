package com.smeassistant.rag_mcp_assistant.knowledge.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

    private static final int CHUNK_SIZE = 800;       // caractères cible par chunk
    private static final int CHUNK_OVERLAP = 180;    // chevauchement (sur frontière de paragraphe/ligne)

    /**
     * Découpe un texte en chunks en respectant les frontières naturelles :
     *  - Niveau 1 : paragraphes (séparés par double saut de ligne \n\n)
     *  - Niveau 2 : si un paragraphe dépasse CHUNK_SIZE, on le découpe à la limite
     *    de ligne (\n) la plus proche sans dépasser CHUNK_SIZE
     *  - Jamais de coupure au milieu d'une ligne / d'un mot
     *  - L'overlap s'effectue en reculant à la dernière frontière de paragraphe/ligne
     *    avant la limite, pour garder un contexte sémantique cohérent
     */
    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        // Normalisation : \r\n → \n, puis split sur \n\n
        String normalized = text.replace("\r\n", "\n").trim();

        // 1. Découpage en paragraphes (double saut de ligne)
        String[] paragraphs = normalized.split("\n\n+");

        StringBuilder current = new StringBuilder();
        int chunkIndex = 0;

        for (String paragraph : paragraphs) {
            String p = paragraph.trim();
            if (p.isEmpty()) continue;

            // Cas 1 : paragraphe tient dans le chunk courant → on l'ajoute
            if (current.length() + p.length() + 2 <= CHUNK_SIZE) {
                if (current.length() > 0) current.append("\n\n");
                current.append(p);
                continue;
            }

            // Cas 2 : paragraphe tient seul dans un chunk mais pas dans le courant → flush current
            if (p.length() <= CHUNK_SIZE) {
                if (current.length() > 0) {
                    chunks.add(current.toString().trim());
                    chunkIndex++;
                }
                current = new StringBuilder(p);
                continue;
            }

            // Cas 3 : paragraphe dépasse CHUNK_SIZE → découpage par lignes
            if (current.length() > 0) {
                chunks.add(current.toString().trim());
                chunkIndex++;
                current = new StringBuilder();
            }

            String[] lines = p.split("\n");
            StringBuilder lineBuf = new StringBuilder();

            for (String line : lines) {
                String l = line.trim();
                if (l.isEmpty()) continue;

                // La ligne seule dépasse CHUNK_SIZE → on la coupe par mots (en dernier recours)
                if (l.length() > CHUNK_SIZE) {
                    if (lineBuf.length() > 0) {
                        chunks.add(lineBuf.toString().trim());
                        chunkIndex++;
                        lineBuf = new StringBuilder();
                    }
                    // Découpe par mots
                    String[] words = l.split("\\s+");
                    StringBuilder wordBuf = new StringBuilder();
                    for (String word : words) {
                        if (wordBuf.length() + word.length() + 1 > CHUNK_SIZE) {
                            chunks.add(wordBuf.toString().trim());
                            chunkIndex++;
                            wordBuf = new StringBuilder();
                        }
                        if (wordBuf.length() > 0) wordBuf.append(" ");
                        wordBuf.append(word);
                    }
                    if (wordBuf.length() > 0) {
                        lineBuf.append(wordBuf);
                    }
                    continue;
                }

                // Ligne normale : l'ajouter si ça rentre, sinon flush
                if (lineBuf.length() + l.length() + 1 > CHUNK_SIZE) {
                    chunks.add(lineBuf.toString().trim());
                    chunkIndex++;
                    lineBuf = new StringBuilder();
                }
                if (lineBuf.length() > 0) lineBuf.append("\n");
                lineBuf.append(l);
            }
            if (lineBuf.length() > 0) {
                current = lineBuf;
            }
        }

        // Flush du dernier chunk
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }
}
