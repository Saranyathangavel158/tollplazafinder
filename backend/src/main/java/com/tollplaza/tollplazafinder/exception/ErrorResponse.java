package com.tollplaza.tollplazafinder.exception;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standard JSON error body returned by {@link GlobalExceptionHandler}.
 *
 * @param errors per-field problems; empty when the error is not tied to a field
 */
@Schema(description = "Standard error body returned for all handled failures")
public record ErrorResponse(
        @Schema(description = "When the error occurred (UTC)", example = "2026-11-19T10:15:30.123Z")
        Instant timestamp,
        @Schema(description = "HTTP status code", example = "400")
        int status,
        @Schema(description = "HTTP status reason phrase", example = "Bad Request")
        String error,
        @Schema(description = "Human-readable explanation", example = "Validation failed")
        String message,
        @Schema(description = "Per-field problems; empty when the error is not tied to a field")
        List<FieldIssue> errors) {

    @Schema(description = "A validation problem on a single request field")
    public record FieldIssue(
            @Schema(description = "Name of the invalid request field", example = "sourcePincode")
            String field,
            @Schema(description = "Why the value was rejected",
                    example = "PIN code must be exactly 6 digits and must not start with 0")
            String message) {
    }
}
