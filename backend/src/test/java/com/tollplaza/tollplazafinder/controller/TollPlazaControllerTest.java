package com.tollplaza.tollplazafinder.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.tollplaza.tollplazafinder.dto.TollPlazaDetail;
import com.tollplaza.tollplazafinder.dto.TollPlazaRequest;
import com.tollplaza.tollplazafinder.dto.TollPlazaResponse;
import com.tollplaza.tollplazafinder.exception.GlobalExceptionHandler;
import com.tollplaza.tollplazafinder.geocoding.GeocodingException;
import com.tollplaza.tollplazafinder.geocoding.LocationNotFoundException;
import com.tollplaza.tollplazafinder.routing.RouteNotFoundException;
import com.tollplaza.tollplazafinder.routing.RoutingException;
import com.tollplaza.tollplazafinder.service.TollPlazaService;

/**
 * The service is mocked; the real controller, request validation and global exception handler are used.
 */
class TollPlazaControllerTest {

    private static final String URL = "/api/v1/toll-plazas";
    private static final String VALID_BODY = "{\"sourcePincode\":\"560064\",\"destinationPincode\":\"600001\"}";

    private TollPlazaService tollPlazaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        tollPlazaService = mock(TollPlazaService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new TollPlazaController(tollPlazaService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private ResultActions postJson(String body) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void validRequestReturnsRouteAndTollPlazas() throws Exception {
        when(tollPlazaService.findTollPlazas(new TollPlazaRequest("560064", "600001")))
                .thenReturn(new TollPlazaResponse("560064", "600001", 377.85, List.of(
                        new TollPlazaDetail("Devanahalli Toll Plaza", 13.2456, 77.7123, 19.23),
                        new TollPlazaDetail("Mahasamudram Toll Plaza", 12.95, 79.1, 204.2))));

        postJson(VALID_BODY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourcePincode").value("560064"))
                .andExpect(jsonPath("$.destinationPincode").value("600001"))
                .andExpect(jsonPath("$.routeDistanceKm").value(377.85))
                .andExpect(jsonPath("$.tollPlazas.length()").value(2))
                .andExpect(jsonPath("$.tollPlazas[0].tollPlazaName").value("Devanahalli Toll Plaza"))
                .andExpect(jsonPath("$.tollPlazas[0].latitude").value(13.2456))
                .andExpect(jsonPath("$.tollPlazas[0].longitude").value(77.7123))
                .andExpect(jsonPath("$.tollPlazas[0].distanceFromSourceKm").value(19.23))
                .andExpect(jsonPath("$.tollPlazas[1].tollPlazaName").value("Mahasamudram Toll Plaza"));
    }

    @Test
    void routeWithNoTollsReturnsEmptyArray() throws Exception {
        when(tollPlazaService.findTollPlazas(any()))
                .thenReturn(new TollPlazaResponse("560064", "600001", 12.5, List.of()));

        postJson(VALID_BODY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeDistanceKm").value(12.5))
                .andExpect(jsonPath("$.tollPlazas").isArray())
                .andExpect(jsonPath("$.tollPlazas").isEmpty());
    }

    @Test
    void invalidPincodeIsRejectedBeforeTheServiceIsCalled() throws Exception {
        postJson("{\"sourcePincode\":\"0123AB\",\"destinationPincode\":\"600001\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("sourcePincode"));

        verify(tollPlazaService, never()).findTollPlazas(any());
    }

    @Test
    void identicalPincodesAreRejectedBeforeTheServiceIsCalled() throws Exception {
        postJson("{\"sourcePincode\":\"560064\",\"destinationPincode\":\"560064\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("destinationPincode"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Source and destination PIN codes must not be identical"));

        verify(tollPlazaService, never()).findTollPlazas(any());
    }

    @Test
    void malformedJsonReturnsStructuredError() throws Exception {
        postJson("{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));

        verify(tollPlazaService, never()).findTollPlazas(any());
    }

    @Test
    void pincodeNotFoundReturns404() throws Exception {
        when(tollPlazaService.findTollPlazas(any()))
                .thenThrow(new LocationNotFoundException("No location found for PIN code 600001"));

        postJson(VALID_BODY)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("No location found for PIN code 600001"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void noRouteReturns404() throws Exception {
        when(tollPlazaService.findTollPlazas(any()))
                .thenThrow(new RouteNotFoundException("No route found between the given locations"));

        postJson(VALID_BODY)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No route found between the given locations"));
    }

    @Test
    void geocodingProviderFailureReturns502() throws Exception {
        when(tollPlazaService.findTollPlazas(any()))
                .thenThrow(new GeocodingException("Geoapify geocoding failed for PIN code 560064 (HTTP 500)"));

        postJson(VALID_BODY)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.message").value("Geoapify geocoding failed for PIN code 560064 (HTTP 500)"));
    }

    @Test
    void routingProviderFailureReturns502() throws Exception {
        when(tollPlazaService.findTollPlazas(any()))
                .thenThrow(new RoutingException("Could not reach Geoapify routing (connection error or timeout)"));

        postJson(VALID_BODY)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message")
                        .value("Could not reach Geoapify routing (connection error or timeout)"));
    }

    @Test
    void getIsNotAllowedOnTheEndpoint() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isMethodNotAllowed());
    }
}
