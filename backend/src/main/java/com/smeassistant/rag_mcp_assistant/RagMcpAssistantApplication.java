package com.smeassistant.rag_mcp_assistant;

import org.springframework.ai.mcp.client.common.autoconfigure.McpClientAutoConfiguration;
import org.springframework.ai.mcp.client.common.autoconfigure.McpToolCallbackAutoConfiguration;
import org.springframework.ai.mcp.client.common.autoconfigure.StdioTransportAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Application principale.
 *
 * <p>On exclut {@code McpClientAutoConfiguration} et
 * {@code McpToolCallbackAutoConfiguration} pour éviter la création précoce des beans
 * {@code mcpSyncClients} et {@code mcpToolCallbacks} qui forcent l'initialisation du
 * client MCP (appel HTTP au endpoint {@code /mcp}) pendant
 * {@code preInstantiateSingletons()} — donc AVANT que Tomcat ne soit à l'écoute.
 * Le client et le serveur MCP tournent dans le même process.</p>
 *
 * <p>Le branchement paresseux est défini dans
 * {@link com.smeassistant.rag_mcp_assistant.config.McpClientConfig} : on injecte
 * {@code List<McpSyncClient>} via un {@code @Lazy ObjectProvider}, donc la connexion
 * HTTP au serveur MCP local n'est ouverte qu'à la première requête utilisateur</p>
 */
@SpringBootApplication(exclude = {
        McpClientAutoConfiguration.class,
        McpToolCallbackAutoConfiguration.class,
        StdioTransportAutoConfiguration.class
})
@EnableAsync
public class RagMcpAssistantApplication {

	public static void main(String[] args) {
		SpringApplication.run(RagMcpAssistantApplication.class, args);
	}

}
