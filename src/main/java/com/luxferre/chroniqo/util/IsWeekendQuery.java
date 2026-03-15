package com.luxferre.chroniqo.util;

import java.time.DayOfWeek;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

/**
 * {@link TemporalQuery} that returns {@code true} for
 * Saturday and Sunday and {@code false} for all other days of the week.
 *
 * <p>Usage: {@code localDate.query(new IsWeekendQuery())}
 *
 * @author Luxferre86
 * @since 28.02.2026
 */
public class IsWeekendQuery implements TemporalQuery<Boolean> {
    /**
     * Returns {@code true} if the day-of-week of {@code temporal} is Saturday
     * or Sunday, {@code false} otherwise.
     *
     * @param temporal the temporal object to query
     * @return {@code true} for weekend days
     */
    @Override
    public Boolean queryFrom(TemporalAccessor temporal) {
        DayOfWeek dow = DayOfWeek.of(temporal.get(ChronoField.DAY_OF_WEEK));
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }
}