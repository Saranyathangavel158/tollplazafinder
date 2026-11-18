package com.tollplaza.tollplazafinder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tollplaza.tollplazafinder.dataset.TollPlaza;
import com.tollplaza.tollplazafinder.dataset.TollPlazaDataService;
import com.tollplaza.tollplazafinder.dto.TollPlazaDetail;
import com.tollplaza.tollplazafinder.dto.TollPlazaRequest;
import com.tollplaza.tollplazafinder.dto.TollPlazaResponse;
import com.tollplaza.tollplazafinder.geocoding.Coordinate;
import com.tollplaza.tollplazafinder.geocoding.GeocodingException;
import com.tollplaza.tollplazafinder.geocoding.GeoapifyGeocodingService;
import com.tollplaza.tollplazafinder.geocoding.LocationNotFoundException;
import com.tollplaza.tollplazafinder.matching.MatchedTollPlaza;
import com.tollplaza.tollplazafinder.matching.TollPlazaMatchingService;
import com.tollplaza.tollplazafinder.routing.GeoapifyRoutingService;
import com.tollplaza.tollplazafinder.routing.Route;
import com.tollplaza.tollplazafinder.routing.RoutingException;

/**
 * Orchestration tests: every collaborator is a mock, so no network, CSV or Spring context is involved.
 */
class TollPlazaServiceTest {

    private static final Coordinate SOURCE = new Coordinate(12.9, 77.5);
    private static final Coordinate DESTINATION = new Coordinate(13.08, 80.27);
    private static final Route ROUTE = new Route(377.846, 16860, List.of(SOURCE, DESTINATION));
    private static final TollPlazaRequest REQUEST = new TollPlazaRequest("560064", "600001");

    private GeoapifyGeocodingService geocoding;
    private GeoapifyRoutingService routing;
    private TollPlazaMatchingService matching;
    private TollPlazaDataService data;
    private TollPlazaService service;

    private final List<TollPlaza> dataset = List.of(new TollPlaza(77.7, 13.2, "Dataset Toll", "Karnataka"));

    @BeforeEach
    void setUp() {
        geocoding = mock(GeoapifyGeocodingService.class);
        routing = mock(GeoapifyRoutingService.class);
        matching = mock(TollPlazaMatchingService.class);
        data = mock(TollPlazaDataService.class);
        service = new TollPlazaService(geocoding, routing, matching, data);

        when(data.getAll()).thenReturn(dataset);
    }

    @Test
    void buildsResponseFromGeocodingRoutingAndMatching() {
        TollPlaza first = new TollPlaza(77.7123, 13.2456, "First Toll", "Karnataka");
        TollPlaza second = new TollPlaza(79.1, 12.95, "Second Toll", "Tamil Nadu");
        when(geocoding.geocode("560064")).thenReturn(SOURCE);
        when(geocoding.geocode("600001")).thenReturn(DESTINATION);
        when(routing.getRoute(SOURCE, DESTINATION)).thenReturn(ROUTE);
        when(matching.match(ROUTE, dataset)).thenReturn(List.of(
                new MatchedTollPlaza(first, 19.2345, 0.1),
                new MatchedTollPlaza(second, 204.2049, 0.0)));

        TollPlazaResponse response = service.findTollPlazas(REQUEST);

        assertEquals("560064", response.sourcePincode());
        assertEquals("600001", response.destinationPincode());
        assertEquals(377.85, response.routeDistanceKm());
        assertEquals(List.of(
                new TollPlazaDetail("First Toll", 13.2456, 77.7123, 19.23),
                new TollPlazaDetail("Second Toll", 12.95, 79.1, 204.2)),
                response.tollPlazas());
    }

    @Test
    void usesEachProviderWithTheRightInputs() {
        when(geocoding.geocode("560064")).thenReturn(SOURCE);
        when(geocoding.geocode("600001")).thenReturn(DESTINATION);
        when(routing.getRoute(SOURCE, DESTINATION)).thenReturn(ROUTE);
        when(matching.match(ROUTE, dataset)).thenReturn(List.of());

        service.findTollPlazas(REQUEST);

        verify(geocoding).geocode("560064");
        verify(geocoding).geocode("600001");
        verify(routing).getRoute(SOURCE, DESTINATION);
        verify(matching).match(ROUTE, dataset);
    }

    @Test
    void emptyTollResultStillReturnsRouteSummary() {
        when(geocoding.geocode("560064")).thenReturn(SOURCE);
        when(geocoding.geocode("600001")).thenReturn(DESTINATION);
        when(routing.getRoute(SOURCE, DESTINATION)).thenReturn(ROUTE);
        when(matching.match(ROUTE, dataset)).thenReturn(List.of());

        TollPlazaResponse response = service.findTollPlazas(REQUEST);

        assertEquals(377.85, response.routeDistanceKm());
        assertTrue(response.tollPlazas().isEmpty());
    }

    @Test
    void sourceGeocodingFailureStopsTheFlow() {
        when(geocoding.geocode("560064")).thenThrow(new GeocodingException("Geoapify geocoding failed (HTTP 500)"));

        assertThrows(GeocodingException.class, () -> service.findTollPlazas(REQUEST));

        verify(geocoding, never()).geocode("600001");
        verify(routing, never()).getRoute(any(), any());
        verify(matching, never()).match(any(), any());
    }

    @Test
    void destinationNotFoundPropagatesAsLocationNotFound() {
        when(geocoding.geocode("560064")).thenReturn(SOURCE);
        when(geocoding.geocode("600001")).thenThrow(new LocationNotFoundException("No location found for PIN code 600001"));

        assertThrows(LocationNotFoundException.class, () -> service.findTollPlazas(REQUEST));

        verify(routing, never()).getRoute(any(), any());
    }

    @Test
    void routingFailureStopsBeforeMatching() {
        when(geocoding.geocode("560064")).thenReturn(SOURCE);
        when(geocoding.geocode("600001")).thenReturn(DESTINATION);
        when(routing.getRoute(SOURCE, DESTINATION)).thenThrow(new RoutingException("Geoapify routing failed (HTTP 503)"));

        assertThrows(RoutingException.class, () -> service.findTollPlazas(REQUEST));

        verify(matching, never()).match(any(), any());
    }
}
