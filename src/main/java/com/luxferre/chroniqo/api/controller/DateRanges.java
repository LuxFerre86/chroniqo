package com.luxferre.chroniqo.api.controller;

import com.luxferre.chroniqo.i18n.Messages;
import com.luxferre.chroniqo.service.TimeEntryValidationException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Validation helper for date-range query parameters.
 *
 * @author Luxferre86
 * @since 14.11.2026
 */
final class DateRanges {

    /**
     * Upper bound for a requested range, large enough for a full leap year.
     */
    static final int MAX_DAYS = 366;

    private DateRanges() {
    }

    /**
     * Ensures {@code from <= to} and that the range does not exceed {@link #MAX_DAYS} days.
     *
     * @param from first day (inclusive)
     * @param to   last day (inclusive)
     * @throws TimeEntryValidationException if the range is inverted or too large
     */
    static void validate(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw TimeEntryValidationException.invalidRange(Messages.get("error.range.endBeforeStart"), from, to);
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw TimeEntryValidationException.valueTooLarge(Messages.get("field.dateRange"), Messages.get("unit.days", ChronoUnit.DAYS.between(from, to) + 1), Messages.get("unit.days", MAX_DAYS));
        }
    }
}
