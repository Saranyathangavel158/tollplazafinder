package com.tollplaza.tollplazafinder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.tollplaza.tollplazafinder.config.CacheConfig;
import com.tollplaza.tollplazafinder.dto.TollPlazaRequest;
import com.tollplaza.tollplazafinder.dto.TollPlazaResponse;
import com.tollplaza.tollplazafinder.geocoding.Coordinate;
import com.tollplaza.tollplazafinder.geocoding.GeocodingException;
import com.tollplaza.tollplazafinder.geocoding.GeoapifyGeocodingService;
import com.tollplaza.tollplazafinder.routing.GeoapifyRoutingService;
import com.tollplaza.tollplazafinder.routing.Route;
import com.tollplaza.tollplazafinder.routing.RoutingException;

/**
 * Real Spring context (so the caching proxy is active) with only the two Geoapify services mocked,
 * so no network is used and provider calls can be counted.
 */
@SpringBootTest
class TollPlazaServiceCachingTest {

    private static final Coordinate BENGALURU = new Coordinate(13.0, 77.6);
    private static final Coordinate CHENNAI = new Coordinate(13.08, 80.27);

    @Autowired
    private TollPlazaService tollPlazaService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private GeoapifyGeocodingService geocodingService;

    @MockitoBean
    private GeoapifyRoutingService routingService;

    @BeforeEach
    void setUp() {
        cacheManager.getCache(CacheConfig.TOLL_PLAZAS_CACHE).clear();
        when(geocodingService.geocode("560064")).thenReturn(BENGALURU);
        when(geocodingService.geocode("600001")).thenReturn(CHENNAI);
        when(routingService.getRoute(any(), any()))
                .thenReturn(new Route(350.0, 18000, List.of(BENGALURU, CHENNAI)));
    }

    @Test
    void firstRequestCallsGeocodingAndRouting() {
        TollPlazaResponse response = tollPlazaService.findTollPlazas(new TollPlazaRequest("560064", "600001"));

        assertEquals("560064", response.sourcePincode());
        verify(geocodingService, times(2)).geocode(any());
        verify(routingService, times(1)).getRoute(any(), any());
    }

    @Test
    void repeatedRequestIsServedFromCacheWithoutProviderCalls() {
        TollPlazaResponse first = tollPlazaService.findTollPlazas(new TollPlazaRequest("560064", "600001"));
        TollPlazaResponse second = tollPlazaService.findTollPlazas(new TollPlazaRequest("560064", "600001"));

        assertSame(first, second);
        verify(geocodingService, times(2)).geocode(any()); // still only the first request's two calls
        verify(routingService, times(1)).getRoute(any(), any());
    }

    @Test
    void reversedPincodesUseASeparateCacheEntry() {
        tollPlazaService.findTollPlazas(new TollPlazaRequest("560064", "600001"));
        TollPlazaResponse reversed = tollPlazaService.findTollPlazas(new TollPlazaRequest("600001", "560064"));

        assertEquals("600001", reversed.sourcePincode());
        assertEquals("560064", reversed.destinationPincode());
        verify(geocodingService, times(4)).geocode(any());
        verify(routingService, times(2)).getRoute(any(), any());

        // both directions are now cached
        tollPlazaService.findTollPlazas(new TollPlazaRequest("560064", "600001"));
        tollPlazaService.findTollPlazas(new TollPlazaRequest("600001", "560064"));
        verify(routingService, times(2)).getRoute(any(), any());
    }

    @Test
    void failedGeocodingIsNotCachedAndIsRetried() {
        when(geocodingService.geocode("600001"))
                .thenThrow(new GeocodingException("Geoapify geocoding failed (HTTP 500)"))
                .thenReturn(CHENNAI);

        assertThrows(GeocodingException.class,
                () -> tollPlazaService.findTollPlazas(new TollPlazaRequest("560064", "600001")));
        TollPlazaResponse retry = tollPlazaService.findTollPlazas(new TollPlazaRequest("560064", "600001"));

        assertNotNull(retry);
        verify(routingService, times(1)).getRoute(any(), any()); // only the successful attempt reached routing
        verify(geocodingService, times(2)).geocode("600001");    // the failure was retried, not replayed
    }

    @Test
    void failedRoutingIsNotCachedAndIsRetried() {
        when(routingService.getRoute(any(), any()))
                .thenThrow(new RoutingException("Geoapify routing failed (HTTP 503)"))
                .thenReturn(new Route(350.0, 18000, List.of(BENGALURU, CHENNAI)));

        assertThrows(RoutingException.class,
                () -> tollPlazaService.findTollPlazas(new TollPlazaRequest("560064", "600001")));
        TollPlazaResponse retry = tollPlazaService.findTollPlazas(new TollPlazaRequest("560064", "600001"));

        assertEquals(350.0, retry.routeDistanceKm());
        verify(routingService, times(2)).getRoute(any(), any());

        // the success is cached from now on
        tollPlazaService.findTollPlazas(new TollPlazaRequest("560064", "600001"));
        verify(routingService, times(2)).getRoute(any(), any());
    }

    @Test
    void cacheIsConfiguredWithSizeLimitAndTwentyFourHourExpiry() {
        CaffeineCache cache = (CaffeineCache) cacheManager.getCache(CacheConfig.TOLL_PLAZAS_CACHE);
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = cache.getNativeCache();

        assertEquals(1000L, nativeCache.policy().eviction().orElseThrow().getMaximum());
        assertEquals(Duration.ofHours(24),
                Duration.ofNanos(nativeCache.policy().expireAfterWrite().orElseThrow()
                        .getExpiresAfter(java.util.concurrent.TimeUnit.NANOSECONDS)));
    }
}
