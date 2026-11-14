package com.luxferre.chroniqo.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for {@code DELETE /api/me}.
 */
public record DeleteAccountRequest(@NotBlank String password) {
}
