package com.campuslands.incidents.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Single error format for the whole API.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fields) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, Map.of());
    }
}
