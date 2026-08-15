package com.luxferre.chroniqo.util;

/**
 * Represents the result of a field validation. Replaces the Vaadin
 * {@code ValidationResult} to remove the Vaadin dependency.
 *
 * @author Luxferre86
 */
public final class ValidationResult {

    private final String errorMessage;

    private ValidationResult(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /** Creates a successful (non-error) validation result. */
    public static ValidationResult ok() {
        return new ValidationResult(null);
    }

    /** Creates a failed validation result with the given message. */
    public static ValidationResult error(String message) {
        return new ValidationResult(message);
    }

    /** Returns {@code true} when this result represents a validation failure. */
    public boolean isError() {
        return errorMessage != null;
    }

    /**
     * Returns the error message, or {@code null} when the result is not an
     * error.
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}
