package com.campuslands.incidents.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campuslands.incidents.config.RagProperties;
import com.campuslands.incidents.dto.SystemInfoResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/info")
@Tag(name = "System")
public class SystemInfoController {

    private final RagProperties ragProperties;

    public SystemInfoController(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    @GetMapping
    @Operation(summary = "System information and active RAG configuration")
    public SystemInfoResponse info() {
        return new SystemInfoResponse(
                "Technology Incident Analysis System",
                "0.1.0",
                ragProperties.similarityThreshold(),
                ragProperties.topK());
    }
}
