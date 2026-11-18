package com.tollplaza.tollplazafinder.dto;

import java.util.List;

/**
 * Successful response: route summary plus the toll plazas along it.
 */
public record TollPlazaResponse(
        String sourcePincode,
        String destinationPincode,
        double routeDistanceKm,
        List<TollPlazaDetail> tollPlazas) {
}
