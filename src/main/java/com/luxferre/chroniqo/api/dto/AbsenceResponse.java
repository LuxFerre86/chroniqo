package com.luxferre.chroniqo.api.dto;

import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.AbsenceType;

import java.time.LocalDate;

/**
 * A single recorded absence day.
 *
 * @param date the absence date
 * @param type the absence type
 */
public record AbsenceResponse(LocalDate date, AbsenceType type) {

    /**
     * Maps an {@link Absence} entity to its API representation.
     *
     * @param absence the entity
     * @return the response
     */
    public static AbsenceResponse of(Absence absence) {
        return new AbsenceResponse(absence.getDate(), absence.getType());
    }
}
