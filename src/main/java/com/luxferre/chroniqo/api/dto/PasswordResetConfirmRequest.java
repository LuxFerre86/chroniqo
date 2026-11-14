package com.luxferre.chroniqo.api.dto;

import com.luxferre.chroniqo.util.StrongPassword;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload for {@code POST /api/auth/password-reset/confirm}.
 */
public record PasswordResetConfirmRequest(@NotBlank String token,
                                          @StrongPassword String newPassword) {
}
