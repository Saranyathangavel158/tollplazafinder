package com.tollplaza.tollplazafinder.routing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import com.tollplaza.tollplazafinder.geocoding.Coordinate;
import com.tollplaza.tollplazafinder.geocoding.GeoapifyGeocodingService;

/**
 * Opt-in smoke test against the REAL Geoapify APIs. Runs only when GEOAPIFY_API_KEY is set.
 * The key is read from the environment and is never printed.
 * Run explicitly with: mvn test -Dtest=GeoapifyRoutingServiceRealApiIT
 */
@EnabledIfEnvironmentVariable(named = "GEOAPIFY_API_KEY", matches = ".+")
class GeoapifyRoutingServiceRealApiIT {

    @Test
    void routesBetweenTwoRealPincodes() {
        String key = System.getenv("GEOAPIFY_API_KEY");
        RestClient client = RestClient.builder().baseUrl("https://api.geoapify.com").build();
        GeoapifyGeocodingService geocoder = new GeoapifyGeocodingService(client, key);
        GeoapifyRoutingService router = new GeoapifyRoutingService(client, key);

        Coordinate source = geocoder.geocode("560064");
        Coordinate destination = geocoder.geocode("600001");

        Route route = router.getRoute(source, destination);

        System.out.println("Real routing result 560064 -> 600001: distanceKm=" + route.distanceKm()
                + ", durationSeconds=" + route.durationSeconds() + ", geometryPoints=" + route.geometry().size());
        assertTrue(route.distanceKm() > 100 && route.distanceKm() < 1000,
                "unexpected distance: " + route.distanceKm());
        assertTrue(route.durationSeconds() > 0);
        assertFalse(route.geometry().isEmpty());
        Coordinate first = route.geometry().get(0);
        assertTrue(first.latitude() >= 6 && first.latitude() <= 38, "first point latitude outside India");
        assertTrue(first.longitude() >= 68 && first.longitude() <= 98, "first point longitude outside India");
    }
}
