package com.campuslands.incidents.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI().info(new Info()
                .title("Technology Incident Analysis System")
                .description("Incident management, knowledge base, RAG assistant and analytics")
                .version("0.1.0"));
    }
}
