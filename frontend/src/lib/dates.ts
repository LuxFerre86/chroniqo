/** Local-calendar date helpers. ISO dates are always "YYYY-MM-DD" in local time (never UTC). */

import { DAYS_OF_WEEK, type DayOfWeekName } from '../api/types';
import { getLanguage, getLocale } from '../i18n';

const pad = (n: number) => String(n).padStart(2, '0');

export function toIsoDate(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function parseIsoDate(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
}

export function todayIso(): string {
  return toIsoDate(new Date());
}

export interface YearMonth {
  year: number;
  /** 1-12 */
  month: number;
}

export function currentYearMonth(): YearMonth {
  const now = new Date();
  return { year: now.getFullYear(), month: now.getMonth() + 1 };
}

export function addMonths(ym: YearMonth, delta: number): YearMonth {
  const d = new Date(ym.year, ym.month - 1 + delta, 1);
  return { year: d.getFullYear(), month: d.getMonth() + 1 };
}

export function sameMonth(a: YearMonth, b: YearMonth): boolean {
  return a.year === b.year && a.month === b.month;
}

export function daysInMonth(ym: YearMonth): number {
  return new Date(ym.year, ym.month, 0).getDate();
}

export function isoOf(ym: YearMonth, day: number): string {
  return `${ym.year}-${pad(ym.month)}-${pad(day)}`;
}

/** Monday-based weekday index (0 = Monday … 6 = Sunday) of a date. */
export function weekdayIndex(date: Date): number {
  return (date.getDay() + 6) % 7;
}

/** Monday-based index (0 = Monday … 6 = Sunday) for a weekday name. */
export function weekdayIndexOf(day: DayOfWeekName): number {
  return DAYS_OF_WEEK.indexOf(day);
}

/** Monday-based weekday indices in display order, starting with the given first day of the week. */
export function weekdayOrder(firstDay: DayOfWeekName = 'MONDAY'): number[] {
  const start = weekdayIndexOf(firstDay);
  return Array.from({ length: 7 }, (_, i) => (start + i) % 7);
}

/** Number of empty cells before the first of the month when weeks start on `firstDay`. */
export function firstWeekdayIndex(ym: YearMonth, firstDay: DayOfWeekName = 'MONDAY'): number {
  const monthStart = weekdayIndex(new Date(ym.year, ym.month - 1, 1));
  return (monthStart - weekdayIndexOf(firstDay) + 7) % 7;
}

/** "November 2026" */
export function formatMonthLabel(ym: YearMonth): string {
  return new Date(ym.year, ym.month - 1, 1).toLocaleDateString(getLocale(), { month: 'long', year: 'numeric' });
}

/** "Saturday, 14.11.2026" (dashboard header) */
export function formatLongDate(date: Date): string {
  const weekday = date.toLocaleDateString(getLocale(), { weekday: 'long' });
  return `${weekday}, ${formatShortDate(toIsoDate(date))}`;
}

/** "14.11.2026" – the one date format used throughout the app. */
export function formatShortDate(iso: string): string {
  const [y, m, d] = iso.split('-');
  return getLanguage() === 'de' ? `${d}.${m}.${y}` : `${d}/${m}/${y}`;
}

/** "14.11.2026, 09:30" from a server timestamp ("2026-11-14T09:30:00"); date-only values lose the time part. */
export function formatDateTime(value: string | null | undefined, fallback = ''): string {
  if (!value) return fallback;
  const match = /^(\d{4}-\d{2}-\d{2})(?:[T ](\d{2}:\d{2}))?/.exec(value);
  if (!match) return fallback;
  const date = formatShortDate(match[1]);
  return match[2] ? `${date}, ${match[2]}` : date;
}

export function weekdayShort(date: Date): string {
  return date.toLocaleDateString(getLocale(), { weekday: 'short' });
}

export function isWeekend(iso: string): boolean {
  const day = parseIsoDate(iso).getDay();
  return day === 0 || day === 6;
}

/** Splits an inclusive ISO date range into Monday–Friday days and weekend days. */
export function countDays(startIso: string, endIso: string): { weekdays: number; weekendDays: number } {
  let weekdays = 0;
  let weekendDays = 0;
  const cursor = parseIsoDate(startIso);
  const end = parseIsoDate(endIso);
  while (cursor <= end) {
    if (cursor.getDay() === 0 || cursor.getDay() === 6) weekendDays++;
    else weekdays++;
    cursor.setDate(cursor.getDate() + 1);
  }
  return { weekdays, weekendDays };
}
