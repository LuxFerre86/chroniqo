package com.luxferre.chroniqo.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload for {@code POST /api/auth/verification-email/resend}.
 */
public record ResendVerificationRequest(@NotBlank @Email String email) {
}
