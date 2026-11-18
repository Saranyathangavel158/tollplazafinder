package com.tollplaza.tollplazafinder.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.tollplaza.tollplazafinder.dataset.TollPlazaDataService;
import com.tollplaza.tollplazafinder.geocoding.Coordinate;
import com.tollplaza.tollplazafinder.geocoding.GeoapifyGeocodingService;
import com.tollplaza.tollplazafinder.routing.GeoapifyRoutingService;
import com.tollplaza.tollplazafinder.routing.Route;

/**
 * Opt-in end-to-end check against the REAL Geoapify APIs and the REAL CSV dataset:
 * geocode -> route -> toll matching. Runs only when GEOAPIFY_API_KEY is set; Spring injects the key
 * from that environment variable and this test never reads or logs it.
 * Live data changes, so it asserts structure and invariants, never a fixed toll count or names.
 * Run explicitly with: mvn test -Dtest=TollPlazaMatchingRealRouteIT
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GEOAPIFY_API_KEY", matches = ".+")
class TollPlazaMatchingRealRouteIT {

    private static final Logger log = LoggerFactory.getLogger(TollPlazaMatchingRealRouteIT.class);

    @Autowired
    private GeoapifyGeocodingService geocodingService;

    @Autowired
    private GeoapifyRoutingService routingService;

    @Autowired
    private TollPlazaMatchingService matchingService;

    @Autowired
    private TollPlazaDataService dataService;

    @Test
    void matchesRealTollPlazasOnRealRouteFrom560064To600001() {
        Coordinate source = geocodingService.geocode("560064");
        Coordinate destination = geocodingService.geocode("600001");
        Route route = routingService.getRoute(source, destination);

        List<String> report = new ArrayList<>();
        report.add("route 560064 -> 600001: distanceKm=" + round(route.distanceKm())
                + ", durationMinutes=" + round(route.durationSeconds() / 60)
                + ", geometryPoints=" + route.geometry().size()
                + ", thresholdKm=" + matchingService.getThresholdKm());

        List<MatchedTollPlaza> matches = matchingService.match(route, dataService.getAll());

        report.add("matched toll plazas: " + matches.size());
        for (MatchedTollPlaza m : matches) {
            report.add(m.tollPlaza().name() + " (" + m.tollPlaza().state() + ") - "
                    + round(m.distanceFromSourceKm()) + " km from source, "
                    + round(m.distanceFromRouteKm()) + " km off route");
        }
        // Surefire may swallow forked-JVM console output, so the diagnostics are also written to target/.
        report.forEach(line -> log.info("IT {}", line));
        writeReport(report);

        // the dataset and route are real
        assertEquals(1536, dataService.getAll().size());
        assertTrue(route.distanceKm() > 100 && route.distanceKm() < 1000);
        assertFalse(route.geometry().isEmpty());

        // the result is valid (no fixed count or names asserted)
        assertNotNull(matches);
        double previous = -1;
        for (MatchedTollPlaza m : matches) {
            assertTrue(m.distanceFromSourceKm() >= previous, "results must be ordered by distance from source");
            assertTrue(m.distanceFromSourceKm() >= 0 && m.distanceFromSourceKm() <= route.distanceKm() * 1.05,
                    "distance from source must lie within the route length");
            assertTrue(m.distanceFromRouteKm() >= 0 && m.distanceFromRouteKm() <= matchingService.getThresholdKm());
            previous = m.distanceFromSourceKm();
        }
        assertEquals(matches.size(), new HashSet<>(matches.stream().map(MatchedTollPlaza::tollPlaza).toList()).size(),
                "no duplicate toll plazas");
    }

    private static void writeReport(List<String> lines) {
        try {
            Path file = Path.of("target", "real-route-it-report.txt");
            Files.createDirectories(file.getParent());
            Files.write(file, lines);
        } catch (IOException e) {
            log.warn("Could not write IT report: {}", e.getClass().getSimpleName());
        }
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
