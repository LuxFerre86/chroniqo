package com.luxferre.chroniqo.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload for {@code POST /api/auth/password-reset/request}.
 */
public record PasswordResetRequestRequest(@NotBlank @Email String email) {
}
