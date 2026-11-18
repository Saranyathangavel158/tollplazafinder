package com.tollplaza.tollplazafinder.exception;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tollplaza.tollplazafinder.geocoding.GeocodingException;
import com.tollplaza.tollplazafinder.geocoding.LocationNotFoundException;
import com.tollplaza.tollplazafinder.routing.RouteNotFoundException;
import com.tollplaza.tollplazafinder.routing.RoutingException;

/**
 * Converts validation and request-parsing failures into consistent JSON errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldIssue> issues = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldIssue(fe.getField(), fe.getDefaultMessage()))
                .sorted(Comparator.comparing(ErrorResponse.FieldIssue::field)
                        .thenComparing(ErrorResponse.FieldIssue::message))
                .toList();
        log.debug("Request validation failed: {}", issues);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", issues);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        log.debug("Unreadable request body: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST,
                "Request body is missing or is not valid JSON", List.of());
    }

    @ExceptionHandler({ LocationNotFoundException.class, RouteNotFoundException.class })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        log.info("Not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    /** Geoapify failed (HTTP error, timeout, bad response, missing key): the upstream service is at fault. */
    @ExceptionHandler({ GeocodingException.class, RoutingException.class })
    public ResponseEntity<ErrorResponse> handleProviderFailure(RuntimeException ex) {
        log.warn("Provider failure: {}", ex.getMessage()); // exception messages never contain the API key
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage(), List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
            List<ErrorResponse.FieldIssue> issues) {
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(),
                message, issues);
        return ResponseEntity.status(status).body(body);
    }
}
