package com.luxferre.chroniqo.dto;

import com.luxferre.chroniqo.model.AbsenceType;

import java.time.LocalDate;

public record DaySummaryDTO(
        LocalDate date,
        boolean isWorkday,
        int workedMinutes,
        int targetMinutes,
        int balanceMinutes,
        AbsenceType absenceType
) {
}