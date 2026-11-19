package com.tollplaza.tollplazafinder.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiConfigTest {

    private static final String PATH = "$.paths['/api/v1/toll-plazas'].post";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsDescribeTheApiAndTheTollPlazaEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Toll Plaza Finder API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.servers[0].url").value("https://tollplazafinder.onrender.com"))
                .andExpect(jsonPath(PATH).exists())
                .andExpect(jsonPath(PATH + ".responses['200']").exists())
                .andExpect(jsonPath(PATH + ".responses['400']").exists())
                .andExpect(jsonPath(PATH + ".responses['404']").exists())
                .andExpect(jsonPath(PATH + ".responses['502']").exists())
                .andExpect(jsonPath("$.components.schemas.TollPlazaRequest").exists())
                .andExpect(jsonPath("$.components.schemas.TollPlazaResponse").exists())
                .andExpect(jsonPath("$.components.schemas.TollPlazaDetail").exists())
                .andExpect(jsonPath("$.components.schemas.ErrorResponse").exists());
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
