package com.luxferre.chroniqo.api.controller;

import com.luxferre.chroniqo.service.CountrySubdivisionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Normalizes and validates the optional country / subdivision selection that
 * is part of registration and profile updates.
 *
 * @author Luxferre86
 * @since 14.11.2026
 */
@Component
@RequiredArgsConstructor
class ProfileValidator {

    private final CountrySubdivisionRegistry countryRegistry;

    /**
     * @param countryCode raw country code, may be blank
     * @return the upper-cased country code, or {@code null} if none was selected
     * @throws IllegalArgumentException if the country is unknown
     */
    String normalizeCountry(String countryCode) {
        if (!StringUtils.hasText(countryCode)) {
            return null;
        }
        String normalized = countryCode.trim().toUpperCase();
        if (!countryRegistry.getAllCountries().containsKey(normalized)) {
            throw new IllegalArgumentException("Unknown country: " + normalized);
        }
        return normalized;
    }

    /**
     * @param countryCode     the already normalized country code, may be {@code null}
     * @param subdivisionCode raw subdivision code, may be blank
     * @return the registry's subdivision code, or {@code null} if none applies
     * @throws IllegalArgumentException if the subdivision does not belong to the country
     */
    String normalizeSubdivision(String countryCode, String subdivisionCode) {
        if (countryCode == null || !StringUtils.hasText(subdivisionCode)) {
            return null;
        }
        String requested = subdivisionCode.trim();
        return countryRegistry.getSubdivisions(countryCode).keySet().stream()
                .filter(key -> key.equalsIgnoreCase(requested))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown region: " + requested));
    }
}
