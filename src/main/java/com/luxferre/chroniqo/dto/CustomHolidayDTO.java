package com.luxferre.chroniqo.dto;

import java.time.LocalDate;

/**
 * A custom day off of the current user.
 *
 * @param id        identifier
 * @param name      display name, e.g. "Bridge day"
 * @param startDate first day (inclusive)
 * @param endDate   last day (inclusive)
 * @param recurring whether the entry repeats every year
 */
public record CustomHolidayDTO(String id, String name, LocalDate startDate, LocalDate endDate, boolean recurring) {
}
