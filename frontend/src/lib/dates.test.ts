import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  addMonths,
  countDays,
  currentYearMonth,
  daysInMonth,
  firstWeekdayIndex,
  formatDateTime,
  formatLongDate,
  formatMonthLabel,
  formatShortDate,
  isWeekend,
  isoOf,
  parseIsoDate,
  sameMonth,
  toIsoDate,
  todayIso,
  weekdayIndex,
  weekdayOrder,
  weekdayShort,
} from './dates';

afterEach(() => {
  vi.useRealTimers();
});

describe('toIsoDate / parseIsoDate', () => {
  it('formats local calendar dates with zero padding', () => {
    expect(toIsoDate(new Date(2026, 0, 5))).toBe('2026-01-05');
    expect(toIsoDate(new Date(2026, 10, 14))).toBe('2026-11-14');
  });

  it('uses local time, not UTC (late evening and early morning)', () => {
    expect(toIsoDate(new Date(2026, 11, 31, 23, 59))).toBe('2026-12-31');
    expect(toIsoDate(new Date(2026, 0, 1, 0, 1))).toBe('2026-01-01');
  });

  it('round-trips ISO dates', () => {
    const date = parseIsoDate('2024-02-29');
    expect(date.getFullYear()).toBe(2024);
    expect(date.getMonth()).toBe(1);
    expect(date.getDate()).toBe(29);
    expect(toIsoDate(date)).toBe('2024-02-29');
  });
});

describe('todayIso / currentYearMonth', () => {
  it('derive from the current local date', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 10, 14, 12, 0));
    expect(todayIso()).toBe('2026-11-14');
    expect(currentYearMonth()).toEqual({ year: 2026, month: 11 });
  });
});

describe('addMonths', () => {
  it('moves within a year', () => {
    expect(addMonths({ year: 2026, month: 5 }, 1)).toEqual({ year: 2026, month: 6 });
    expect(addMonths({ year: 2026, month: 5 }, -1)).toEqual({ year: 2026, month: 4 });
  });

  it('wraps over year boundaries', () => {
    expect(addMonths({ year: 2026, month: 12 }, 1)).toEqual({ year: 2027, month: 1 });
    expect(addMonths({ year: 2026, month: 1 }, -1)).toEqual({ year: 2025, month: 12 });
    expect(addMonths({ year: 2026, month: 3 }, 24)).toEqual({ year: 2028, month: 3 });
  });
});

describe('sameMonth', () => {
  it('compares year and month', () => {
    expect(sameMonth({ year: 2026, month: 3 }, { year: 2026, month: 3 })).toBe(true);
    expect(sameMonth({ year: 2026, month: 3 }, { year: 2025, month: 3 })).toBe(false);
    expect(sameMonth({ year: 2026, month: 3 }, { year: 2026, month: 4 })).toBe(false);
  });
});

describe('daysInMonth', () => {
  it.each([
    [{ year: 2026, month: 1 }, 31],
    [{ year: 2026, month: 2 }, 28],
    [{ year: 2024, month: 2 }, 29],
    [{ year: 2026, month: 4 }, 30],
    [{ year: 2026, month: 12 }, 31],
  ])('%j has %i days', (ym, expected) => {
    expect(daysInMonth(ym)).toBe(expected);
  });
});

describe('isoOf', () => {
  it('builds padded ISO dates', () => {
    expect(isoOf({ year: 2026, month: 3 }, 7)).toBe('2026-03-07');
    expect(isoOf({ year: 2026, month: 11 }, 30)).toBe('2026-11-30');
  });
});

describe('firstWeekdayIndex', () => {
  it('is 0 for Monday and 6 for Sunday', () => {
    // 1 Jun 2026 is a Monday, 1 Nov 2026 is a Sunday.
    expect(firstWeekdayIndex({ year: 2026, month: 6 })).toBe(0);
    expect(firstWeekdayIndex({ year: 2026, month: 11 })).toBe(6);
  });

  it('handles a Wednesday start', () => {
    // 1 Jan 2025 is a Wednesday.
    expect(firstWeekdayIndex({ year: 2025, month: 1 })).toBe(2);
  });

  it('counts from the given first day of the week', () => {
    // 1 Nov 2026 is a Sunday: first cell for Sunday-start weeks, last for Monday-start weeks.
    expect(firstWeekdayIndex({ year: 2026, month: 11 }, 'SUNDAY')).toBe(0);
    expect(firstWeekdayIndex({ year: 2026, month: 11 }, 'MONDAY')).toBe(6);
    // 1 Jun 2026 is a Monday.
    expect(firstWeekdayIndex({ year: 2026, month: 6 }, 'SUNDAY')).toBe(1);
    expect(firstWeekdayIndex({ year: 2026, month: 6 }, 'SATURDAY')).toBe(2);
  });
});

describe('weekdayOrder', () => {
  it('defaults to Monday first', () => {
    expect(weekdayOrder()).toEqual([0, 1, 2, 3, 4, 5, 6]);
  });

  it('rotates to the given first day', () => {
    expect(weekdayOrder('SUNDAY')).toEqual([6, 0, 1, 2, 3, 4, 5]);
    expect(weekdayOrder('SATURDAY')).toEqual([5, 6, 0, 1, 2, 3, 4]);
  });
});

describe('weekdayIndex', () => {
  it('is Monday based', () => {
    expect(weekdayIndex(new Date(2026, 10, 16))).toBe(0);
    expect(weekdayIndex(new Date(2026, 10, 15))).toBe(6);
  });
});

describe('formatters', () => {
  it('formats the month label', () => {
    expect(formatMonthLabel({ year: 2026, month: 11 })).toBe('November 2026');
  });

  it('formats the long dashboard date', () => {
    expect(formatLongDate(new Date(2026, 10, 14))).toBe('Saturday, 14/11/2026');
  });

  it('formats server timestamps with and without time', () => {
    expect(formatDateTime('2026-11-14T08:05:30')).toBe('14/11/2026, 08:05');
    expect(formatDateTime('2026-11-14 08:05:30')).toBe('14/11/2026, 08:05');
    expect(formatDateTime('2026-11-14')).toBe('14/11/2026');
    expect(formatDateTime(null, 'Never')).toBe('Never');
    expect(formatDateTime('garbage', 'Unknown')).toBe('Unknown');
  });

  it('counts weekdays and weekend days of a range', () => {
    expect(countDays('2026-11-14', '2026-11-18')).toEqual({ weekdays: 3, weekendDays: 2 });
    expect(countDays('2026-11-16', '2026-11-16')).toEqual({ weekdays: 1, weekendDays: 0 });
  });

  it('formats the short dialog date', () => {
    expect(formatShortDate('2026-11-14')).toBe('14/11/2026');
  });

  it('formats the short weekday', () => {
    expect(weekdayShort(new Date(2026, 10, 14))).toBe('Sat');
  });
});

describe('isWeekend', () => {
  it('detects Saturday and Sunday', () => {
    expect(isWeekend('2026-11-14')).toBe(true);
    expect(isWeekend('2026-11-15')).toBe(true);
  });

  it('is false on weekdays', () => {
    expect(isWeekend('2026-11-13')).toBe(false);
    expect(isWeekend('2026-11-16')).toBe(false);
  });
});
