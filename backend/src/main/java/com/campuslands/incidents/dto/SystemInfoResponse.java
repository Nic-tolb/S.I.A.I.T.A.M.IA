package com.campuslands.incidents.dto;

public record SystemInfoResponse(
        String name,
        String version,
        double similarityThreshold,
        int topK) {
}
