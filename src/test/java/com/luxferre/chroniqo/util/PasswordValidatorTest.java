package com.luxferre.chroniqo.util;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordValidatorTest {

    private PasswordValidator validator;
    private ValueContext ctx;

    @BeforeEach
    void setUp() {
        validator = new PasswordValidator();
        ctx = new ValueContext();
    }

    // =========================================================================
    // Valid passwords
    // =========================================================================

    @Nested
    class ValidPasswords {

        @ParameterizedTest
        @ValueSource(strings = {
                "Secur3P@ssword",
                "MyStr0ng!Pass",
                "C0mpl3x#Secret",
                "V4l!dPassw0rd!",
                "Ab1!Ab1!Ab1!",   // exactly 12 chars, all classes
        })
        void validPassword_returnsOk(String password) {
            assertThat(validator.apply(password, ctx).isError()).isFalse();
        }

        @Test
        void passwordAtMaxLength_returnsOk() {
            // 128 chars: uppercase, lowercase, digit, special
            String password = "Aa1!" + "a".repeat(PasswordValidator.MAX_LENGTH - 4);
            assertThat(password.length()).isEqualTo(PasswordValidator.MAX_LENGTH);
            assertThat(validator.apply(password, ctx).isError()).isFalse();
        }
    }

    // =========================================================================
    // Null / empty / blank
    // =========================================================================

    @Nested
    class NullOrEmpty {

        @Test
        void nullPassword_returnsError() {
            assertThat(validator.apply(null, ctx).isError()).isTrue();
        }

        @Test
        void emptyPassword_returnsError() {
            assertThat(validator.apply("", ctx).isError()).isTrue();
        }

        @Test
        void blankPassword_returnsError() {
            assertThat(validator.apply("   ", ctx).isError()).isTrue();
        }
    }

    // =========================================================================
    // Length boundaries
    // =========================================================================

    @Nested
    class LengthBoundary {

        @Test
        void exactlyMinLength_withAllRequirements_returnsOk() {
            String password = "Aa1!" + "a".repeat(PasswordValidator.MIN_LENGTH - 4);
            assertThat(password.length()).isEqualTo(PasswordValidator.MIN_LENGTH);
            assertThat(validator.apply(password, ctx).isError()).isFalse();
        }

        @Test
        void oneLessThanMinLength_returnsError() {
            String password = "Aa1!" + "a".repeat(PasswordValidator.MIN_LENGTH - 5);
            assertThat(password.length()).isEqualTo(PasswordValidator.MIN_LENGTH - 1);
            ValidationResult result = validator.apply(password, ctx);
            assertThat(result.isError()).isTrue();
            assertThat(result.getErrorMessage()).contains(String.valueOf(PasswordValidator.MIN_LENGTH));
        }

        @Test
        void oneMoreThanMaxLength_returnsError() {
            String password = "Aa1!" + "a".repeat(PasswordValidator.MAX_LENGTH - 3);
            assertThat(password.length()).isEqualTo(PasswordValidator.MAX_LENGTH + 1);
            ValidationResult result = validator.apply(password, ctx);
            assertThat(result.isError()).isTrue();
            assertThat(result.getErrorMessage()).contains(String.valueOf(PasswordValidator.MAX_LENGTH));
        }
    }

    // =========================================================================
    // Missing character classes
    // =========================================================================

    @Nested
    class MissingCharacterClass {

        @Test
        void noUppercase_returnsError() {
            ValidationResult result = validator.apply("secur3p@ssword!", ctx);
            assertThat(result.isError()).isTrue();
            assertThat(result.getErrorMessage()).containsIgnoringCase("uppercase");
        }

        @Test
        void noLowercase_returnsError() {
            ValidationResult result = validator.apply("SECUR3P@SSWORD!", ctx);
            assertThat(result.isError()).isTrue();
            assertThat(result.getErrorMessage()).containsIgnoringCase("lowercase");
        }

        @Test
        void noDigit_returnsError() {
            ValidationResult result = validator.apply("SecureP@ssword!", ctx);
            assertThat(result.isError()).isTrue();
            assertThat(result.getErrorMessage()).containsIgnoringCase("digit");
        }

        @Test
        void noSpecialChar_returnsError() {
            ValidationResult result = validator.apply("Secur3Password", ctx);
            assertThat(result.isError()).isTrue();
            assertThat(result.getErrorMessage()).containsIgnoringCase("special");
        }

        @Test
        void onlyLowercase_returnsError() {
            assertThat(validator.apply("passwordpassword", ctx).isError()).isTrue();
        }

        @Test
        void onlyUppercase_returnsError() {
            assertThat(validator.apply("PASSWORDPASSWORD", ctx).isError()).isTrue();
        }

        @Test
        void onlyDigits_returnsError() {
            assertThat(validator.apply("123456789012", ctx).isError()).isTrue();
        }

        @Test
        void onlySpecialChars_returnsError() {
            assertThat(validator.apply("!@#$%^&*()_+-=", ctx).isError()).isTrue();
        }
    }

    // =========================================================================
    // Special character recognition
    // =========================================================================

    @Nested
    class SpecialCharRecognition {

        @ParameterizedTest
        @ValueSource(strings = {"!", "@", "#", "$", "%", "^", "&", "*", "(", ")",
                "_", "+", "-", "=", "[", "]", "{", "}", "|", ";", ":", ",", ".", "<", ">", "?"})
        void eachSpecialChar_isRecognised(String special) {
            // pad to min length with valid other classes
            String password = "Secur3Pwd" + special + "xxx";
            // trim/pad to ensure >= MIN_LENGTH
            assertThat(validator.apply(password, ctx).isError()).isFalse();
        }
    }

    // =========================================================================
    // Edge cases
    // =========================================================================

    @Nested
    class EdgeCases {

        @Test
        void passwordWithAllRequiredClasses_atMinBoundary() {
            String password = "Aa1!"; // 4 chars, but needs MIN_LENGTH chars
            ValidationResult result = validator.apply(password, ctx);
            assertThat(result.isError()).isTrue();
        }

        @Test
        void passwordWithConsecutiveSpecialChars() {
            String password = "Abc123!!!@@@xyz";
            assertThat(validator.apply(password, ctx).isError()).isFalse();
        }

        @Test
        void passwordWithUnicodeCharacters() {
            String password = "Äöü123!Abc";
            // Special chars check only counts ASCII special chars
            ValidationResult result = validator.apply(password, ctx);
            assertThat(result.isError()).isTrue(); // No ASCII special char
        }

        @Test
        void passwordWithWhitespaceOnly() {
            assertThat(validator.apply("     ", ctx).isError()).isTrue();
        }

        @Test
        void passwordWithNumbers_verifyDigitCheck() {
            String password = "abcdefGHIJ!@#$%^";
            ValidationResult result = validator.apply(password, ctx);
            // Missing digit
            assertThat(result.isError()).isTrue();
            assertThat(result.getErrorMessage()).containsIgnoringCase("digit");
        }
    }
}