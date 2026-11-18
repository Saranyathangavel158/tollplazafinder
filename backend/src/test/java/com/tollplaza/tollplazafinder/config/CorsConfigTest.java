package com.tollplaza.tollplazafinder.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigTest {

    private static final String URL = "/api/v1/toll-plazas";
    private static final String ORIGIN = "https://tollplazafinder.vercel.app";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflightFromFrontendIsAllowed() throws Exception {
        mockMvc.perform(options(URL)
                        .header("Origin", ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN));
    }

    @Test
    void preflightFromOtherOriginIsRejected() throws Exception {
        mockMvc.perform(options(URL)
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
