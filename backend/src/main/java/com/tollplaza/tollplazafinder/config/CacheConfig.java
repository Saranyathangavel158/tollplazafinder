package com.tollplaza.tollplazafinder.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Turns on Spring's annotation-driven caching. The Caffeine cache itself (name, size, expiry)
 * is configured through the spring.cache.* properties in application.properties.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String TOLL_PLAZAS_CACHE = "tollPlazas";
}
