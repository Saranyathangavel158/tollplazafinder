package com.tollplaza.tollplazafinder.exception;

import java.time.Instant;
import java.util.List;

/**
 * Standard JSON error body returned by {@link GlobalExceptionHandler}.
 *
 * @param errors per-field problems; empty when the error is not tied to a field
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<FieldIssue> errors) {

    public record FieldIssue(String field, String message) {
    }
}
