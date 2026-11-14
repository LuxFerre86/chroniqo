package com.luxferre.chroniqo.api.dto;

import com.luxferre.chroniqo.util.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code POST /api/auth/register}.
 */
public record RegisterRequest(@NotBlank @Email @Size(max = 255) String email,
                              @StrongPassword String password,
                              @NotBlank @Size(max = 255) String firstName,
                              @NotBlank @Size(max = 255) String lastName,
                              @Min(0) @Max(80) int weeklyTargetHours,
                              @Size(max = 10) String countryCode,
                              @Size(max = 20) String subdivisionCode) {
}
