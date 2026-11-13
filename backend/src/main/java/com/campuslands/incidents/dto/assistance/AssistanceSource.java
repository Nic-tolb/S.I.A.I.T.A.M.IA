package com.campuslands.incidents.dto.assistance;

/**
 * Knowledge article used as a source, with its similarity to the question (0..1).
 * Same shape as the items of {@code sources} in the n8n contract (n8n/README.md).
 */
public record AssistanceSource(Long articleId, String title, double similarity) {
}
