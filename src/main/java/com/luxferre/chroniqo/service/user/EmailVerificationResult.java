package com.luxferre.chroniqo.service.user;

/**
 * Result of an email verification attempt.
 *
 * @author Luxferre86
 * @since 15.03.2026
 */
public enum EmailVerificationResult {

    /**
     * Token was unknown or expired – the user account was NOT enabled.
     */
    INVALID,

    /**
     * Token was valid, account enabled, and the HTTP session was successfully established.
     * The user is now logged in and can be forwarded directly to the dashboard.
     */
    VERIFIED_LOGGED_IN,

    /**
     * Token was valid and the account was enabled, but the session could not be persisted
     * (no servlet request context was available at call time).
     * The user must navigate to the login page manually.
     */
    VERIFIED_LOGIN_REQUIRED
}