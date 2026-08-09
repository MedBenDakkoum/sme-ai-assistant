package com.smeassistant.rag_mcp_assistant.llm;

import org.springframework.ai.tool.ToolCallback;

public interface LlmClient {

    /**
     * Envoie un prompt au LLM et retourne la réponse textuelle.
     */
    String generate(String prompt);

    /**
     * Version plus complète qui permet de passer un system prompt + user prompt.
     */
    String generate(String systemPrompt, String userPrompt);

    /**
     * Variante "function calling" : le LLM peut appeler les tools fournis
     * (ex: MCP search_knowledge_base) avant de produire sa réponse finale.
     */
    String generate(String systemPrompt, String userPrompt, ToolCallback... tools);
}
