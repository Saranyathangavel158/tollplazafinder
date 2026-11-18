package com.tollplaza.tollplazafinder.geocoding;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Builds the {@link RestClient} used for Geoapify, with connect and read timeouts.
 * Tests replace this client with one bound to a mock server.
 */
@Configuration
public class GeoapifyClientConfig {

    @Bean
    RestClient geoapifyRestClient(
            @Value("${geoapify.base-url:https://api.geoapify.com}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
