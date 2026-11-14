package com.luxferre.chroniqo.api.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Payload for creating or updating a time entry. Semantic validation (future
 * dates, overlaps, break length, ...) is performed by the service layer.
 */
public record TimeEntryRequest(LocalDate date,
                               LocalTime startTime,
                               LocalTime endTime,
                               Integer breakMinutes,
                               @Size(max = 500) String notes) {
}
