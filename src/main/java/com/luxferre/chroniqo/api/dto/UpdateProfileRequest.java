package com.luxferre.chroniqo.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Set;

/**
 * Payload for {@code PUT /api/me}.
 */
public record UpdateProfileRequest(@NotBlank @Size(max = 255) String firstName,
                                   @NotBlank @Size(max = 255) String lastName,
                                   @NotNull @DecimalMin("0") @DecimalMax("80") @Digits(integer = 2, fraction = 1) BigDecimal weeklyTargetHours,
                                   @NotEmpty Set<DayOfWeek> workingDays,
                                   @Size(max = 10) String countryCode,
                                   @Size(max = 20) String subdivisionCode) {
}
