package com.luxferre.chroniqo.api.dto;

import com.luxferre.chroniqo.util.StrongPassword;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload for {@code PUT /api/me/password}.
 */
public record ChangePasswordRequest(@NotBlank String currentPassword,
                                    @StrongPassword String newPassword) {
}
