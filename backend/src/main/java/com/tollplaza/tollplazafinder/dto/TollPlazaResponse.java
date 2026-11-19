package com.tollplaza.tollplazafinder.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Successful response: route summary plus the toll plazas along it.
 */
@Schema(description = "Route summary and the toll plazas found along the driving route")
public record TollPlazaResponse(
        @Schema(description = "Source PIN code, as requested", example = "560064")
        String sourcePincode,
        @Schema(description = "Destination PIN code, as requested", example = "600001")
        String destinationPincode,
        @Schema(description = "Driving route distance in kilometres, rounded to 2 decimals", example = "346.12")
        double routeDistanceKm,
        @Schema(description = "Toll plazas on the route, in travel order from the source; empty if none were found")
        List<TollPlazaDetail> tollPlazas) {
}
