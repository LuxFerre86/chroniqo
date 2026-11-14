package com.luxferre.chroniqo.util;

import java.time.DayOfWeek;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * Resolves the first day of the week for a user's country.
 */
public final class WeekStart {

    private WeekStart() {
    }

    /**
     * Returns the first day of the week used in the given country, e.g. Monday
     * for {@code DE} and Sunday for {@code US}.
     *
     * @param countryCode ISO 3166-1 alpha-2 country code, may be {@code null}
     * @return the regional first day of the week, or {@link DayOfWeek#MONDAY}
     * if the country is missing or unknown
     */
    public static DayOfWeek forCountry(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) {
            return DayOfWeek.MONDAY;
        }
        Locale locale = Locale.of("", countryCode.toUpperCase(Locale.ROOT));
        return WeekFields.of(locale).getFirstDayOfWeek();
    }
}
