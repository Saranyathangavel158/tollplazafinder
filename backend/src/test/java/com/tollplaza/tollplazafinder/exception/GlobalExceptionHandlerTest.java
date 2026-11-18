package com.tollplaza.tollplazafinder.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.tollplaza.tollplazafinder.dto.TollPlazaRequest;
import com.tollplaza.tollplazafinder.dto.TollPlazaResponse;

import jakarta.validation.Valid;

/**
 * Exercises the exception handler through a throw-away controller that exists only in tests
 * (the real endpoint is not implemented yet).
 */
class GlobalExceptionHandlerTest {

    @RestController
    static class StubController {
        @PostMapping("/test/toll-plazas")
        TollPlazaResponse find(@Valid @RequestBody TollPlazaRequest request) {
            return new TollPlazaResponse(request.sourcePincode(), request.destinationPincode(), 0, List.of());
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new StubController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private ResultActions postJson(String body) throws Exception {
        return mockMvc.perform(post("/test/toll-plazas").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void validRequestPasses() throws Exception {
        postJson("{\"sourcePincode\":\"110001\",\"destinationPincode\":\"560001\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourcePincode").value("110001"));
    }

    @Test
    void invalidPincodeReturnsStructuredError() throws Exception {
        postJson("{\"sourcePincode\":\"01A\",\"destinationPincode\":\"560001\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("sourcePincode"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("PIN code must be exactly 6 digits and must not start with 0"));
    }

    @Test
    void missingPincodeIsReported() throws Exception {
        postJson("{\"sourcePincode\":\"110001\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("destinationPincode"))
                .andExpect(jsonPath("$.errors[0].message").value("PIN code is required"));
    }

    @Test
    void identicalPincodesReturnClearError() throws Exception {
        postJson("{\"sourcePincode\":\"110001\",\"destinationPincode\":\"110001\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("destinationPincode"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Source and destination PIN codes must not be identical"));
    }

    @Test
    void malformedJsonReturnsStructuredError() throws Exception {
        postJson("{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void missingBodyReturnsStructuredError() throws Exception {
        mockMvc.perform(post("/test/toll-plazas").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
    }
}
