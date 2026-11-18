package com.tollplaza.tollplazafinder.matching;

import com.tollplaza.tollplazafinder.geocoding.Coordinate;

/**
 * Small geographic helpers. All inputs are degrees; all distances are kilometres.
 */
final class GeoMath {

    static final double EARTH_RADIUS_KM = 6371.0088;

    private GeoMath() {
    }

    static double haversineKm(Coordinate a, Coordinate b) {
        return haversineKm(a.latitude(), a.longitude(), b.latitude(), b.longitude());
    }

    static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(h)));
    }
}
