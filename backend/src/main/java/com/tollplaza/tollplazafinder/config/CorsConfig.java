package com.tollplaza.tollplazafinder.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The single CORS configuration: lets the deployed frontend call /api/**.
 * Allowed origins can be overridden (comma-separated) with the CORS_ALLOWED_ORIGINS environment variable.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(
            @Value("${tollplaza.cors.allowed-origins:https://tollplazafinder.vercel.app}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept", "Authorization")
                .maxAge(3600);
    }
}
