package com.luxferre.chroniqo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Data transfer object representing a single time entry.
 *
 * <p>Carries the mutable fields that the user can edit: date, start time,
 * end time, break duration in minutes, and an optional free-text note.
 * {@code endTime} may be {@code null} for an entry that has been started
 * but not yet completed.
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
@Data
@NoArgsConstructor
public final class TimeEntryDTO {
    private String id;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer breakMinutes;
    private String notes;

    public TimeEntryDTO(LocalDate date,
                        LocalTime startTime,
                        LocalTime endTime,
                        Integer breakMinutes,
                        String notes) {
        this(null, date, startTime, endTime, breakMinutes, notes);
    }

    public TimeEntryDTO(String id,
                        LocalDate date,
                        LocalTime startTime,
                        LocalTime endTime,
                        Integer breakMinutes,
                        String notes) {
        this.id = id;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.breakMinutes = breakMinutes;
        this.notes = notes;
    }
}