package com.smeassistant.rag_mcp_assistant.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class OpenRouterClient implements LlmClient {

    private final ChatClient chatClient;

    public OpenRouterClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String generate(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }

    @Override
    public String generate(String systemPrompt, String userPrompt, ToolCallback... tools) {
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt);
        if (tools != null && tools.length > 0) {
            // ChatClientRequestSpec.tools(Object...) is the non-deprecated way to pass
            // ToolCallback[] from the MCP server; toolCallbacks(...) is deprecated in
            // Spring AI 2.0.0. ToolCallback implements the relevant interface, so the
            // cast to Object[] is safe.
            spec = spec.tools((Object[]) tools);
        }
        return spec.call().content();
    }
}
