package com.tollplaza.tollplazafinder.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

/**
 * OpenAPI metadata shown in Swagger UI (/swagger-ui.html) and served at /v3/api-docs.
 * Contains no secrets or environment values.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tollPlazaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Toll Plaza Finder API")
                        .description("REST API to find toll plazas between two Indian PIN codes.")
                        .version("1.0.0"))
                .addServersItem(new Server()
                        .url("https://tollplazafinder.onrender.com")
                        .description("Production (Render)"));
    }
}
