package com.luxferre.chroniqo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.focus_shift.jollyday.core.HolidayCalendar;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry for ISO-3166-2 countries and their subdivisions.
 *
 * <p>This component loads a JSON resource containing country and subdivision
 * information and exposes helper methods for looking up display names,
 * subdivisions, and holiday calendar support (via Jollyday).</p>
 *
 * <p>The registry is initialized on bean construction using {@link PostConstruct}.</p>
 *
 * @author Luxferre86
 * @since 22.03.2026
 */
@Slf4j
@Component
public class CountrySubdivisionRegistry {

    private static final String RESOURCE = "iso-3166-2.json";

    @Getter
    private Map<String, String> allCountries = Collections.emptyMap();

    private Map<String, Map<String, String>> subdivisionsByCountry = Collections.emptyMap();
    private Set<String> jollyDayCodes = Collections.emptySet();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Initialize the registry after construction.
     *
     * <p>This method populates the set of Jollyday-supported country codes and
     * loads country/subdivision data from the JSON resource. It also logs a
     * summary of the loaded data.</p>
     */
    @PostConstruct
    void init() {
        jollyDayCodes = initJollyDayCodes();
        loadFromJson();

        log.info("CountrySubdivisionRegistry: {} countries, {} with subdivisions, {} with jollyday support",
                allCountries.size(),
                subdivisionsByCountry.size(),
                allCountries.keySet().stream().filter(this::hasHolidaySupport).count());
    }

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    /**
     * Build an immutable set of upper-cased two-letter country codes supported by Jollyday.
     *
     * @return unmodifiable set of country codes (two-letter ISO-like codes)
     */
    private Set<String> initJollyDayCodes() {
        return Arrays.stream(HolidayCalendar.values())
                .map(c -> c.getId().toUpperCase())
                .filter(id -> id.length() == 2)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Load country and subdivision data from the JSON resource.
     *
     * <p>The method populates {@link #allCountries} and {@link #subdivisionsByCountry}.
     * On failure both maps are set to empty maps and an error is logged.</p>
     */
    private void loadFromJson() {
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {

            Map<String, CountryRaw> raw =
                    mapper.readValue(in, new TypeReference<>() {
                    });

            Map<String, String> tmp = raw.entrySet().stream()
                    .filter(e -> hasText(e.getValue().name()))
                    .collect(Collectors.toMap(
                            e -> normalize(e.getKey()),
                            e -> e.getValue().name()
                    ));

            allCountries = tmp.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue,
                                    (a, b) -> a,
                                    LinkedHashMap::new
                            ),
                            Collections::unmodifiableMap
                    ));

            subdivisionsByCountry = raw.entrySet().stream()
                    .map(this::mapSubdivisions)
                    .filter(Objects::nonNull)
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue,
                                    (a, b) -> a,
                                    LinkedHashMap::new
                            ),
                            Collections::unmodifiableMap
                    ));

        } catch (Exception e) {
            log.error("Failed to load {} — country/subdivision selection unavailable", RESOURCE, e);
            allCountries = Map.of();
            subdivisionsByCountry = Map.of();
        }
    }

    /**
     * Convert a raw entry to a map entry of normalized country code -> sorted/immutable subdivisions map.
     *
     * <p>Returns {@code null} when the raw entry does not contain subdivisions.</p>
     *
     * @param entry raw country entry
     * @return normalized entry or {@code null} if no subdivisions exist
     */
    private Map.Entry<String, Map<String, String>> mapSubdivisions(
            Map.Entry<String, CountryRaw> entry) {

        Map<String, String> divisions = entry.getValue().divisions();
        if (divisions == null || divisions.isEmpty()) {
            return null;
        }

        Map<String, String> sorted = divisions.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toMap(
                        e -> normalize(e.getKey()),
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return Map.entry(
                normalize(entry.getKey()),
                Collections.unmodifiableMap(sorted)
        );
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Get the subdivisions map for a given country code.
     *
     * @param countryCode ISO country code (case-insensitive)
     * @return immutable map of subdivision code -> display name; empty map if none
     */
    public Map<String, String> getSubdivisions(String countryCode) {
        return subdivisionsByCountry.getOrDefault(normalize(countryCode), Map.of());
    }

    /**
     * Determine whether a given country has holiday calendar support via Jollyday.
     *
     * @param countryCode ISO country code (case-insensitive)
     * @return {@code true} if Jollyday has a matching calendar; otherwise {@code false}
     */
    public boolean hasHolidaySupport(String countryCode) {
        return jollyDayCodes.contains(normalize(countryCode));
    }

    /**
     * Resolve the Jollyday {@link HolidayCalendar} for a country code.
     *
     * @param countryCode ISO country code (case-insensitive)
     * @return optional HolidayCalendar if found
     */
    public Optional<HolidayCalendar> getCalendar(String countryCode) {
        String code = normalize(countryCode);

        return Arrays.stream(HolidayCalendar.values())
                .filter(c -> c.getId().equalsIgnoreCase(code))
                .findFirst();
    }

    /**
     * Get the human-readable display name for a country code.
     *
     * @param countryCode ISO country code (case-insensitive)
     * @return display name if available; otherwise returns the normalized code or empty string for blank input
     */
    public String getCountryDisplayName(String countryCode) {
        String code = normalize(countryCode);
        return code.isEmpty() ? "" : allCountries.getOrDefault(code, code);
    }

    /**
     * Get the human-readable display name for a subdivision.
     *
     * @param countryCode     ISO country code (case-insensitive)
     * @param subdivisionCode ISO-3166-2 subdivision code (case-insensitive)
     * @return display name if available; otherwise returns the normalized subdivision code or empty string for blank input
     */
    public String getSubdivisionDisplayName(String countryCode, String subdivisionCode) {
        String sub = normalize(subdivisionCode);
        return sub.isEmpty() ? "" : getSubdivisions(countryCode).getOrDefault(sub, sub);
    }

    /**
     * Convert an ISO-3166-2 code (like "GB-ENG") to the subdivision portion used by Jollyday (e.g. "ENG").
     *
     * @param iso31662Code full ISO-3166-2 code
     * @return subdivision portion or the input if no dash present
     */
    public String toJollyDaySubdivision(String iso31662Code) {
        String code = normalize(iso31662Code);
        int dash = code.indexOf('-');
        return dash >= 0 ? code.substring(dash + 1) : code;
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Normalize input to an upper-cased non-null string.
     *
     * @param value input value
     * @return upper-cased string or empty string when input is null
     */
    private String normalize(String value) {
        return value == null ? "" : value.toUpperCase();
    }

    /**
     * Check whether a string contains non-blank text.
     *
     * @param value input value
     * @return {@code true} when value is not null and contains non-whitespace characters
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // -------------------------------------------------------------------------
    // DTO (Typed JSON Mapping!)
    // -------------------------------------------------------------------------

    /**
     * DTO record for mapping the JSON structure of a country entry.
     *
     * <p>Fields:
     * <ul>
     *     <li>{@code name} - display name of the country</li>
     *     <li>{@code divisions} - map of subdivision code -> display name</li>
     * </ul>
     */
    private record CountryRaw(
            String name,
            Map<String, String> divisions
    ) {
    }
}