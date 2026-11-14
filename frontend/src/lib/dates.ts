/** Local-calendar date helpers. ISO dates are always "YYYY-MM-DD" in local time (never UTC). */

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

/** 0 = Monday … 6 = Sunday weekday index of the first day of the month. */
export function firstWeekdayIndex(ym: YearMonth): number {
  return (new Date(ym.year, ym.month - 1, 1).getDay() + 6) % 7;
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
