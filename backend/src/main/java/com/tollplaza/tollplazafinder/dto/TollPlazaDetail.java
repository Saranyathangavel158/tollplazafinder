package com.tollplaza.tollplazafinder.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A toll plaza found along a route, as returned to API clients.
 */
@Schema(description = "A toll plaza located on the route")
public record TollPlazaDetail(
        @Schema(description = "Name of the toll plaza", example = "Electronic City Toll Plaza")
        String tollPlazaName,
        @Schema(description = "Latitude in decimal degrees", example = "12.8452")
        double latitude,
        @Schema(description = "Longitude in decimal degrees", example = "77.6602")
        double longitude,
        @Schema(description = "Distance along the route from the source, in kilometres, rounded to 2 decimals",
                example = "18.45")
        double distanceFromSourceKm) {
}
