package com.luxferre.chroniqo.api.dto;

import com.luxferre.chroniqo.dto.AbsenceTypeDTO;
import com.luxferre.chroniqo.dto.DaySummaryDTO;

import java.time.LocalDate;

/**
 * Aggregated figures for a single day.
 *
 * @param date           the day
 * @param workday        whether the day counts as a working day (configured weekday, no absence, no public holiday)
 * @param workedMinutes  minutes worked
 * @param targetMinutes  target minutes
 * @param balanceMinutes worked minus target minutes
 * @param absenceType    {@code VACATION}, {@code SICK}, {@code HOLIDAY} or {@code null}
 * @param absenceLabel   label of the absence or public holiday, or {@code null}
 */
public record DaySummaryResponse(LocalDate date,
                                 boolean workday,
                                 int workedMinutes,
                                 int targetMinutes,
                                 int balanceMinutes,
                                 AbsenceTypeDTO absenceType,
                                 String absenceLabel) {

    /**
     * Maps a {@link DaySummaryDTO} to its API representation.
     *
     * @param dto the summary
     * @return the response, or {@code null} if the input is {@code null}
     */
    public static DaySummaryResponse of(DaySummaryDTO dto) {
        if (dto == null) {
            return null;
        }
        return new DaySummaryResponse(dto.date(), dto.isWorkday(), dto.workedMinutes(), dto.targetMinutes(),
                dto.balanceMinutes(), dto.absenceType(), dto.absenceLabel());
    }
}
