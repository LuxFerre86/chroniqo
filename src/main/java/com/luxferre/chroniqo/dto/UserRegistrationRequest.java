package com.luxferre.chroniqo.dto;

import java.math.BigDecimal;

/**
 * Immutable request object carrying the data needed to register a new user.
 *
 * <p>The request contains profile, authentication, and optional public-holiday
 * configuration fields. Validation is performed by the caller and the domain
 * model/service layer.
 *
 * @author Luxferre86
 * @since 06.04.2026
 */
public record UserRegistrationRequest(String email,
                                      String password,
                                      String firstName,
                                      String lastName,
                                      BigDecimal weeklyTargetHours,
                                      String countryCode,
                                      String subdivisionCode,
                                      String language) {

    public UserRegistrationRequest(String email, String password, String firstName, String lastName,
                                   BigDecimal weeklyTargetHours, String countryCode, String subdivisionCode) {
        this(email, password, firstName, lastName, weeklyTargetHours, countryCode, subdivisionCode, null);
    }

}

