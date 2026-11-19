package com.tollplaza.tollplazafinder.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tollplaza.tollplazafinder.dto.TollPlazaRequest;
import com.tollplaza.tollplazafinder.dto.TollPlazaResponse;
import com.tollplaza.tollplazafinder.exception.ErrorResponse;
import com.tollplaza.tollplazafinder.service.TollPlazaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * POST /api/v1/toll-plazas: toll plazas between two PIN codes.
 * Validation errors and provider failures are handled by the global exception handler.
 */
@RestController
@RequestMapping("/api/v1/toll-plazas")
@Tag(name = "Toll Plazas", description = "Find toll plazas along the driving route between two PIN codes")
public class TollPlazaController {

    private final TollPlazaService tollPlazaService;

    public TollPlazaController(TollPlazaService tollPlazaService) {
        this.tollPlazaService = tollPlazaService;
    }

    @Operation(
            summary = "Find toll plazas between two PIN codes",
            description = "Geocodes both PIN codes, computes the driving route between them and returns the toll "
                    + "plazas located on that route, ordered by distance from the source. Results are cached for "
                    + "24 hours per ordered source/destination pair.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Route found; toll plazas listed (possibly empty)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TollPlazaResponse.class),
                            examples = @ExampleObject(name = "Bengaluru to Chennai", value = """
                                    {
                                      "sourcePincode": "560064",
                                      "destinationPincode": "600001",
                                      "routeDistanceKm": 346.12,
                                      "tollPlazas": [
                                        {
                                          "tollPlazaName": "Electronic City Toll Plaza",
                                          "latitude": 12.8452,
                                          "longitude": 77.6602,
                                          "distanceFromSourceKm": 18.45
                                        },
                                        {
                                          "tollPlazaName": "Krishnagiri Toll Plaza",
                                          "latitude": 12.5266,
                                          "longitude": 78.2141,
                                          "distanceFromSourceKm": 92.7
                                        }
                                      ]
                                    }"""))),
            @ApiResponse(responseCode = "400",
                    description = "Validation failed (missing/invalid PIN code, identical PIN codes) "
                            + "or the body is missing or not valid JSON",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Invalid PIN code", value = """
                                    {
                                      "timestamp": "2026-11-19T10:15:30.123Z",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "Validation failed",
                                      "errors": [
                                        {
                                          "field": "sourcePincode",
                                          "message": "PIN code must be exactly 6 digits and must not start with 0"
                                        }
                                      ]
                                    }"""))),
            @ApiResponse(responseCode = "404",
                    description = "A PIN code could not be located, or no driving route exists between them",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Location not found", value = """
                                    {
                                      "timestamp": "2026-11-19T10:15:30.123Z",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "No location found for PIN code 999999",
                                      "errors": []
                                    }"""))),
            @ApiResponse(responseCode = "502",
                    description = "The upstream geocoding/routing provider (Geoapify) failed or timed out",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Provider failure", value = """
                                    {
                                      "timestamp": "2026-11-19T10:15:30.123Z",
                                      "status": 502,
                                      "error": "Bad Gateway",
                                      "message": "Could not reach Geoapify routing (connection error or timeout)",
                                      "errors": []
                                    }""")))
    })
    @PostMapping
    public TollPlazaResponse findTollPlazas(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Source and destination PIN codes",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TollPlazaRequest.class),
                            examples = @ExampleObject(name = "Bengaluru to Chennai", value = """
                                    {
                                      "sourcePincode": "560064",
                                      "destinationPincode": "600001"
                                    }""")))
            @Valid @RequestBody TollPlazaRequest request) {
        return tollPlazaService.findTollPlazas(request);
    }
}
