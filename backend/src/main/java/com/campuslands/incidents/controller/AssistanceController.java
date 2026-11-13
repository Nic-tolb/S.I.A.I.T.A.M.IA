package com.campuslands.incidents.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campuslands.incidents.dto.assistance.AssistanceRequest;
import com.campuslands.incidents.dto.assistance.AssistanceResponse;
import com.campuslands.incidents.service.assistance.AiQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/incidents/{incidentId}/assistance")
@Tag(name = "AI assistant", description = "RAG assistant over the knowledge base (RF03)")
public class AssistanceController {

    private final AiQueryService aiQueryService;

    public AssistanceController(AiQueryService aiQueryService) {
        this.aiQueryService = aiQueryService;
    }

    @PostMapping
    @Operation(summary = "Ask the assistant for a solution to an incident",
            description = "Registers the query, searches the knowledge base and answers only from validated "
                    + "articles. The response status says whether an answer was produced (ANSWERED), nothing "
                    + "matched (NO_RESULTS) or the AI services failed (ERROR).")
    @ApiResponse(responseCode = "200", description = "Query processed; see status in the body")
    @ApiResponse(responseCode = "400", description = "Blank description: the AI flow is never called")
    @ApiResponse(responseCode = "404", description = "The incident does not exist")
    public AssistanceResponse assist(@PathVariable Long incidentId, @Valid @RequestBody AssistanceRequest request) {
        return aiQueryService.assist(incidentId, request.description());
    }
}
