package com.tollplaza.tollplazafinder.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Indian PIN code: required, exactly six ASCII digits, first digit 1-9.
 * Composed from standard constraints, so no custom validator class is needed.
 * A null value reports "is required"; any other bad value reports the format message.
 */
@NotNull(message = "PIN code is required")
@Pattern(regexp = "[1-9][0-9]{5}",
        message = "PIN code must be exactly 6 digits and must not start with 0")
@Documented
@Constraint(validatedBy = {})
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface IndianPincode {

    String message() default "Invalid PIN code";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
