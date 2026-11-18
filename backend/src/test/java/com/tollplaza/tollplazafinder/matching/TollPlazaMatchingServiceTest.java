package com.tollplaza.tollplazafinder.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tollplaza.tollplazafinder.dataset.TollPlaza;
import com.tollplaza.tollplazafinder.geocoding.Coordinate;
import com.tollplaza.tollplazafinder.routing.Route;

/**
 * Pure unit tests with small hand-built routes along the equator (1 degree of longitude is about
 * 111.2 km there, and 0.009 degrees of latitude is about 1.0 km), so expected values are easy to reason about.
 */
class TollPlazaMatchingServiceTest {

    private static final double KM_PER_DEGREE = GeoMath.haversineKm(0, 0, 0, 1); // ~111.19 km

    private final TollPlazaMatchingService service = new TollPlazaMatchingService(2.0);

    private static Route route(Coordinate... points) {
        return new Route(0, 0, List.of(points));
    }

    /** West-to-east route along the equator from longitude 0 to 2 with a vertex at longitude 1. */
    private static Route equatorRoute() {
        return route(new Coordinate(0, 0), new Coordinate(0, 1), new Coordinate(0, 2));
    }

    private static TollPlaza toll(String name, double latitude, double longitude) {
        return new TollPlaza(longitude, latitude, name, "Test State"); // note: record order is lon, lat
    }

    @Test
    void tollExactlyOnRouteSegmentIsMatched() {
        List<MatchedTollPlaza> result = service.match(equatorRoute(), List.of(toll("On Route", 0, 0.5)));

        assertEquals(1, result.size());
        assertEquals(0.0, result.get(0).distanceFromRouteKm(), 1e-6);
        assertEquals(0.5 * KM_PER_DEGREE, result.get(0).distanceFromSourceKm(), 0.01);
    }

    @Test
    void tollInsideSegmentIsMatchedEvenWhenFarFromEveryVertex() {
        // longitude 0.5 is ~55 km from both neighbouring vertices: only segment matching can find it
        List<MatchedTollPlaza> result = service.match(equatorRoute(), List.of(toll("Mid Segment", 0.005, 0.5)));

        assertEquals(1, result.size());
        assertEquals(0.556, result.get(0).distanceFromRouteKm(), 0.01);
    }

    @Test
    void tollWithinThresholdIsMatchedAndOutsideIsNot() {
        List<MatchedTollPlaza> result = service.match(equatorRoute(), List.of(
                toll("Within 1km", 0.009, 0.5),     // ~1.0 km north of the route
                toll("Within 1.9km", 0.017, 1.5),   // ~1.9 km
                toll("Outside 2.2km", 0.02, 1.2)));  // ~2.2 km

        assertEquals(List.of("Within 1km", "Within 1.9km"),
                result.stream().map(m -> m.tollPlaza().name()).toList());
    }

    @Test
    void thresholdIsConfigurable() {
        TollPlaza farToll = toll("3km away", 0.027, 0.5); // ~3.0 km

        assertTrue(new TollPlazaMatchingService(2.0).match(equatorRoute(), List.of(farToll)).isEmpty());
        assertEquals(1, new TollPlazaMatchingService(5.0).match(equatorRoute(), List.of(farToll)).size());
    }

