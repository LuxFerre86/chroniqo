package com.luxferre.chroniqo.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.util.Set;

/**
 * Payload for {@code PUT /api/me}.
 */
public record UpdateProfileRequest(@NotBlank @Size(max = 255) String firstName,
                                   @NotBlank @Size(max = 255) String lastName,
                                   @Min(0) @Max(80) int weeklyTargetHours,
                                   @NotEmpty Set<DayOfWeek> workingDays,
                                   @Size(max = 10) String countryCode,
                                   @Size(max = 20) String subdivisionCode) {
}
