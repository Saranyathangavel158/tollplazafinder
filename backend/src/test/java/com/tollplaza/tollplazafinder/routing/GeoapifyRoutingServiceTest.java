package com.tollplaza.tollplazafinder.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.tollplaza.tollplazafinder.geocoding.Coordinate;

/**
 * All HTTP traffic is served by MockRestServiceServer; the real Geoapify API is never called.
 */
class GeoapifyRoutingServiceTest {

    private static final String KEY = "test-key-not-real";
    private static final Coordinate SOURCE = new Coordinate(12.9, 77.5);
    private static final Coordinate DESTINATION = new Coordinate(28.6, 77.2);
    private static final String URL_START = "https://api.geoapify.com/v1/routing";

    private MockRestServiceServer server;
    private GeoapifyRoutingService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.geoapify.com");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new GeoapifyRoutingService(builder.build(), KEY);
    }

    private void respondWith(String json) {
        server.reset(); // allows several responses within one test
        server.expect(requestTo(Matchers.startsWith(URL_START)))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @Test
    void parsesDistanceDurationAndMultiLineStringGeometry() {
        server.expect(requestTo(Matchers.startsWith(URL_START)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("waypoints", "12.9%2C77.5%7C28.6%2C77.2"))
                .andExpect(queryParam("mode", "drive"))
                .andExpect(queryParam("apiKey", KEY))
                .andRespond(withSuccess("""
                        {"features":[{
                          "properties":{"distance":2150500.5,"time":93600.0},
                          "geometry":{"type":"MultiLineString","coordinates":[
                            [[77.5,12.9],[77.6,13.5]],
                            [[77.6,13.5],[77.2,28.6]]]}}]}
                        """, MediaType.APPLICATION_JSON));

        Route route = service.getRoute(SOURCE, DESTINATION);

        assertEquals(2150.5005, route.distanceKm(), 1e-9);
        assertEquals(93600.0, route.durationSeconds());
        assertEquals(4, route.geometry().size());
        // GeoJSON is [longitude, latitude]: 77.5 is the longitude, 12.9 the latitude
        assertEquals(new Coordinate(12.9, 77.5), route.geometry().get(0));
        assertEquals(new Coordinate(28.6, 77.2), route.geometry().get(3));
        server.verify();
    }

    @Test
    void acceptsPlainLineStringGeometry() {
        respondWith("""
                {"features":[{"properties":{"distance":1000,"time":60},
                  "geometry":{"type":"LineString","coordinates":[[77.5,12.9],[77.2,28.6]]}}]}
                """);

        Route route = service.getRoute(SOURCE, DESTINATION);

        assertEquals(1.0, route.distanceKm());
        assertEquals(2, route.geometry().size());
        assertEquals(new Coordinate(28.6, 77.2), route.geometry().get(1));
    }

    @Test
    void emptyFeaturesThrowNoRoute() {
        respondWith("{\"features\":[]}");

        RoutingException ex = assertThrows(RoutingException.class, () -> service.getRoute(SOURCE, DESTINATION));
        assertTrue(ex.getMessage().contains("No route found"));
    }

    @Test
    void malformedJsonThrows() {
        respondWith("{not json");

        RoutingException ex = assertThrows(RoutingException.class, () -> service.getRoute(SOURCE, DESTINATION));
        assertTrue(ex.getMessage().contains("malformed"));
    }

    @Test
    void missingFeaturesArrayThrows() {
        respondWith("{\"foo\":1}");

        assertThrows(RoutingException.class, () -> service.getRoute(SOURCE, DESTINATION));
    }

    @Test
    void missingDistanceOrTimeThrows() {
        respondWith("""
                {"features":[{"properties":{"time":60},
                  "geometry":{"coordinates":[[77.5,12.9],[77.2,28.6]]}}]}
                """);

        RoutingException ex = assertThrows(RoutingException.class, () -> service.getRoute(SOURCE, DESTINATION));
        assertTrue(ex.getMessage().contains("distance"));
    }

    @Test
    void missingOrEmptyGeometryThrows() {
        respondWith("{\"features\":[{\"properties\":{\"distance\":1000,\"time\":60}}]}");
        RoutingException missing = assertThrows(RoutingException.class,
                () -> service.getRoute(SOURCE, DESTINATION));
        assertTrue(missing.getMessage().contains("no route geometry"));

        respondWith("""
                {"features":[{"properties":{"distance":1000,"time":60},
                  "geometry":{"coordinates":[]}}]}
                """);
        assertThrows(RoutingException.class, () -> service.getRoute(SOURCE, DESTINATION));
    }

    @Test
    void invalidGeometryPointsThrow() {
        respondWith("""
                {"features":[{"properties":{"distance":1000,"time":60},
                  "geometry":{"coordinates":[[77.5,"x"],[77.2,28.6]]}}]}
                """);
        assertThrows(RoutingException.class, () -> service.getRoute(SOURCE, DESTINATION));

        respondWith("""
                {"features":[{"properties":{"distance":1000,"time":60},
                  "geometry":{"coordinates":[[77.5,95.0],[77.2,28.6]]}}]}
                """);
        assertThrows(RoutingException.class, () -> service.getRoute(SOURCE, DESTINATION));
    }

    @Test
    void httpErrorThrowsWithoutLeakingKey() {
        server.expect(requestTo(Matchers.startsWith(URL_START))).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        RoutingException ex = assertThrows(RoutingException.class, () -> service.getRoute(SOURCE, DESTINATION));
        assertTrue(ex.getMessage().contains("HTTP 401"));
        assertFalse(ex.getMessage().contains(KEY));
    }

    @Test
    void serverErrorThrows() {
        server.expect(requestTo(Matchers.startsWith(URL_START))).andRespond(withServerError());

        RoutingException ex = assertThrows(RoutingException.class, () -> service.getRoute(SOURCE, DESTINATION));
        assertTrue(ex.getMessage().contains("HTTP 500"));
    }

    @Test
    void connectionFailureThrowsWithoutLeakingKey() {
        server.expect(requestTo(Matchers.startsWith(URL_START)))
                .andRespond(request -> {
                    throw new IOException("simulated timeout for " + request.getURI());
                });

        RoutingException ex = assertThrows(RoutingException.class, () -> service.getRoute(SOURCE, DESTINATION));
        assertTrue(ex.getMessage().contains("connection error or timeout"));
        assertFalse(ex.getMessage().contains(KEY));
        assertNull(ex.getCause());
    }

    @Test
    void invalidCoordinatesAreRejectedWithoutRequest() {
        assertThrows(IllegalArgumentException.class, () -> service.getRoute(null, DESTINATION));
        assertThrows(IllegalArgumentException.class, () -> service.getRoute(SOURCE, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.getRoute(new Coordinate(91, 77), DESTINATION));
        assertThrows(IllegalArgumentException.class,
                () -> service.getRoute(SOURCE, new Coordinate(20, 181)));
        assertThrows(IllegalArgumentException.class,
                () -> service.getRoute(new Coordinate(Double.NaN, 77), DESTINATION));
        server.verify(); // no requests were expected or made
    }

    @Test
    void missingApiKeyFailsBeforeAnyRequest() {
        GeoapifyRoutingService noKey = new GeoapifyRoutingService(RestClient.create(), "");
        RoutingException ex = assertThrows(RoutingException.class, () -> noKey.getRoute(SOURCE, DESTINATION));
        assertTrue(ex.getMessage().contains("GEOAPIFY_API_KEY"));
    }
}
