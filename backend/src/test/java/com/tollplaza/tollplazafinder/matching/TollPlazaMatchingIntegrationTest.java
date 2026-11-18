package com.tollplaza.tollplazafinder.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.tollplaza.tollplazafinder.dataset.TollPlaza;
import com.tollplaza.tollplazafinder.dataset.TollPlazaDataService;
import com.tollplaza.tollplazafinder.geocoding.Coordinate;
import com.tollplaza.tollplazafinder.routing.Route;

/**
 * Checks the Spring wiring (configured threshold) and the matcher against the real loaded dataset.
 * The route is hand-built, so no network access is involved.
 */
@SpringBootTest
class TollPlazaMatchingIntegrationTest {

    @Autowired
    private TollPlazaMatchingService matchingService;

    @Autowired
    private TollPlazaDataService dataService;

    @Test
    void thresholdIsReadFromApplicationProperties() {
        assertEquals(2.0, matchingService.getThresholdKm());
    }

    @Test
    void findsARealDatasetTollOnAHandBuiltRoute() {
        TollPlaza dhamnod = dataService.getAll().stream()
                .filter(t -> t.name().equals("Dhamnod Toll Plaza"))
                .findFirst()
                .orElseThrow();

        // a route running east along the toll's own latitude, from 0.5 degrees west to 0.5 degrees east
        Route route = new Route(0, 0, List.of(
                new Coordinate(dhamnod.latitude(), dhamnod.longitude() - 0.5),
                new Coordinate(dhamnod.latitude(), dhamnod.longitude() + 0.5)));

        List<MatchedTollPlaza> result = matchingService.match(route, dataService.getAll());

        assertTrue(result.stream().anyMatch(m -> m.tollPlaza().equals(dhamnod)));
        MatchedTollPlaza found = result.stream().filter(m -> m.tollPlaza().equals(dhamnod)).findFirst().get();
        assertEquals(0.0, found.distanceFromRouteKm(), 0.01);
        // half a degree of longitude at this latitude is about 51 km
        assertEquals(GeoMath.haversineKm(dhamnod.latitude(), dhamnod.longitude() - 0.5,
                dhamnod.latitude(), dhamnod.longitude()), found.distanceFromSourceKm(), 0.1);
    }
}
