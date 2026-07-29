package com.smeassistant.rag_mcp_assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RagMcpAssistantApplication {

	public static void main(String[] args) {
		SpringApplication.run(RagMcpAssistantApplication.class, args);
	}

}
