package com.luxferre.chroniqo.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordValidatorTest {

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
            assertThat(PasswordValidator.validate(password)).isEmpty();
            assertThat(PasswordValidator.isValid(password)).isTrue();
        }

        @Test
        void passwordAtMaxLength_returnsOk() {
            String password = "Aa1!" + "a".repeat(PasswordValidator.MAX_LENGTH - 4);
            assertThat(password.length()).isEqualTo(PasswordValidator.MAX_LENGTH);
            assertThat(PasswordValidator.validate(password)).isEmpty();
        }
    }

    // =========================================================================
    // Null / empty / blank
    // =========================================================================

    @Nested
    class NullOrEmpty {

        @Test
        void nullPassword_returnsError() {
            assertThat(PasswordValidator.validate(null)).contains("Password is required");
            assertThat(PasswordValidator.isValid(null)).isFalse();
        }

        @Test
        void emptyPassword_returnsError() {
            assertThat(PasswordValidator.validate("")).contains("Password is required");
        }

        @Test
        void blankPassword_returnsError() {
            assertThat(PasswordValidator.validate("   ")).contains("Password is required");
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
            assertThat(PasswordValidator.validate(password)).isEmpty();
        }

        @Test
        void oneLessThanMinLength_returnsError() {
            String password = "Aa1!" + "a".repeat(PasswordValidator.MIN_LENGTH - 5);
            assertThat(password.length()).isEqualTo(PasswordValidator.MIN_LENGTH - 1);
            Optional<String> result = PasswordValidator.validate(password);
            assertThat(result).isPresent();
            assertThat(result.get()).contains(String.valueOf(PasswordValidator.MIN_LENGTH));
        }

        @Test
        void oneMoreThanMaxLength_returnsError() {
            String password = "Aa1!" + "a".repeat(PasswordValidator.MAX_LENGTH - 3);
            assertThat(password.length()).isEqualTo(PasswordValidator.MAX_LENGTH + 1);
            Optional<String> result = PasswordValidator.validate(password);
            assertThat(result).isPresent();
            assertThat(result.get()).contains(String.valueOf(PasswordValidator.MAX_LENGTH));
        }
    }

    // =========================================================================
    // Missing character classes
    // =========================================================================

    @Nested
    class MissingCharacterClass {

        @Test
        void noUppercase_returnsError() {
            Optional<String> result = PasswordValidator.validate("secur3p@ssword!");
            assertThat(result).isPresent();
            assertThat(result.get()).containsIgnoringCase("uppercase");
        }

        @Test
        void noLowercase_returnsError() {
            Optional<String> result = PasswordValidator.validate("SECUR3P@SSWORD!");
            assertThat(result).isPresent();
            assertThat(result.get()).containsIgnoringCase("lowercase");
        }

        @Test
        void noDigit_returnsError() {
            Optional<String> result = PasswordValidator.validate("SecureP@ssword!");
            assertThat(result).isPresent();
            assertThat(result.get()).containsIgnoringCase("digit");
        }

        @Test
        void noSpecialChar_returnsError() {
            Optional<String> result = PasswordValidator.validate("Secur3Password");
            assertThat(result).isPresent();
            assertThat(result.get()).containsIgnoringCase("special");
        }

        @Test
        void onlyLowercase_returnsError() {
            assertThat(PasswordValidator.isValid("passwordpassword")).isFalse();
        }

        @Test
        void onlyUppercase_returnsError() {
            assertThat(PasswordValidator.isValid("PASSWORDPASSWORD")).isFalse();
        }

        @Test
        void onlyDigits_returnsError() {
            assertThat(PasswordValidator.isValid("123456789012")).isFalse();
        }

        @Test
        void onlySpecialChars_returnsError() {
            assertThat(PasswordValidator.isValid("!@#$%^&*()_+-=")).isFalse();
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
            String password = "Secur3Pwd" + special + "xxx";
            assertThat(PasswordValidator.validate(password)).isEmpty();
        }
    }

    // =========================================================================
    // Edge cases
    // =========================================================================

    @Nested
    class EdgeCases {

        @Test
        void passwordWithAllRequiredClasses_belowMinLength_returnsError() {
            assertThat(PasswordValidator.isValid("Aa1!")).isFalse();
        }

        @Test
        void passwordWithConsecutiveSpecialChars() {
            assertThat(PasswordValidator.isValid("Abc123!!!@@@xyz")).isTrue();
        }

        @Test
        void passwordWithUnicodeCharacters() {
            // Special chars check only counts ASCII special chars from SPECIAL_CHARS
            assertThat(PasswordValidator.isValid("Äöü123!Abc")).isFalse();
            assertThat(PasswordValidator.isValid("Äöüabc123ÄÖÜ")).isFalse();
        }

        @Test
        void passwordWithWhitespaceOnly() {
            assertThat(PasswordValidator.isValid("     ")).isFalse();
        }

        @Test
        void passwordWithNumbers_verifyDigitCheck() {
            Optional<String> result = PasswordValidator.validate("abcdefGHIJ!@#$%^");
            assertThat(result).isPresent();
            assertThat(result.get()).containsIgnoringCase("digit");
        }

        @Test
        void helperText_mentionsMinimumLength() {
            assertThat(PasswordValidator.HELPER_TEXT).contains(String.valueOf(PasswordValidator.MIN_LENGTH));
        }
    }
}
