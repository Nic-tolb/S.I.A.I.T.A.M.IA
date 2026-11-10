package com.campuslands.incidents.exception;

/**
 * Failure in the AI chain (n8n, embeddings, Qdrant or OpenRouter).
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiServiceException(String message) {
        super(message);
    }
}
