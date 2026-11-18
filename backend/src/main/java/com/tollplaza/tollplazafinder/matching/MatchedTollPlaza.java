package com.tollplaza.tollplazafinder.matching;

import com.tollplaza.tollplazafinder.dataset.TollPlaza;

/**
 * A toll plaza found along a route.
 *
 * @param tollPlaza            the dataset record
 * @param distanceFromSourceKm distance travelled ALONG the route from the source to the point
 *                             on the route closest to the toll plaza
 * @param distanceFromRouteKm  how far the toll plaza is from that closest route point
 */
public record MatchedTollPlaza(TollPlaza tollPlaza, double distanceFromSourceKm, double distanceFromRouteKm) {
}
