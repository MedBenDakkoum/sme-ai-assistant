package com.smeassistant.rag_mcp_assistant.mcp;

import com.smeassistant.rag_mcp_assistant.knowledge.service.SearchService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeMcpTool {

    private final SearchService searchService;

    public KnowledgeMcpTool(SearchService searchService) {
        this.searchService = searchService;
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
        return searchService.search(query, 4);
    }
}
