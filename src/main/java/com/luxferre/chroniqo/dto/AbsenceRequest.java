package com.luxferre.chroniqo.dto;

import com.luxferre.chroniqo.model.AbsenceType;
import com.luxferre.chroniqo.service.AbsenceService;

import java.time.LocalDate;

/**
 * Immutable request object carrying the parameters needed to record an absence
 * (vacation, sick leave, or public holiday) for a date range.
 *
 * <p>The range is inclusive on both ends. Weekend days within the range are
 * silently skipped by {@link AbsenceService}.
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
public record AbsenceRequest(LocalDate startDate,
                             LocalDate endDate,
                             AbsenceType absenceType) {

}