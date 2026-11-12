package com.campuslands.incidents.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campuslands.incidents.dto.indexing.IndexingResult;
import com.campuslands.incidents.service.indexing.KnowledgeReindexService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Vector store maintenance for the knowledge base. Kept apart from the article CRUD controller,
 * which shares the {@code /api/knowledge-articles} base path.
 */
@RestController
@RequestMapping("/api/knowledge-articles")
@Tag(name = "Knowledge base indexing")
public class KnowledgeIndexingController {

    private final KnowledgeReindexService reindexService;

    public KnowledgeIndexingController(KnowledgeReindexService reindexService) {
        this.reindexService = reindexService;
    }

    @PostMapping("/reindex")
    @Operation(summary = "Re-index every knowledge article in the vector store",
            description = "Active and verified articles are upserted; the rest are removed. Safe to repeat.")
    @ApiResponse(responseCode = "200", description = "All articles were processed")
    @ApiResponse(responseCode = "502", description = "The indexing flow reported errors; the body lists them")
    public ResponseEntity<IndexingResult> reindex() {
        IndexingResult result = reindexService.reindexAll();
        HttpStatus status = result.successful() ? HttpStatus.OK : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(result);
    }
}
