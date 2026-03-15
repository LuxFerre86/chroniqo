package com.luxferre.chroniqo.util;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.validator.AbstractValidator;

/**
 * Validates that a password meets the application's security policy:
 * <ul>
 *   <li>At least {@value #MIN_LENGTH} characters</li>
 *   <li>No more than {@value #MAX_LENGTH} characters (DoS protection during hashing)</li>
 *   <li>At least one uppercase letter (A–Z)</li>
 *   <li>At least one lowercase letter (a–z)</li>
 *   <li>At least one digit (0–9)</li>
 *   <li>At least one special character from {@value #SPECIAL_CHARS}</li>
 * </ul>
 *
 * <p>Use {@link #HELPER_TEXT} as the helper text on any password field to keep
 * the UI hint consistent with the actual enforcement.
 */
public class PasswordValidator extends AbstractValidator<String> {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 128;
    public static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    public static final String HELPER_TEXT =
            "At least " + MIN_LENGTH + " characters, including uppercase, lowercase, a digit and a special character ("
                    + SPECIAL_CHARS + ")";

    public PasswordValidator() {
        super("Invalid password");
    }

    @Override
    public ValidationResult apply(String value, ValueContext context) {
        if (value == null || value.isBlank()) {
            return ValidationResult.error("Password is required");
        }
        if (value.length() < MIN_LENGTH) {
            return ValidationResult.error(
                    "Password must be at least " + MIN_LENGTH + " characters");
        }
        if (value.length() > MAX_LENGTH) {
            return ValidationResult.error(
                    "Password must not exceed " + MAX_LENGTH + " characters");
        }
        if (value.chars().noneMatch(Character::isUpperCase)) {
            return ValidationResult.error(
                    "Password must contain at least one uppercase letter");
        }
        if (value.chars().noneMatch(Character::isLowerCase)) {
            return ValidationResult.error(
                    "Password must contain at least one lowercase letter");
        }
        if (value.chars().noneMatch(Character::isDigit)) {
            return ValidationResult.error(
                    "Password must contain at least one digit");
        }
        if (!containsSpecialChar(value)) {
            return ValidationResult.error(
                    "Password must contain at least one special character (" + SPECIAL_CHARS + ")");
        }
        return ValidationResult.ok();
    }

    private boolean containsSpecialChar(String value) {
        for (char c : value.toCharArray()) {
            if (SPECIAL_CHARS.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }
}