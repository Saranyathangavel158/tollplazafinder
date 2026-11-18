package com.tollplaza.tollplazafinder.matching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.tollplaza.tollplazafinder.dataset.TollPlaza;
import com.tollplaza.tollplazafinder.geocoding.Coordinate;
import com.tollplaza.tollplazafinder.routing.Route;

/**
 * Finds the toll plazas that lie along a route.
 *
 * <p>For every toll plaza the service finds the closest point on the route LINE (any segment, not
 * only the vertices). If that point is within the configured threshold, the toll is a match and its
 * position is the cumulative distance travelled along the route up to that point.
 */
@Service
public class TollPlazaMatchingService {

    private static final double KM_PER_DEGREE_LAT = Math.PI * GeoMath.EARTH_RADIUS_KM / 180.0;

    private final double thresholdKm;

    public TollPlazaMatchingService(
            @Value("${tollplaza.matching.threshold-km:2.0}") double thresholdKm) {
        if (!(thresholdKm > 0) || Double.isInfinite(thresholdKm)) {
            throw new IllegalArgumentException(
                    "tollplaza.matching.threshold-km must be a positive number but was " + thresholdKm);
        }
        this.thresholdKm = thresholdKm;
    }

    public double getThresholdKm() {
        return thresholdKm;
    }

    /**
     * @return matched toll plazas ordered by distance travelled from the source; empty when the
     *         route has fewer than two points, the dataset is empty, or either argument is null
     */
    public List<MatchedTollPlaza> match(Route route, Collection<TollPlaza> tollPlazas) {
        if (route == null || route.geometry() == null || route.geometry().size() < 2
                || tollPlazas == null || tollPlazas.isEmpty()) {
            return List.of();
        }

        List<Coordinate> points = route.geometry();
        int segmentCount = points.size() - 1;

        // cumulativeKm[i] = distance along the route from the source to points.get(i)
        double[] cumulativeKm = new double[points.size()];
        for (int i = 1; i < points.size(); i++) {
            cumulativeKm[i] = cumulativeKm[i - 1] + GeoMath.haversineKm(points.get(i - 1), points.get(i));
        }

        // identical records are only reported once
        Set<TollPlaza> unique = new LinkedHashSet<>(tollPlazas);
        List<MatchedTollPlaza> matches = new ArrayList<>();

        for (TollPlaza plaza : unique) {
            if (plaza == null) {
                continue;
            }
            MatchedTollPlaza match = matchOne(plaza, points, cumulativeKm, segmentCount);
            if (match != null) {
                matches.add(match);
            }
        }

        matches.sort(Comparator.comparingDouble(MatchedTollPlaza::distanceFromSourceKm)
                .thenComparing(m -> m.tollPlaza().name()));
        return List.copyOf(matches);
    }

    private MatchedTollPlaza matchOne(TollPlaza plaza, List<Coordinate> points, double[] cumulativeKm,
            int segmentCount) {
        double latMargin = thresholdKm / KM_PER_DEGREE_LAT;
        double cosLat = Math.max(Math.cos(Math.toRadians(plaza.latitude())), 1e-6);
        double lonMargin = latMargin / cosLat;

        double bestDistanceKm = Double.MAX_VALUE;
        double bestAlongKm = 0;

        for (int i = 0; i < segmentCount; i++) {
            Coordinate a = points.get(i);
            Coordinate b = points.get(i + 1);

            // cheap bounding-box rejection before doing any trigonometry
            if (plaza.latitude() < Math.min(a.latitude(), b.latitude()) - latMargin
                    || plaza.latitude() > Math.max(a.latitude(), b.latitude()) + latMargin
                    || plaza.longitude() < Math.min(a.longitude(), b.longitude()) - lonMargin
                    || plaza.longitude() > Math.max(a.longitude(), b.longitude()) + lonMargin) {
                continue;
            }

            double t = projectionFraction(plaza, a, b);
            Coordinate nearest = new Coordinate(
                    a.latitude() + t * (b.latitude() - a.latitude()),
                    a.longitude() + t * (b.longitude() - a.longitude()));
            double distanceKm = GeoMath.haversineKm(plaza.latitude(), plaza.longitude(),
                    nearest.latitude(), nearest.longitude());

            // strict "<" keeps the earliest segment when a route passes the same spot twice
            if (distanceKm < bestDistanceKm) {
                bestDistanceKm = distanceKm;
                bestAlongKm = cumulativeKm[i] + GeoMath.haversineKm(a, nearest);
            }
        }

        if (bestDistanceKm > thresholdKm) {
            return null;
        }
        return new MatchedTollPlaza(plaza, bestAlongKm, bestDistanceKm);
    }

    /**
     * Position (0..1) of the point on segment a-b closest to the plaza. The projection is done in a
     * local flat approximation (x = east, y = north, in km) centred on the segment, which is accurate
     * for short segments; the resulting distances are then measured with Haversine.
     */
    private static double projectionFraction(TollPlaza plaza, Coordinate a, Coordinate b) {
        double cosLat = Math.cos(Math.toRadians((a.latitude() + b.latitude()) / 2));
        double kmPerDegreeLon = KM_PER_DEGREE_LAT * cosLat;

        double abX = (b.longitude() - a.longitude()) * kmPerDegreeLon;
        double abY = (b.latitude() - a.latitude()) * KM_PER_DEGREE_LAT;
        double apX = (plaza.longitude() - a.longitude()) * kmPerDegreeLon;
        double apY = (plaza.latitude() - a.latitude()) * KM_PER_DEGREE_LAT;

        double lengthSquared = abX * abX + abY * abY;
        if (lengthSquared == 0) {
            return 0; // zero-length segment (repeated point)
        }
        double t = (apX * abX + apY * abY) / lengthSquared;
        return Math.max(0, Math.min(1, t));
    }
}
