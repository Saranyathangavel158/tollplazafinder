package com.tollplaza.tollplazafinder.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.tollplaza.tollplazafinder.geocoding.Coordinate;

class GeoMathTest {

    @Test
    void oneDegreeOfLongitudeOnTheEquatorIsAbout111Km() {
        assertEquals(111.195, GeoMath.haversineKm(0, 0, 0, 1), 0.01);
    }

    @Test
    void distanceIsSymmetricAndZeroForSamePoint() {
        Coordinate delhi = new Coordinate(28.6139, 77.2090);
        Coordinate mumbai = new Coordinate(19.0760, 72.8777);

        assertEquals(0.0, GeoMath.haversineKm(delhi, delhi), 1e-9);
        assertEquals(GeoMath.haversineKm(delhi, mumbai), GeoMath.haversineKm(mumbai, delhi), 1e-9);
    }

    @Test
    void delhiToMumbaiIsAbout1150Km() {
        Coordinate delhi = new Coordinate(28.6139, 77.2090);
        Coordinate mumbai = new Coordinate(19.0760, 72.8777);

        assertEquals(1153, GeoMath.haversineKm(delhi, mumbai), 10);
    }
}
