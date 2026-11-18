package com.tollplaza.tollplazafinder.geocoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/**
 * All HTTP traffic is served by MockRestServiceServer; the real Geoapify API is never called.
 */
class GeoapifyGeocodingServiceTest {

    private static final String KEY = "test-key-not-real";

    private MockRestServiceServer server;
    private GeoapifyGeocodingService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.geoapify.com");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new GeoapifyGeocodingService(builder.build(), KEY);
    }

    @Test
    void returnsCoordinatesOfFirstResult() {
        server.expect(requestTo(Matchers.startsWith("https://api.geoapify.com/v1/geocode/search")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("text", "110001%2C%20India"))
                .andExpect(queryParam("apiKey", KEY))
                .andRespond(withSuccess("""
                        {"type":"FeatureCollection","features":[
                          {"properties":{"lat":28.6328,"lon":77.2197,"city":"New Delhi"}},
                          {"properties":{"lat":1.0,"lon":2.0}}]}
                        """, MediaType.APPLICATION_JSON));

        Coordinate result = service.geocode("110001");

        assertEquals(new Coordinate(28.6328, 77.2197), result);
        server.verify();
    }

    @Test
    void emptyResultsThrowNotFound() {
        server.expect(requestTo(Matchers.startsWith("https://api.geoapify.com/v1/geocode/search")))
                .andRespond(withSuccess("{\"features\":[]}", MediaType.APPLICATION_JSON));

        GeocodingException ex = assertThrows(GeocodingException.class, () -> service.geocode("110001"));
        assertTrue(ex.getMessage().contains("No location found"));
    }

    @Test
    void httpErrorThrowsWithoutLeakingKey() {
        server.expect(requestTo(Matchers.startsWith("https://api.geoapify.com/v1/geocode/search")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        GeocodingException ex = assertThrows(GeocodingException.class, () -> service.geocode("110001"));
        assertTrue(ex.getMessage().contains("HTTP 401"));
        assertFalse(ex.getMessage().contains(KEY));
    }

    @Test
    void serverErrorThrows() {
        server.expect(requestTo(Matchers.startsWith("https://api.geoapify.com/v1/geocode/search")))
                .andRespond(withServerError());

        GeocodingException ex = assertThrows(GeocodingException.class, () -> service.geocode("110001"));
        assertTrue(ex.getMessage().contains("HTTP 500"));
    }

    @Test
    void timeoutOrConnectionFailureThrowsWithoutLeakingKey() {
        server.expect(requestTo(Matchers.startsWith("https://api.geoapify.com/v1/geocode/search")))
                .andRespond(request -> {
                    throw new IOException("simulated timeout for " + request.getURI());
                });

        GeocodingException ex = assertThrows(GeocodingException.class, () -> service.geocode("110001"));
        assertTrue(ex.getMessage().contains("connection error or timeout"));
        assertFalse(ex.getMessage().contains(KEY));
        assertEquals(null, ex.getCause());
    }

    @Test
    void malformedJsonThrows() {
        server.expect(requestTo(Matchers.startsWith("https://api.geoapify.com/v1/geocode/search")))
                .andRespond(withSuccess("{not json", MediaType.APPLICATION_JSON));

        GeocodingException ex = assertThrows(GeocodingException.class, () -> service.geocode("110001"));
        assertTrue(ex.getMessage().contains("malformed"));
    }

    @Test
    void unexpectedStructureThrows() {
        server.expect(requestTo(Matchers.startsWith("https://api.geoapify.com/v1/geocode/search")))
                .andRespond(withSuccess("{\"features\":[{\"properties\":{\"lat\":\"abc\"}}]}",
                        MediaType.APPLICATION_JSON));

        GeocodingException ex = assertThrows(GeocodingException.class, () -> service.geocode("110001"));
        assertTrue(ex.getMessage().contains("malformed"));
    }

    @Test
    void missingFeaturesArrayThrows() {
        server.expect(requestTo(Matchers.startsWith("https://api.geoapify.com/v1/geocode/search")))
                .andRespond(withSuccess("{\"foo\":1}", MediaType.APPLICATION_JSON));

        assertThrows(GeocodingException.class, () -> service.geocode("110001"));
    }

    @Test
    void missingApiKeyFailsBeforeAnyRequest() {
        GeoapifyGeocodingService noKey = new GeoapifyGeocodingService(RestClient.create(), "");
        GeocodingException ex = assertThrows(GeocodingException.class, () -> noKey.geocode("110001"));
        assertTrue(ex.getMessage().contains("GEOAPIFY_API_KEY"));
    }

    @Test
    void invalidPincodeIsRejectedWithoutRequest() {
        assertThrows(IllegalArgumentException.class, () -> service.geocode("012345"));
        assertThrows(IllegalArgumentException.class, () -> service.geocode("abc"));
        assertThrows(IllegalArgumentException.class, () -> service.geocode(null));
        server.verify(); // no requests were expected or made
    }
}
