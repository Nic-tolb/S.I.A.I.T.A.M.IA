package com.campuslands.incidents.exception;

/**
 * The request is valid but clashes with existing data, answered with 409 Conflict.
 * Examples: duplicate category name or user email, deleting a category that has incidents,
 * rating an answer twice.
 *
 * The message is shown to the user, so it is written in Spanish (like the DTO validation messages).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
