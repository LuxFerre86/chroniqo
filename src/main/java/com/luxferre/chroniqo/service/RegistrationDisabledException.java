package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.service.user.UserService;

/**
 * Thrown by {@link UserService#register}
 * when public registration has been disabled via the
 * {@code app.registration.enabled} configuration property.
 *
 * @author Luxferre86
 * @since 11.03.2026
 */
public class RegistrationDisabledException extends RuntimeException {

    public RegistrationDisabledException(String message) {
        super(message);
    }
}