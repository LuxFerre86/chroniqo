package com.luxferre.chroniqo.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.DayOfWeek;

import static org.assertj.core.api.Assertions.assertThat;

class WeekStartTest {

    @ParameterizedTest
    @CsvSource({
            "DE, MONDAY",
            "AT, MONDAY",
            "GB, MONDAY",
            "US, SUNDAY",
            "us, SUNDAY",
            "CA, SUNDAY",
            "JP, SUNDAY",
            "AE, MONDAY"
    })
    void forCountry_returnsRegionalFirstDay(String country, DayOfWeek expected) {
        assertThat(WeekStart.forCountry(country)).isEqualTo(expected);
    }

    @Test
    void forCountry_withoutCountry_fallsBackToMonday() {
        assertThat(WeekStart.forCountry(null)).isEqualTo(DayOfWeek.MONDAY);
        assertThat(WeekStart.forCountry("")).isEqualTo(DayOfWeek.MONDAY);
        assertThat(WeekStart.forCountry("DEU")).isEqualTo(DayOfWeek.MONDAY);
    }
}
