package com.tollplaza.tollplazafinder.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tollplaza.tollplazafinder.dto.TollPlazaRequest;
import com.tollplaza.tollplazafinder.dto.TollPlazaResponse;
import com.tollplaza.tollplazafinder.service.TollPlazaService;

import jakarta.validation.Valid;

/**
 * POST /api/v1/toll-plazas: toll plazas between two PIN codes.
 * Validation errors and provider failures are handled by the global exception handler.
 */
@RestController
@RequestMapping("/api/v1/toll-plazas")
public class TollPlazaController {

    private final TollPlazaService tollPlazaService;

    public TollPlazaController(TollPlazaService tollPlazaService) {
        this.tollPlazaService = tollPlazaService;
    }

    @PostMapping
    public TollPlazaResponse findTollPlazas(@Valid @RequestBody TollPlazaRequest request) {
        return tollPlazaService.findTollPlazas(request);
    }
}
