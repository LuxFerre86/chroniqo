package com.luxferre.chroniqo.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * Reminder preferences of the current user; also used as the response body.
 *
 * @param startTime wall-clock time ("HH:mm") from which the start reminder may fire
 */
public record ReminderSettingsDto(boolean startEnabled,
                                  @NotNull LocalTime startTime,
                                  boolean endEnabled,
                                  @Min(1) @Max(24) int endAfterHours) {
}
