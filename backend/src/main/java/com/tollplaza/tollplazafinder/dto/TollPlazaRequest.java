package com.tollplaza.tollplazafinder.dto;

import com.tollplaza.tollplazafinder.validation.DifferentPincodes;
import com.tollplaza.tollplazafinder.validation.IndianPincode;

/**
 * Incoming request: the two PIN codes of the journey.
 */
@DifferentPincodes
public record TollPlazaRequest(
        @IndianPincode String sourcePincode,
        @IndianPincode String destinationPincode) {
}
