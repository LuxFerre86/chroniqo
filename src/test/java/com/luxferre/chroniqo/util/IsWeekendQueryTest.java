package com.luxferre.chroniqo.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class IsWeekendQueryTest {

    private final IsWeekendQuery query = new IsWeekendQuery();

    @ParameterizedTest(name = "{0} is a weekend day")
    @EnumSource(value = DayOfWeek.class, names = {"SATURDAY", "SUNDAY"})
    void weekendDays_returnTrue(DayOfWeek day) {
        LocalDate date = LocalDate.now().with(day);
        assertThat(date.query(query)).isTrue();
    }

    @ParameterizedTest(name = "{0} is a weekday")
    @EnumSource(value = DayOfWeek.class, names = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"})
    void weekdays_returnFalse(DayOfWeek day) {
        LocalDate date = LocalDate.now().with(day);
        assertThat(date.query(query)).isFalse();
    }

    @Test
    void saturday_returnsTrue() {
        assertThat(LocalDate.now().with(DayOfWeek.SATURDAY).query(query)).isTrue();
    }

    @Test
    void sunday_returnsTrue() {
        assertThat(LocalDate.now().with(DayOfWeek.SUNDAY).query(query)).isTrue();
    }

    @Test
    void monday_returnsFalse() {
        assertThat(LocalDate.now().with(DayOfWeek.MONDAY).query(query)).isFalse();
    }

    @Test
    void friday_returnsFalse() {
        assertThat(LocalDate.now().with(DayOfWeek.FRIDAY).query(query)).isFalse();
    }
}