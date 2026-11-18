package com.tollplaza.tollplazafinder.geocoding;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Converts an Indian PIN code to coordinates using the Geoapify Geocoding Search API.
 * The API key is injected from configuration and is never logged or put in exception messages.
 */
@Service
public class GeoapifyGeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeoapifyGeocodingService.class);
    private static final Pattern PINCODE = Pattern.compile("[1-9][0-9]{5}");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final RestClient restClient;
    private final String apiKey;

    public GeoapifyGeocodingService(@Qualifier("geoapifyRestClient") RestClient restClient,
            @Value("${geoapify.api.key:}") String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    public Coordinate geocode(String pincode) {
        if (pincode == null || !PINCODE.matcher(pincode).matches()) {
            throw new IllegalArgumentException("PIN code must be exactly 6 digits and must not start with 0");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeocodingException(
                    "Geoapify API key is not configured. Set the GEOAPIFY_API_KEY environment variable.");
        }

        String body;
        try {
            body = restClient.get()
                    .uri(uri -> uri.path("/v1/geocode/search")
                            .queryParam("text", "{text}")
                            .queryParam("apiKey", "{apiKey}")
                            .build(pincode + ", India", apiKey))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            // Deliberately not using e.getMessage(): it can contain the request URL.
            log.warn("Geoapify returned HTTP {} while geocoding PIN code {}", e.getStatusCode().value(), pincode);
            throw new GeocodingException("Geoapify geocoding failed for PIN code " + pincode
                    + " (HTTP " + e.getStatusCode().value() + ")");
        } catch (RestClientException e) {
            log.warn("Geoapify request failed for PIN code {}: {}", pincode, e.getClass().getSimpleName());
            throw new GeocodingException("Could not reach Geoapify for PIN code " + pincode
                    + " (connection error or timeout)");
        }

        return parse(body, pincode);
    }

    private Coordinate parse(String body, String pincode) {
        if (body == null || body.isBlank()) {
            throw new GeocodingException("Geoapify returned an empty response for PIN code " + pincode);
        }
        JsonNode root;
        try {
            root = JSON.readTree(body);
        } catch (JacksonException e) {
            throw new GeocodingException("Geoapify returned a malformed response for PIN code " + pincode);
        }

        JsonNode features = root.path("features");
        if (!features.isArray()) {
            throw new GeocodingException("Geoapify response for PIN code " + pincode
                    + " is malformed: 'features' array is missing");
        }
        if (features.isEmpty()) {
            throw new LocationNotFoundException("No location found for PIN code " + pincode);
        }

        JsonNode properties = features.get(0).path("properties");
        JsonNode lat = properties.path("lat");
        JsonNode lon = properties.path("lon");
        if (!lat.isNumber() || !lon.isNumber()) {
            throw new GeocodingException("Geoapify response for PIN code " + pincode
                    + " is malformed: 'lat'/'lon' missing or not numeric");
        }
        double latitude = lat.asDouble();
        double longitude = lon.asDouble();
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new GeocodingException("Geoapify returned out-of-range coordinates for PIN code " + pincode);
        }
        return new Coordinate(latitude, longitude);
    }
}
