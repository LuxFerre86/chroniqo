package com.luxferre.chroniqo.api.dto;

import com.luxferre.chroniqo.service.user.EmailVerificationResult;

/**
 * Result of an email verification attempt.
 *
 * @param status {@code VERIFIED_LOGGED_IN}, {@code VERIFIED_LOGIN_REQUIRED} or {@code INVALID}
 */
public record VerifyEmailResponse(EmailVerificationResult status) {
}
