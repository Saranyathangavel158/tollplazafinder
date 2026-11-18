package com.tollplaza.tollplazafinder.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Class-level constraint: source and destination PIN codes must differ.
 */
@Documented
@Constraint(validatedBy = DifferentPincodesValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DifferentPincodes {

    String message() default "Source and destination PIN codes must not be identical";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
