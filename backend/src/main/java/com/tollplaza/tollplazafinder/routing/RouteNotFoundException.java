package com.tollplaza.tollplazafinder.routing;

/**
 * Geoapify answered normally but found no driving route between the two locations.
 * Kept separate from other routing failures so the API can answer 404 instead of 502.
 */
public class RouteNotFoundException extends RoutingException {

    public RouteNotFoundException(String message) {
        super(message);
    }
}
