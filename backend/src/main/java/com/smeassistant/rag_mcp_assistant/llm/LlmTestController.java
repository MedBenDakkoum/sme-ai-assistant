package com.smeassistant.rag_mcp_assistant.llm;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LlmTestController {

    private final LlmClient llmClient;

    public LlmTestController(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @GetMapping("/api/test-llm")
    public String testLlm(@RequestParam(defaultValue = "Dis bonjour en français") String prompt) {
        return llmClient.generate(prompt);
    }
}
