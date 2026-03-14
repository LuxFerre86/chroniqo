package com.luxferre.chroniqo.service;

import lombok.Getter;

/**
 * Exception thrown when time entry validation fails.
 * Contains structured information about the validation error
 * to enable user-friendly error messages in the UI.
 *
 * @author LuxFerre86
 * @since 12.03.2026
 */
@Getter
public class TimeEntryValidationException extends RuntimeException {

    /**
     * Type of validation error for programmatic handling
     */
    private final ValidationErrorType errorType;

    /**
     * The field that failed validation (e.g., "breakMinutes", "endTime")
     */
    private final String field;

    /**
     * The value that was rejected (for debugging)
     */
    private final Object rejectedValue;

    public TimeEntryValidationException(
            ValidationErrorType errorType,
            String field,
            Object rejectedValue,
            String message) {
        super(message);
        this.errorType = errorType;
        this.field = field;
        this.rejectedValue = rejectedValue;
    }

    public enum ValidationErrorType {
        NULL_VALUE,
        FUTURE_DATE,
        NEGATIVE_VALUE,
        VALUE_TOO_LARGE,
        INVALID_RANGE,
        INCONSISTENT_DATA
    }

    // Convenience factory methods for common validation errors

    public static TimeEntryValidationException nullValue(String field, String message) {
        return new TimeEntryValidationException(
                ValidationErrorType.NULL_VALUE,
                field,
                null,
                message
        );
    }

    public static TimeEntryValidationException futureDate(Object date) {
        return new TimeEntryValidationException(
                ValidationErrorType.FUTURE_DATE,
                "date",
                date,
                "Date cannot be in the future"
        );
    }

    public static TimeEntryValidationException negativeValue(String field, Object value) {
        return new TimeEntryValidationException(
                ValidationErrorType.NEGATIVE_VALUE,
                field,
                value,
                String.format("%s cannot be negative (got: %s)", field, value)
        );
    }

    public static TimeEntryValidationException valueTooLarge(String field, Object value, Object max) {
        return new TimeEntryValidationException(
                ValidationErrorType.VALUE_TOO_LARGE,
                field,
                value,
                String.format("%s cannot exceed %s (got: %s)", field, max, value)
        );
    }

    public static TimeEntryValidationException invalidRange(String message, Object start, Object end) {
        return new TimeEntryValidationException(
                ValidationErrorType.INVALID_RANGE,
                "timeRange",
                String.format("%s -> %s", start, end),
                message
        );
    }

    public static TimeEntryValidationException inconsistentData(String message) {
        return new TimeEntryValidationException(
                ValidationErrorType.INCONSISTENT_DATA,
                "timeEntry",
                null,
                message
        );
    }
}