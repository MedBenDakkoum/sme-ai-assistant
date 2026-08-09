package com.smeassistant.rag_mcp_assistant.config;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Configuration du client MCP local.
 *
 * <p>Le serveur MCP (Streamable HTTP) tourne dans le meme process Spring Boot, sur le meme
 * port Tomcat. Le bean {@code List<McpSyncClient>} fourni par
 * {@code McpClientAutoConfiguration} appelle {@code .initialize()} au moment de la creation
 * du bean, pendant {@code preInstantiateSingletons()} — donc AVANT que Tomcat ne soit
 * a l'ecoute du endpoint /mcp.</p>
 *
 * <p>De meme, {@code ToolCallingAutoConfiguration} cree un {@code ToolCallbackResolver}
 * qui appelle {@code getToolCallbacks()} sur tous les {@code ToolCallbackProvider} beans
 * lors de son initialisation, ce qui declenche aussi la connexion prematuree.</p>
 *
 * <p>Pour eviter ces deux blocages, on exclut {@code McpClientAutoConfiguration} et
 * {@code McpToolCallbackAutoConfiguration} dans
 * {@link com.smeassistant.rag_mcp_assistant.RagMcpAssistantApplication} et on fournit
 * ici un bean {@link McpSyncClient} en {@code @Lazy}. Ce bean n'est PAS enregistre
 * comme {@code ToolCallbackProvider} pour eviter que le resolver global ne le resolve
 * trop tot. C'est {@code RagService} qui l'utilise directement via
 * {@code SyncMcpToolCallbackProvider} au moment de l'appel LLM.</p>
 */
@Configuration
public class McpClientConfig {

    private static final Logger log = LoggerFactory.getLogger(McpClientConfig.class);

    /**
     * Cree le {@link McpSyncClient} a la demande (lazy) en construisant le transport
     * Streamable HTTP directement, sans dependre de McpClientAutoConfiguration.
     *
     * <p>Le client sera reellement instancie et connecte uniquement quand
     * {@code RagService.ask()} l'utilisera pour la premiere fois,
     * c-a-d apres le demarrage complet de Tomcat.</p>
     */
    @Bean
    @Lazy
    public McpSyncClient mcpSyncClient(
            @Value("${spring.ai.mcp.client.streamable-http.connections.rag-server.url}") String mcpServerUrl) {

        String endpointUrl = mcpServerUrl + "/mcp";
        log.info("Creating lazy MCP sync client for URL: {}", endpointUrl);

        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(endpointUrl).build();
        McpSyncClient client = McpClient.sync(transport)
                .clientInfo(McpSchema.Implementation.builder("rag-mcp-client", "0.0.1").build())
                .build();

        return client;
    }
}
