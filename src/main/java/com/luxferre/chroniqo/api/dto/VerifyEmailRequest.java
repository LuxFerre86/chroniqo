package com.luxferre.chroniqo.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for {@code POST /api/auth/verify-email}.
 */
public record VerifyEmailRequest(@NotBlank String token) {
}
