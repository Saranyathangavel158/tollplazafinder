package com.tollplaza.tollplazafinder.dto;

/**
 * A toll plaza found along a route, as returned to API clients.
 */
public record TollPlazaDetail(
        String tollPlazaName,
        double latitude,
        double longitude,
        double distanceFromSourceKm) {
}
