package com.luxferre.chroniqo.api.dto;

/**
 * Public application configuration needed by the frontend before login.
 *
 * @param appName             display name of the application
 * @param version             application version
 * @param registrationEnabled whether self-registration is currently allowed
 * @param passwordPolicy      the password rules enforced by the server
 * @param pushPublicKey       VAPID public key for Web Push, {@code null} when push reminders are not configured
 */
public record AppConfigResponse(String appName,
                                String version,
                                boolean registrationEnabled,
                                PasswordPolicy passwordPolicy,
                                String pushPublicKey) {

    /**
     * Password rules mirrored by the frontend for instant feedback.
     *
     * @param minLength    minimum password length
     * @param maxLength    maximum password length
     * @param specialChars accepted special characters
     */
    public record PasswordPolicy(int minLength, int maxLength, String specialChars) {
    }
}
