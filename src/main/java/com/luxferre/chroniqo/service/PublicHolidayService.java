package com.luxferre.chroniqo.service;

import de.focus_shift.jollyday.core.Holiday;
import de.focus_shift.jollyday.core.HolidayCalendar;
import de.focus_shift.jollyday.core.HolidayManager;
import de.focus_shift.jollyday.core.ManagerParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service that resolves public holidays for any jollyday-supported country and
 * its optional subdivisions.
 *
 * <p>Holidays are never stored in the database — they are computed on demand
 * and held in an in-memory cache keyed by (countryCode, subdivisionCode, year).
 * The cache is unbounded but grows slowly in practice: at most one entry per
 * country/subdivision/year combination that is actually queried.
 *
 * <p>For countries not supported by jollyday (see
 * {@link CountrySubdivisionRegistry#hasHolidaySupport(String)}), an empty set
 * is returned silently. The user's calendar will simply show no automatic
 * holidays for that country.
 *
 * @author Luxferre86
 * @since 22.03.2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicHolidayService {

    private final CountrySubdivisionRegistry registry;

    /**
     * Cache key: normalized country code + subdivision code (or empty string
     * for nationwide) + year.
     */
    private record CacheKey(String countryCode, String subdivisionCode, Year year) {
    }

    private final Map<CacheKey, Set<LocalDate>> cache = new ConcurrentHashMap<>();

    /**
     * Returns all public holidays for the given country, optional subdivision,
     * and year.
     *
     * <p>If {@code subdivisionCode} is {@code null} or blank, only nationwide
     * holidays are returned. If the country is not supported by jollyday an
     * empty set is returned and a warning is logged once per unsupported code.
     *
     * @param countryCode     ISO 3166-1 alpha-2 code (case-insensitive);
     *                        {@code null} returns an empty set immediately
     * @param subdivisionCode full ISO 3166-2 code (e.g. {@code "DE-BY"}),
     *                        or {@code null} for nationwide holidays only
     * @param year            the calendar year
     * @return immutable set of holiday dates; never {@code null}
     */
    public Set<LocalDate> getHolidays(String countryCode,
                                      String subdivisionCode, Year year) {
        if (countryCode == null) {
            return Set.of();
        }
        String normCountry = countryCode.toUpperCase();
        String normSub = subdivisionCode != null ? subdivisionCode.toUpperCase() : "";
        CacheKey key = new CacheKey(normCountry, normSub, year);
        return cache.computeIfAbsent(key, k -> loadHolidays(normCountry, normSub, year));
    }

    /**
     * Returns {@code true} if the given date is a public holiday in the
     * specified country and subdivision.
     *
     * @param date            the date to check
     * @param countryCode     ISO 3166-1 alpha-2 code; {@code null} → false
     * @param subdivisionCode full ISO 3166-2 code; may be {@code null}
     * @return {@code true} when {@code date} is a public holiday
     */
    public boolean isHoliday(LocalDate date, String countryCode,
                             String subdivisionCode) {
        if (countryCode == null) {
            return false;
        }
        return getHolidays(countryCode, subdivisionCode, Year.from(date)).contains(date);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Set<LocalDate> loadHolidays(String countryCode,
                                        String subdivisionCode, Year year) {
        Optional<HolidayCalendar> calendarOpt = registry.getCalendar(countryCode);
        if (calendarOpt.isEmpty()) {
            log.warn("No jollyday calendar for country '{}' — automatic holiday detection disabled for this country", countryCode);
            return Set.of();
        }

        try {
            HolidayManager manager = HolidayManager.getInstance(ManagerParameters.create(calendarOpt.get()));

            Set<Holiday> holidaySet;
            if (subdivisionCode.isBlank()) {
                holidaySet = manager.getHolidays(year);
            } else {
                // stores full codes like "DE-BY"; jollyday wants only "BY"
                String regionSuffix = registry.toJollyDaySubdivision(subdivisionCode);
                holidaySet = manager.getHolidays(year, regionSuffix);
            }

            return holidaySet.stream().map(Holiday::getActualDate).collect(Collectors.toUnmodifiableSet());

        } catch (Exception e) {
            log.error("Failed to load holidays for country={} subdivision={} year={}", countryCode, subdivisionCode, year, e);
            return Set.of();
        }
    }
}