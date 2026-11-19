package com.tollplaza.tollplazafinder.dto;

import com.tollplaza.tollplazafinder.validation.DifferentPincodes;
import com.tollplaza.tollplazafinder.validation.IndianPincode;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Incoming request: the two PIN codes of the journey.
 */
@DifferentPincodes
@Schema(description = "The two PIN codes of the journey. They must be different.")
public record TollPlazaRequest(
        @Schema(description = "Source PIN code: exactly 6 digits, first digit 1-9",
                example = "560064", pattern = "[1-9][0-9]{5}", requiredMode = Schema.RequiredMode.REQUIRED)
        @IndianPincode String sourcePincode,
        @Schema(description = "Destination PIN code: exactly 6 digits, first digit 1-9, different from the source",
                example = "600001", pattern = "[1-9][0-9]{5}", requiredMode = Schema.RequiredMode.REQUIRED)
        @IndianPincode String destinationPincode) {
}
