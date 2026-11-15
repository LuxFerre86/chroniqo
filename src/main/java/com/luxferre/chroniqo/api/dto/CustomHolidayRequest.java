package com.luxferre.chroniqo.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload for {@code POST /api/custom-holidays}.
 */
public record CustomHolidayRequest(@NotBlank @Size(max = 100) String name,
                                   @NotNull LocalDate startDate,
                                   @NotNull LocalDate endDate,
                                   boolean recurring) {
}
