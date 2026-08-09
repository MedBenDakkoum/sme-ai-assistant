package com.smeassistant.rag_mcp_assistant.mcp;

import com.smeassistant.rag_mcp_assistant.knowledge.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class KnowledgeMcpTool {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeMcpTool.class);

    // Static list to capture MCP tool invocations for debugging/verification
    private static final List<String> mcpInvocations = new CopyOnWriteArrayList<>();

    private final SearchService searchService;

    public KnowledgeMcpTool(SearchService searchService) {
        this.searchService = searchService;
    }

    public static List<String> getMcpInvocations() {
        return new CopyOnWriteArrayList<>(mcpInvocations);
    }

    public static void clearMcpInvocations() {
        mcpInvocations.clear();
    }

    /**
     * Searches the SME knowledge base for chunks relevant to the user's question.
     * Reuses SearchService logic (including similarity threshold filtering).
     *
     * @param query the user's question or search text (natural language)
     * @return list of relevant text chunks from the indexed documents
     */
    @McpTool(
            name = "search_knowledge_base",
            description = "Searches the SME knowledge base (PostgreSQL + pgvector) for text chunks relevant to the user's question. " +
                    "Use this when you need factual information from uploaded documents to answer a question. " +
                    "Returns up to 4 relevant chunks; empty list means no relevant information was found."
    )
    public List<String> searchKnowledgeBase(
            @McpToolParam(description = "Natural-language question or search query (e.g. 'What is the SLA for critical tickets?')")
            String query
    ) {
        String timestamp = LocalDateTime.now().toString();
        String logLine = String.format("[%s] [MCP TOOL INVOKED] search_knowledge_base called via MCP with query: '%s'", timestamp, query);
        log.info(logLine);
        mcpInvocations.add(logLine);

        List<String> results = searchService.search(query, 4);

        String resultLine = String.format("[%s] [MCP TOOL INVOKED] search_knowledge_base returned %d chunk(s) for query: '%s'", timestamp, results.size(), query);
        log.info(resultLine);
        mcpInvocations.add(resultLine);

        return results;
    }
}
