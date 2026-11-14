package com.luxferre.chroniqo.service.user;

/**
 * Thrown when a registration is attempted with an email address that already
 * belongs to an account.
 *
 * <p>Callers exposed to anonymous users must not reveal this condition to the
 * client, otherwise registered email addresses could be enumerated.
 *
 * @author Luxferre86
 * @since 14.11.2026
 */
public class EmailAlreadyRegisteredException extends IllegalArgumentException {

    public EmailAlreadyRegisteredException(String message) {
        super(message);
    }
}
