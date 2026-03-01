package com.luxferre.chroniqo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class TimeEntryDTO {
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer breakMinutes;
}
