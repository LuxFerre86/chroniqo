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
        };
    }
}