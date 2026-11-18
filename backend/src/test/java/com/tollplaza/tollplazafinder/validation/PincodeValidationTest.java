package com.tollplaza.tollplazafinder.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.tollplaza.tollplazafinder.dto.TollPlazaRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class PincodeValidationTest {

    private static final String FORMAT_MSG = "PIN code must be exactly 6 digits and must not start with 0";

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static Set<ConstraintViolation<TollPlazaRequest>> validate(String source, String destination) {
        return validator.validate(new TollPlazaRequest(source, destination));
    }

    private static List<String> messagesFor(Set<ConstraintViolation<TollPlazaRequest>> violations, String field) {
        return violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals(field))
                .map(ConstraintViolation::getMessage)
                .toList();
    }

    @ParameterizedTest
    @ValueSource(strings = { "110001", "560001", "999999", "100000", "400001" })
    void validPincodesAreAccepted(String pin) {
        assertTrue(messagesFor(validate(pin, "600001"), "sourcePincode").isEmpty());
    }

    @Test
    void pincodeBeginningWithZeroIsRejected() {
        assertEquals(List.of(FORMAT_MSG), messagesFor(validate("012345", "600001"), "sourcePincode"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "11000A", "ABCDEF", "11 001", " 110001", "110001 ", "11-001", "11000#",
            "११०००१", "110001\n" })
    void lettersSpacesSpecialCharactersAndNonAsciiDigitsAreRejected(String pin) {
        assertEquals(List.of(FORMAT_MSG), messagesFor(validate(pin, "600001"), "sourcePincode"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "1", "11000", "1100011", "12345678901" })
    void wrongLengthIsRejected(String pin) {
        assertEquals(List.of(FORMAT_MSG), messagesFor(validate("600001", pin), "destinationPincode"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   " })
    void emptyOrBlankIsRejected(String pin) {
        assertEquals(List.of(FORMAT_MSG), messagesFor(validate(pin, "600001"), "sourcePincode"));
    }

    @ParameterizedTest
    @NullSource
    void nullIsRejectedAsRequired(String pin) {
        Set<ConstraintViolation<TollPlazaRequest>> violations = validate(pin, pin);
        assertEquals(List.of("PIN code is required"), messagesFor(violations, "sourcePincode"));
        assertEquals(List.of("PIN code is required"), messagesFor(violations, "destinationPincode"));
        // no extra "identical" error when both are missing
        assertEquals(2, violations.size());
    }

    @Test
    void identicalSourceAndDestinationIsRejected() {
        Set<ConstraintViolation<TollPlazaRequest>> violations = validate("110001", "110001");
        assertEquals(List.of("Source and destination PIN codes must not be identical"),
                messagesFor(violations, "destinationPincode"));
        assertEquals(1, violations.size());
    }

    @Test
    void validRequestHasNoViolations() {
        assertTrue(validate("110001", "560001").isEmpty());
    }
}
