package com.luxferre.chroniqo.dto;

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
                                      int weeklyTargetHours,
                                      String countryCode,
                                      String subdivisionCode) {

}

