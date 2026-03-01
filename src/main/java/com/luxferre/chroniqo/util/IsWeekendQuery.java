package com.luxferre.chroniqo.util;

import java.time.DayOfWeek;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

public class IsWeekendQuery implements TemporalQuery<Boolean> {
    @Override
    public Boolean queryFrom(TemporalAccessor temporal) {
        DayOfWeek dow = DayOfWeek.of(temporal.get(ChronoField.DAY_OF_WEEK));
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }
}
