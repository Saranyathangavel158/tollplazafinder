package com.tollplaza.tollplazafinder.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.tollplaza.tollplazafinder.config.CacheConfig;
import com.tollplaza.tollplazafinder.dataset.TollPlazaDataService;
import com.tollplaza.tollplazafinder.dto.TollPlazaDetail;
import com.tollplaza.tollplazafinder.dto.TollPlazaRequest;
import com.tollplaza.tollplazafinder.dto.TollPlazaResponse;
import com.tollplaza.tollplazafinder.geocoding.Coordinate;
import com.tollplaza.tollplazafinder.geocoding.GeoapifyGeocodingService;
import com.tollplaza.tollplazafinder.matching.MatchedTollPlaza;
import com.tollplaza.tollplazafinder.matching.TollPlazaMatchingService;
import com.tollplaza.tollplazafinder.routing.GeoapifyRoutingService;
import com.tollplaza.tollplazafinder.routing.Route;

/**
 * Orchestrates one request: geocode both PIN codes, fetch the driving route, match toll plazas
 * against it, and build the response. Provider failures propagate as the specific
 * Geocoding/Routing exceptions and are turned into HTTP errors by the global exception handler.
 */
@Service
public class TollPlazaService {

    private static final Logger log = LoggerFactory.getLogger(TollPlazaService.class);

    private final GeoapifyGeocodingService geocodingService;
    private final GeoapifyRoutingService routingService;
    private final TollPlazaMatchingService matchingService;
    private final TollPlazaDataService dataService;

    public TollPlazaService(GeoapifyGeocodingService geocodingService, GeoapifyRoutingService routingService,
            TollPlazaMatchingService matchingService, TollPlazaDataService dataService) {
        this.geocodingService = geocodingService;
        this.routingService = routingService;
        this.matchingService = matchingService;
        this.dataService = dataService;
    }

    /**
     * Cached per (source, destination) pair; the order matters, so B-to-A is a separate entry.
     * Exceptions are never cached, so failed provider calls are retried on the next request.
     */
    @Cacheable(cacheNames = CacheConfig.TOLL_PLAZAS_CACHE,
            key = "#request.sourcePincode() + '->' + #request.destinationPincode()")
    public TollPlazaResponse findTollPlazas(TollPlazaRequest request) {
        Coordinate source = geocodingService.geocode(request.sourcePincode());
        Coordinate destination = geocodingService.geocode(request.destinationPincode());
        Route route = routingService.getRoute(source, destination);

        List<MatchedTollPlaza> matches = matchingService.match(route, dataService.getAll());
        log.info("Route {} -> {}: {} km, {} toll plaza(s) found", request.sourcePincode(),
                request.destinationPincode(), round(route.distanceKm()), matches.size());

        List<TollPlazaDetail> tollPlazas = matches.stream()
                .map(m -> new TollPlazaDetail(
                        m.tollPlaza().name(),
                        m.tollPlaza().latitude(),
                        m.tollPlaza().longitude(),
                        round(m.distanceFromSourceKm())))
                .toList();

        return new TollPlazaResponse(request.sourcePincode(), request.destinationPincode(),
                round(route.distanceKm()), tollPlazas);
    }

    /** Distances are reported to 2 decimal places (10 metres). */
    private static double round(double km) {
        return Math.round(km * 100.0) / 100.0;
    }
}
