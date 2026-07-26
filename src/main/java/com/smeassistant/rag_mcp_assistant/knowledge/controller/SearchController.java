package com.smeassistant.rag_mcp_assistant.knowledge.controller;

import com.smeassistant.rag_mcp_assistant.knowledge.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public Map<String, Object> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "4") int topK
    ) {
        List<String> results = searchService.search(query, topK);
        return Map.of(
                "query", query,
                "topK", topK,
                "results", results
        );
    }
}
