package com.smeassistant.rag_mcp_assistant.llm;

public interface LlmClient {

    /**
     * Envoie un prompt au LLM et retourne la réponse textuelle.
     */
    String generate(String prompt);

    /**
     * Version plus complète qui permet de passer un system prompt + user prompt.
     */
    String generate(String systemPrompt, String userPrompt);
}
