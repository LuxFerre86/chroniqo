package com.luxferre.chroniqo.dto;

import com.luxferre.chroniqo.model.AbsenceType;

import java.time.LocalDate;

public record DaySummaryDTO(
        LocalDate date,
        Integer workedMinutes,
        Integer balanceMinutes,
        AbsenceType absenceType
) {
}
