package com.luxferre.chroniqo.api.dto;

import com.luxferre.chroniqo.model.AbsenceType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Payload for {@code PUT /api/absences}: records the given absence type for
 * every working day (Mon-Fri) within the inclusive date range.
 */
public record AbsenceRangeRequest(@NotNull LocalDate startDate,
                                  @NotNull LocalDate endDate,
                                  @NotNull AbsenceType absenceType) {
}
