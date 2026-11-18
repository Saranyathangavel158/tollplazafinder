package com.tollplaza.tollplazafinder.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class TollPlazaResponseTest {

    @Test
    void responseCarriesRouteSummaryAndTollPlazas() {
        TollPlazaDetail plaza = new TollPlazaDetail("Dhamnod Toll Plaza", 23.41805978, 74.99684426, 12.5);
        TollPlazaResponse response = new TollPlazaResponse("110001", "560001", 2150.0, List.of(plaza));

        assertEquals("110001", response.sourcePincode());
        assertEquals("560001", response.destinationPincode());
        assertEquals(2150.0, response.routeDistanceKm());
        assertEquals(1, response.tollPlazas().size());
        assertEquals("Dhamnod Toll Plaza", response.tollPlazas().get(0).tollPlazaName());
        assertEquals(12.5, response.tollPlazas().get(0).distanceFromSourceKm());
    }
}
