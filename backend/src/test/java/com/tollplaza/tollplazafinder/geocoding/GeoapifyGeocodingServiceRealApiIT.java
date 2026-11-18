package com.tollplaza.tollplazafinder.geocoding;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Opt-in smoke test against the REAL Geoapify API. Runs only when GEOAPIFY_API_KEY is set.
 * The key is read from the environment and is never printed or asserted into a message.
 * Run explicitly with: mvn test -Dtest=GeoapifyGeocodingServiceRealApiIT
 */
@EnabledIfEnvironmentVariable(named = "GEOAPIFY_API_KEY", matches = ".+")
class GeoapifyGeocodingServiceRealApiIT {

    @Test
    void geocodesBengaluruPincodeAgainstRealApi() {
        GeoapifyGeocodingService service = new GeoapifyGeocodingService(
                new GeoapifyClientConfig().geoapifyRestClient("https://api.geoapify.com"),
                System.getenv("GEOAPIFY_API_KEY"));

        Coordinate c = service.geocode("560064");

        System.out.println("Real API result for 560064: lat=" + c.latitude() + ", lon=" + c.longitude());
        assertTrue(c.latitude() >= 6 && c.latitude() <= 38, "latitude outside India bounds: " + c.latitude());
        assertTrue(c.longitude() >= 68 && c.longitude() <= 98, "longitude outside India bounds: " + c.longitude());
    }
}
