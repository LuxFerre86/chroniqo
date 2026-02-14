package com.luxferre.chroniqo.dto;

import com.luxferre.chroniqo.model.AbsenceType;

import java.time.LocalDate;

public record AbsenceRequest(LocalDate startDate,
                             LocalDate endDate,
                             AbsenceType absenceType) {

}
