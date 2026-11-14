package com.luxferre.chroniqo.util;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Password strength policy shared by registration, password reset and
 * password change.
 *
 * <p>The policy requires a minimum and maximum length as well as at least one
 * uppercase letter, one lowercase letter, one digit and one special character
 * from {@link #SPECIAL_CHARS}. It is enforced server-side through
 * {@link StrongPassword} and mirrored in the frontend for instant feedback.
 *
 * @author Luxferre86
 * @since 12.03.2026
 */
public final class PasswordValidator {

    public static final int MIN_LENGTH = 12;
    /**
     * Maximum length. BCrypt only processes the first 72 bytes and rejects longer
     * input, so the policy must never allow more than that.
     */
    public static final int MAX_LENGTH = 72;
    public static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    public static final String HELPER_TEXT =
            "At least " + MIN_LENGTH + " characters, including uppercase, lowercase, a digit and a special character ("
                    + SPECIAL_CHARS + ")";

    private PasswordValidator() {
    }

    /**
     * Validates the given password against the password policy.
     *
     * @param value the candidate password
     * @return the first violated rule as a human-readable message, or empty if the password is valid
     */
    public static Optional<String> validate(String value) {
        if (value == null || value.isBlank()) {
            return Optional.of("Password is required");
        }
        if (value.length() < MIN_LENGTH) {
            return Optional.of("Password must be at least " + MIN_LENGTH + " characters");
        }
        if (value.length() > MAX_LENGTH) {
            return Optional.of("Password must not exceed " + MAX_LENGTH + " characters");
        }
        if (value.getBytes(StandardCharsets.UTF_8).length > MAX_LENGTH) {
            return Optional.of("Password must not exceed " + MAX_LENGTH + " bytes (non-ASCII characters count as several)");
        }
        if (value.chars().noneMatch(Character::isUpperCase)) {
            return Optional.of("Password must contain at least one uppercase letter");
        }
        if (value.chars().noneMatch(Character::isLowerCase)) {
            return Optional.of("Password must contain at least one lowercase letter");
        }
        if (value.chars().noneMatch(Character::isDigit)) {
            return Optional.of("Password must contain at least one digit");
        }
        if (value.chars().noneMatch(c -> SPECIAL_CHARS.indexOf(c) >= 0)) {
            return Optional.of("Password must contain at least one special character");
        }
        return Optional.empty();
    }

    /**
     * Checks the given password against the password policy.
     *
     * @param value the candidate password
     * @return {@code true} if the password satisfies the policy
     */
    public static boolean isValid(String value) {
        return validate(value).isEmpty();
    }
}
