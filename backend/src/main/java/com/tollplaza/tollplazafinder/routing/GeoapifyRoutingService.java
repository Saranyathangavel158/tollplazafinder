package com.tollplaza.tollplazafinder.routing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.tollplaza.tollplazafinder.geocoding.Coordinate;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Fetches a driving route from the Geoapify Routing API.
 * The API key is injected from configuration and is never logged or put in exception messages.
 */
@Service
public class GeoapifyRoutingService {

    private static final Logger log = LoggerFactory.getLogger(GeoapifyRoutingService.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final RestClient restClient;
    private final String apiKey;

    public GeoapifyRoutingService(@Qualifier("geoapifyRestClient") RestClient restClient,
            @Value("${geoapify.api.key:}") String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    public Route getRoute(Coordinate source, Coordinate destination) {
        validate(source, "source");
        validate(destination, "destination");
        if (apiKey == null || apiKey.isBlank()) {
            throw new RoutingException(
                    "Geoapify API key is not configured. Set the GEOAPIFY_API_KEY environment variable.");
        }

        String waypoints = plain(source.latitude()) + "," + plain(source.longitude())
                + "|" + plain(destination.latitude()) + "," + plain(destination.longitude());

        String body;
        try {
            body = restClient.get()
                    .uri(uri -> uri.path("/v1/routing")
                            .queryParam("waypoints", "{waypoints}")
                            .queryParam("mode", "drive")
                            .queryParam("apiKey", "{apiKey}")
                            .build(waypoints, apiKey))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            // Deliberately not using e.getMessage(): it can contain the request URL.
            log.warn("Geoapify routing returned HTTP {}", e.getStatusCode().value());
            throw new RoutingException("Geoapify routing failed (HTTP " + e.getStatusCode().value() + ")");
        } catch (RestClientException e) {
            log.warn("Geoapify routing request failed: {}", e.getClass().getSimpleName());
            throw new RoutingException("Could not reach Geoapify routing (connection error or timeout)");
        }

        return parse(body);
    }

    private static void validate(Coordinate c, String name) {
        if (c == null) {
            throw new IllegalArgumentException(name + " coordinate is required");
        }
        if (!Double.isFinite(c.latitude()) || c.latitude() < -90 || c.latitude() > 90) {
            throw new IllegalArgumentException(name + " latitude must be between -90 and 90");
        }
        if (!Double.isFinite(c.longitude()) || c.longitude() < -180 || c.longitude() > 180) {
            throw new IllegalArgumentException(name + " longitude must be between -180 and 180");
        }
    }

    /** Formats a double without scientific notation. */
    private static String plain(double value) {
        return BigDecimal.valueOf(value).toPlainString();
    }

    private Route parse(String body) {
        if (body == null || body.isBlank()) {
            throw new RoutingException("Geoapify routing returned an empty response");
        }
        JsonNode root;
        try {
            root = JSON.readTree(body);
        } catch (JacksonException e) {
            throw new RoutingException("Geoapify routing returned a malformed response");
        }

        JsonNode features = root.path("features");
        if (!features.isArray()) {
            throw new RoutingException("Geoapify routing response is malformed: 'features' array is missing");
        }
        if (features.isEmpty()) {
            throw new RouteNotFoundException("No route found between the given locations");
        }

        JsonNode feature = features.get(0);
        JsonNode distance = feature.path("properties").path("distance");
        JsonNode time = feature.path("properties").path("time");
        if (!distance.isNumber() || !time.isNumber() || distance.asDouble() < 0 || time.asDouble() < 0) {
            throw new RoutingException(
                    "Geoapify routing response is malformed: 'distance'/'time' missing or invalid");
        }

        List<Coordinate> geometry = parseGeometry(feature.path("geometry").path("coordinates"));
        return new Route(distance.asDouble() / 1000.0, time.asDouble(), List.copyOf(geometry));
    }

    /**
     * Geoapify returns a MultiLineString (array of legs, each an array of [lon, lat]);
     * a plain LineString (array of [lon, lat]) is accepted too. Legs are concatenated in order.
     */
    private List<Coordinate> parseGeometry(JsonNode coordinates) {
        if (!coordinates.isArray() || coordinates.isEmpty()) {
            throw new RoutingException("Geoapify routing response has no route geometry");
        }
        List<Coordinate> points = new ArrayList<>();
        boolean multiLine = coordinates.get(0).isArray() && coordinates.get(0).size() > 0
                && coordinates.get(0).get(0).isArray();
        if (multiLine) {
            for (JsonNode leg : coordinates) {
                addLine(leg, points);
            }
        } else {
            addLine(coordinates, points);
        }
        if (points.isEmpty()) {
            throw new RoutingException("Geoapify routing response has no route geometry");
        }
        return points;
    }

    private void addLine(JsonNode line, List<Coordinate> points) {
        if (!line.isArray()) {
            throw new RoutingException("Geoapify routing response is malformed: invalid geometry");
        }
        for (JsonNode position : line) {
            if (!position.isArray() || position.size() < 2
                    || !position.get(0).isNumber() || !position.get(1).isNumber()) {
                throw new RoutingException("Geoapify routing response is malformed: invalid geometry point");
            }
            double longitude = position.get(0).asDouble(); // GeoJSON order: [longitude, latitude]
            double latitude = position.get(1).asDouble();
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                throw new RoutingException("Geoapify routing response contains out-of-range coordinates");
            }
            points.add(new Coordinate(latitude, longitude));
        }
    }
}
