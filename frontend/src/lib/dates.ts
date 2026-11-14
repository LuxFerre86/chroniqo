/** Local-calendar date helpers. ISO dates are always "YYYY-MM-DD" in local time (never UTC). */

import { DAYS_OF_WEEK, type DayOfWeekName } from '../api/types';

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
  return new Date(ym.year, ym.month - 1, 1).toLocaleDateString('en-GB', { month: 'long', year: 'numeric' });
}

/** "Saturday, November 14, 2026" (dashboard header) */
export function formatLongDate(date: Date): string {
  return date.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' });
}

/** "14.11.2026" (dialog header) */
export function formatShortDate(iso: string): string {
  const [y, m, d] = iso.split('-');
  return `${d}.${m}.${y}`;
}

export function weekdayShort(date: Date): string {
  return date.toLocaleDateString('en-GB', { weekday: 'short' });
}

export function isWeekend(iso: string): boolean {
  const day = parseIsoDate(iso).getDay();
  return day === 0 || day === 6;
}
