package com.luxferre.chroniqo.api.controller;

import com.luxferre.chroniqo.api.dto.AppConfigResponse;
import com.luxferre.chroniqo.api.dto.LookupItem;
import com.luxferre.chroniqo.config.AppProperties;
import com.luxferre.chroniqo.service.CountrySubdivisionRegistry;
import com.luxferre.chroniqo.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public endpoints delivering application configuration and the country /
 * subdivision lookup lists needed by the registration and settings forms.
 *
 * @author Luxferre86
 * @since 14.11.2026
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConfigController {

    private final AppProperties appProperties;
    private final CountrySubdivisionRegistry countryRegistry;

    /**
     * Returns the public application configuration.
     *
     * @return app name, version, registration flag and password policy
     */
    @GetMapping("/config")
    public AppConfigResponse config() {
        return new AppConfigResponse(
                appProperties.getName(),
                appProperties.getVersion(),
                appProperties.getRegistrationProperties().isEnabled(),
                new AppConfigResponse.PasswordPolicy(
                        PasswordValidator.MIN_LENGTH,
                        PasswordValidator.MAX_LENGTH,
                        PasswordValidator.SPECIAL_CHARS));
    }

    /**
     * Returns all countries sorted by name.
     *
     * @return list of country code/name pairs
     */
    @GetMapping("/countries")
    public List<LookupItem> countries() {
        return toItems(countryRegistry.getAllCountries());
    }

    /**
     * Returns the subdivisions (states / regions) of a country.
     *
     * @param code the country code
     * @return list of subdivision code/name pairs, empty if the country has none
     */
    @GetMapping("/countries/{code}/subdivisions")
    public List<LookupItem> subdivisions(@PathVariable String code) {
        return toItems(countryRegistry.getSubdivisions(code));
    }

    private List<LookupItem> toItems(java.util.Map<String, String> source) {
        return source.entrySet().stream()
                .map(e -> new LookupItem(e.getKey(), e.getValue()))
                .toList();
    }
}
