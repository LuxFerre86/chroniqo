package com.luxferre.chroniqo.api.dto;

import com.luxferre.chroniqo.util.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload for {@code POST /api/auth/register}.
 */
public record RegisterRequest(@NotBlank @Email @Size(max = 255) String email,
                              @StrongPassword String password,
                              @NotBlank @Size(max = 255) String firstName,
                              @NotBlank @Size(max = 255) String lastName,
                              @NotNull @DecimalMin("0") @DecimalMax("80") @Digits(integer = 2, fraction = 1) BigDecimal weeklyTargetHours,
                              @Size(max = 10) String countryCode,
                              @Size(max = 20) String subdivisionCode,
                              @Size(max = 5) String language) {
}