    @Test
    void invalidThresholdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TollPlazaMatchingService(0));
        assertThrows(IllegalArgumentException.class, () -> new TollPlazaMatchingService(-1));
        assertThrows(IllegalArgumentException.class, () -> new TollPlazaMatchingService(Double.NaN));
    }

    @Test
    void multipleTollsAreOrderedByTravelDistanceNotInputOrder() {
        List<MatchedTollPlaza> result = service.match(equatorRoute(), List.of(
                toll("Third", 0, 1.8),
                toll("First", 0, 0.2),
                toll("Second", 0.005, 1.0)));

        assertEquals(List.of("First", "Second", "Third"),
                result.stream().map(m -> m.tollPlaza().name()).toList());
    }

    @Test
    void cumulativeDistanceIsMeasuredAlongTheRouteAcrossSegments() {
        // second segment: 1 degree to the vertex + 0.5 degree further
        List<MatchedTollPlaza> result = service.match(equatorRoute(), List.of(toll("Past Vertex", 0, 1.5)));

        assertEquals(1.5 * KM_PER_DEGREE, result.get(0).distanceFromSourceKm(), 0.01);
    }

    @Test
    void cumulativeDistanceFollowsTheRouteNotTheStraightLine() {
        // route goes 1 degree east, then 1 degree north (an "L"); the toll sits at the end of the L
        Route lShape = route(new Coordinate(0, 0), new Coordinate(0, 1), new Coordinate(1, 1));

        List<MatchedTollPlaza> result = service.match(lShape, List.of(toll("End of L", 1, 1)));

        double alongRoute = 2 * KM_PER_DEGREE;
        double straightLine = GeoMath.haversineKm(0, 0, 1, 1);
        assertEquals(alongRoute, result.get(0).distanceFromSourceKm(), 0.05);
        assertTrue(result.get(0).distanceFromSourceKm() > straightLine + 50);
    }

    @Test
    void identicalTollRecordsAreReportedOnce() {
        TollPlaza a = toll("Duplicate", 0, 0.5);
        TollPlaza sameAgain = toll("Duplicate", 0, 0.5);

        List<MatchedTollPlaza> result = service.match(equatorRoute(), List.of(a, sameAgain, a));

        assertEquals(1, result.size());
    }

    @Test
    void differentTollsWithTheSameNameAreBothKept() {
        List<MatchedTollPlaza> result = service.match(equatorRoute(), List.of(
                toll("Same Name", 0, 0.5),
                toll("Same Name", 0, 1.5)));

        assertEquals(2, result.size());
    }

    @Test
    void routePassingTheSameTollTwiceReportsItOnceAtTheFirstPass() {
        Route outAndBack = route(new Coordinate(0, 0), new Coordinate(0, 2), new Coordinate(0, 0));

        List<MatchedTollPlaza> result = service.match(outAndBack, List.of(toll("Twice", 0.005, 1.0)));

        assertEquals(1, result.size());
        assertEquals(1.0 * KM_PER_DEGREE, result.get(0).distanceFromSourceKm(), 0.05);
    }

    @Test
    void emptyOrDegenerateRoutesReturnNothing() {
        List<TollPlaza> tolls = List.of(toll("Any", 0, 0.5));

        assertTrue(service.match(null, tolls).isEmpty());
        assertTrue(service.match(new Route(0, 0, List.of()), tolls).isEmpty());
        assertTrue(service.match(route(new Coordinate(0, 0.5)), tolls).isEmpty());
    }

    @Test
    void emptyOrNullDatasetReturnsNothing() {
        assertTrue(service.match(equatorRoute(), List.of()).isEmpty());
        assertTrue(service.match(equatorRoute(), null).isEmpty());
    }

    @Test
    void nullEntriesInDatasetAreIgnored() {
        List<MatchedTollPlaza> result = service.match(equatorRoute(),
                Arrays.asList(null, toll("Real", 0, 0.5)));

        assertEquals(1, result.size());
    }

    @Test
    void axisOrderIsLatitudeLongitudeInCoordinateAndLongitudeLatitudeInTollPlazaRecord() {
        // route runs east-west along latitude 20 from longitude 70 to 80
        Route route = route(new Coordinate(20, 70), new Coordinate(20, 80));

        TollPlaza correct = new TollPlaza(75, 20, "Correct", "S");   // longitude 75, latitude 20
        TollPlaza swapped = new TollPlaza(20, 75, "Swapped", "S");   // longitude 20, latitude 75

        List<MatchedTollPlaza> result = service.match(route, List.of(correct, swapped));

        assertEquals(List.of("Correct"), result.stream().map(m -> m.tollPlaza().name()).toList());
        assertEquals(5 * GeoMath.haversineKm(20, 70, 20, 71), result.get(0).distanceFromSourceKm(), 0.5);
    }

    @Test
    void tollBeyondTheRouteEndIsClampedToTheEndpoint() {
        // 0.005 degrees (~0.56 km) past the end of the route: matches, position is the full route length
        List<MatchedTollPlaza> result = service.match(equatorRoute(), List.of(toll("Past End", 0, 2.005)));

        assertEquals(1, result.size());
        assertEquals(2 * KM_PER_DEGREE, result.get(0).distanceFromSourceKm(), 0.01);
        assertEquals(0.556, result.get(0).distanceFromRouteKm(), 0.01);

        // 5 km past the end: no match
        assertTrue(service.match(equatorRoute(), List.of(toll("Far Past End", 0, 2.05))).isEmpty());
    }

    @Test
    void repeatedRoutePointsDoNotBreakMatching() {
        Route withRepeat = route(new Coordinate(0, 0), new Coordinate(0, 1), new Coordinate(0, 1), new Coordinate(0, 2));

        List<MatchedTollPlaza> result = service.match(withRepeat, List.of(toll("At Repeat", 0, 1.0)));

        assertEquals(1, result.size());
        assertEquals(KM_PER_DEGREE, result.get(0).distanceFromSourceKm(), 0.01);
    }

    @Test
    void diagonalRouteUsesPerpendicularDistance() {
        // diagonal segment from (0,0) to (2,2): the point (1.01, 0.99) is ~1.5 km off the line, mid-way
        Route diagonal = route(new Coordinate(0, 0), new Coordinate(2, 2));

        List<MatchedTollPlaza> near = service.match(diagonal, List.of(toll("Near Diagonal", 1.01, 0.99)));
        List<MatchedTollPlaza> far = service.match(diagonal, List.of(toll("Far From Diagonal", 1.0, 0.0)));

        assertEquals(1, near.size());
        assertTrue(near.get(0).distanceFromRouteKm() < 2.0);
        assertTrue(far.isEmpty());
    }

    @Test
    void resultListIsUnmodifiable() {
        List<MatchedTollPlaza> result = service.match(equatorRoute(), List.of(toll("T", 0, 0.5)));

        assertThrows(UnsupportedOperationException.class, () -> result.add(result.get(0)));
    }
}
