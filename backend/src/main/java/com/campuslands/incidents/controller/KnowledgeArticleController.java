package com.campuslands.incidents.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.campuslands.incidents.dto.knowledge.KnowledgeArticleRequest;
import com.campuslands.incidents.dto.knowledge.KnowledgeArticleResponse;
import com.campuslands.incidents.service.knowledge.KnowledgeArticleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Knowledge article CRUD. Shares the base path with {@link KnowledgeIndexingController}
 * ({@code POST /reindex}, member C); the literal "/reindex" route never clashes with "/{id}".
 * There is no DELETE on purpose: articles are deactivated so past AI answers keep their sources.
 */
@RestController
@RequestMapping("/api/knowledge-articles")
@Tag(name = "Knowledge base", description = "Technical articles used as RAG sources (RF02)")
public class KnowledgeArticleController {

    private final KnowledgeArticleService articleService;

    public KnowledgeArticleController(KnowledgeArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    @Operation(summary = "List all articles, most recently edited first",
            description = "usableAsSource is true only for active and verified articles.")
    public List<KnowledgeArticleResponse> findAll() {
        return articleService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an article by id (404 if it does not exist)")
    public KnowledgeArticleResponse findById(@PathVariable Long id) {
        return articleService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Create an article",
            description = "New articles are active and unverified unless verified is true. "
                    + "The vector store is synced after the article is saved.")
    public ResponseEntity<KnowledgeArticleResponse> create(@Valid @RequestBody KnowledgeArticleRequest request) {
        KnowledgeArticleResponse created = articleService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an article (404 if it does not exist)",
            description = "When verified is omitted, the current verification is kept.")
    public KnowledgeArticleResponse update(@PathVariable Long id, @Valid @RequestBody KnowledgeArticleRequest request) {
        return articleService.update(id, request);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate an article; it becomes a RAG source again if it is verified")
    public KnowledgeArticleResponse activate(@PathVariable Long id) {
        return articleService.activate(id);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate an article; it is removed from the vector store and no longer used as a source")
    public KnowledgeArticleResponse deactivate(@PathVariable Long id) {
        return articleService.deactivate(id);
    }
}
