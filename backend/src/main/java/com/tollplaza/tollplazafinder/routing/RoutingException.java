package com.tollplaza.tollplazafinder.routing;

/**
 * Thrown when a route cannot be obtained (no route, API error, timeout, malformed response,
 * or missing configuration). Messages never contain the request URL, so the API key cannot leak.
 */
public class RoutingException extends RuntimeException {

    public RoutingException(String message) {
        super(message);
    }
}
