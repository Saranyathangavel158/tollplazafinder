package com.tollplaza.tollplazafinder.validation;

import com.tollplaza.tollplazafinder.dto.TollPlazaRequest;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Compares the two PIN codes. Null values are left to the field-level constraints,
 * so a missing PIN code is not also reported as a mismatch problem.
 * The violation is attached to the destinationPincode field.
 */
public class DifferentPincodesValidator implements ConstraintValidator<DifferentPincodes, TollPlazaRequest> {

    @Override
    public boolean isValid(TollPlazaRequest request, ConstraintValidatorContext context) {
        if (request == null || request.sourcePincode() == null || request.destinationPincode() == null) {
            return true;
        }
        if (!request.sourcePincode().equals(request.destinationPincode())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("destinationPincode")
                .addConstraintViolation();
        return false;
    }
}
