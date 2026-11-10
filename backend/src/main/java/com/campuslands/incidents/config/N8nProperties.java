package com.campuslands.incidents.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * n8n integration settings (values come from environment variables).
 */
@ConfigurationProperties(prefix = "app.n8n")
public record N8nProperties(
        String baseUrl,
        String webhookSecret,
        String indexingPath,
        String ragPath,
        Duration connectTimeout,
        Duration readTimeout) {
}
