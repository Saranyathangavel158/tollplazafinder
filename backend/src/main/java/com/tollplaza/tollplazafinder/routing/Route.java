package com.tollplaza.tollplazafinder.routing;

import java.util.List;

import com.tollplaza.tollplazafinder.geocoding.Coordinate;

/**
 * A driving route between two points.
 *
 * @param distanceKm      total distance in kilometres (Geoapify reports metres)
 * @param durationSeconds estimated travel time in seconds
 * @param geometry        ordered route points; GeoJSON [longitude, latitude] pairs are mapped
 *                        into {@link Coordinate} so the axis order can never be confused
 */
public record Route(double distanceKm, double durationSeconds, List<Coordinate> geometry) {
}
