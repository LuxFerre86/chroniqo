package com.luxferre.chroniqo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Data transfer object representing a single time entry.
 *
 * <p>Carries the four mutable fields that the user can edit: date, start time,
 * end time, and break duration in minutes. {@code endTime} may be {@code null}
 * for an entry that has been started but not yet completed.
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class TimeEntryDTO {
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer breakMinutes;
}