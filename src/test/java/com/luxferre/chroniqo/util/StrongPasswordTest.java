package com.luxferre.chroniqo.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StrongPasswordTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    private record Subject(@StrongPassword String password) {
    }

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        factory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Secur3P@ssword", "Ab1!Ab1!Ab1!"})
    void strongPassword_hasNoViolations(String password) {
        assertThat(validator.validate(new Subject(password))).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void missingPassword_isRejectedAsRequired(String password) {
        Set<ConstraintViolation<Subject>> violations = validator.validate(new Subject(password));

        assertThat(violations).singleElement()
                .satisfies(v -> assertThat(v.getMessage()).isEqualTo("Password is required"));
    }

    @Test
    void tooShortPassword_reportsMinLength() {
        assertThat(messageFor("Aa1!")).isEqualTo("Password must be at least 12 characters");
    }

    @Test
    void tooLongPassword_reportsMaxLength() {
        assertThat(messageFor("Aa1!" + "a".repeat(PasswordValidator.MAX_LENGTH)))
                .isEqualTo("Password must not exceed 72 characters");
    }

    @Test
    void multiByteCharacters_countTowardsTheByteLimit() {
        // 40 characters, but 80 UTF-8 bytes
        assertThat(messageFor("Aa1!" + "\u00fc".repeat(36)))
                .isEqualTo("Password must not exceed 72 bytes (non-ASCII characters count as several)");
    }

    @Test
    void missingCharacterClasses_reportTheFirstViolatedRule() {
        assertThat(messageFor("secur3p@ssword!")).containsIgnoringCase("uppercase");
        assertThat(messageFor("SECUR3P@SSWORD!")).containsIgnoringCase("lowercase");
        assertThat(messageFor("SecureP@ssword!")).containsIgnoringCase("digit");
        assertThat(messageFor("Secur3Password")).containsIgnoringCase("special");
    }

    @Test
    void violation_isReportedOnThePasswordProperty() {
        Set<ConstraintViolation<Subject>> violations = validator.validate(new Subject("weak"));

        assertThat(violations).singleElement()
                .satisfies(v -> assertThat(v.getPropertyPath()).hasToString("password"));
    }

    private String messageFor(String password) {
        Set<ConstraintViolation<Subject>> violations = validator.validate(new Subject(password));
        assertThat(violations).hasSize(1);
        return violations.iterator().next().getMessage();
    }
}
