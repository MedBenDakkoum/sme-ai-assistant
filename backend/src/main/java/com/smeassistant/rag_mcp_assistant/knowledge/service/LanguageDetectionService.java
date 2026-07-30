package com.smeassistant.rag_mcp_assistant.knowledge.service;

import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
public class LanguageDetectionService {

    private static final Logger log = LoggerFactory.getLogger(LanguageDetectionService.class);

    private LanguageDetector detector;

    @PostConstruct
    void init() {
        long start = System.currentTimeMillis();
        try {
            // Charge uniquement les profils FR + EN pour économiser la mémoire au démarrage
            List<LanguageProfile> profiles = new LanguageProfileReader().readBuiltIn(Arrays.asList(
                    LdLocale.fromString("fr"),
                    LdLocale.fromString("en")
            ));
            this.detector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                    .withProfiles(profiles)
                    .build();
            log.info("LanguageDetector initialisé (FR+EN uniquement) en {} ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'initialiser le détecteur de langue", e);
        }
    }

    // Mots anglais très discriminants (n'existent pas ou rarement en français courant)
    private static final Set<String> ENGLISH_KEYWORDS = Set.of(
            "the", "is", "are", "was", "were", "what", "why", "how", "when",
            "where", "who", "which", "here", "there", "this", "that",
            "you", "your", "i", "my", "we", "they", "he", "she",
            "hello", "hi", "thanks", "please", "sorry", "yes", "no",
            "can", "could", "would", "should", "do", "does", "did",
            "have", "has", "had", "will", "shall",
            "recipe", "cheese", "food", "make", "give", "tell",
            "know", "think", "want", "need", "like", "good", "bad",
            "not", "don't", "doesn't", "didn't", "won't", "can't"
    );

    /**
     * Détecte la langue du texte via une heuristique multi-niveaux :
     * 1. Diacritiques français (é, è, ê, etc.) → fr (signal quasi-parfait)
     * 2. Mots anglais discriminants → en
     * 3. Fallback : librairie language-detector pour les textes longs ambigus
     *
     * @return "fr" ou "en"
     */
    public String detect(String text) {
        if (text == null || text.isBlank()) return "fr";

        String lower = text.toLowerCase(java.util.Locale.ROOT);

        // 1. Diacritiques français → français (signal fort et fiable)
        if (lower.matches(".*[éèêëàâäôöùûüçïî].*")) {
            return "fr";
        }

        // 2. Mots anglais discriminants → anglais
        String[] words = lower.split("\\s+");
        for (String word : words) {
            // Nettoie la ponctuation autour du mot
            String clean = word.replaceAll("[^a-z]", "");
            if (ENGLISH_KEYWORDS.contains(clean)) {
                return "en";
            }
        }

        // 3. Fallback : librairie pour textes longs ambigus
        try {
            var result = detector.detect(text);
            if (result.isPresent()) {
                String lang = result.get().getLanguage();
                if ("fr".equals(lang) || "en".equals(lang)) {
                    return lang;
                }
            }
        } catch (Exception e) {
            log.warn("Erreur détection langue, fallback FR: {}", e.getMessage());
        }
        return "fr";
    }
}