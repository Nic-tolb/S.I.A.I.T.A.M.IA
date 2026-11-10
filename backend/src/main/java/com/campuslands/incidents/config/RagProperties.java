package com.campuslands.incidents.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Semantic search parameters (RF04).
 *
 * @param similarityThreshold minimum similarity for an article to be accepted as a source
 *                            (must be justified with test evidence in the report)
 * @param topK                maximum number of articles to retrieve (between 3 and 5)
 */
@Validated
@ConfigurationProperties(prefix = "app.rag")
public record RagProperties(
        @DecimalMin("0.0") @DecimalMax("1.0") double similarityThreshold,
        @Min(3) @Max(5) int topK) {
}
