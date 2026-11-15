package com.luxferre.chroniqo.dto;

import com.luxferre.chroniqo.model.AbsenceType;

/**
 * Represents the reason for a user's absence on a working day.
 *
 * <p>This DTO enum is used to transfer absence classification data between layers.</p>
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
public enum AbsenceTypeDTO {
    /** Planned leave approved for the user. */
    VACATION,

    /** Leave caused by illness or medical reasons. */
    SICK,

    /** Day off to reduce overtime; the daily target stays. */
    FLEX_DAY,

    /** Parental leave. */
    PARENTAL_LEAVE,

    /** Business trip; the daily target counts as worked. */
    BUSINESS_TRIP,

    /** Special leave, e.g. wedding or moving. */
    SPECIAL_LEAVE,

    /** Public holiday on which the user is not expected to work. */
    HOLIDAY;

    /**
     * Converts a domain {@link AbsenceType} value to its DTO representation.
     *
     * @param absenceType the domain absence type to convert
     * @return the corresponding {@link AbsenceTypeDTO} value
     */
    public static AbsenceTypeDTO of(AbsenceType absenceType) {
        return switch (absenceType) {
            case VACATION -> VACATION;
            case SICK -> SICK;
            case FLEX_DAY -> FLEX_DAY;
            case PARENTAL_LEAVE -> PARENTAL_LEAVE;
            case BUSINESS_TRIP -> BUSINESS_TRIP;
            case SPECIAL_LEAVE -> SPECIAL_LEAVE;
        };
    }
}