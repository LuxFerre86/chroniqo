package com.luxferre.chroniqo.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Payload for {@code PUT /api/vacation/entitlement}.
 */
public record VacationEntitlementRequest(@Min(2000) @Max(2100) int year,
                                         @Min(0) @Max(366) int days) {
}
