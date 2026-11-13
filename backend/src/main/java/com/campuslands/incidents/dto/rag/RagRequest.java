package com.campuslands.incidents.dto.rag;

/**
 * Body sent to the n8n RAG webhook (contract in n8n/README.md). Threshold and topK come from the
 * Spring Boot configuration, the single source of truth.
 */
public record RagRequest(Long queryId, String description, double threshold, int topK) {
}
