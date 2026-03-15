package com.luxferre.chroniqo.service.user;

/**
 * Thrown when a requested user cannot be found, for example when the security
 * context contains a principal whose email no longer exists in the database.
 *
 * @author Luxferre86
 * @since 22.02.2026
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}