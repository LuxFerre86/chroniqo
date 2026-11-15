import type { DaySummary } from '../api/types';

export interface MonthStats {
  month: number;
  target: number;
  worked: number;
  balance: number;
  /** Days with booked time; basis of the daily average. */
  workedDays: number;
  /** Average worked minutes per day with booked time (0 when there is none). */
  average: number;
}

export interface YearStats {
  months: MonthStats[];
  total: MonthStats;
  /** Number of days up to today per absence type (holidays excluded). */
  absenceDays: Record<string, number>;
}

function emptyMonth(month: number): MonthStats {
  return { month, target: 0, worked: 0, balance: 0, workedDays: 0, average: 0 };
}

function finish(stats: MonthStats): MonthStats {
  stats.balance = stats.worked - stats.target;
  stats.average = stats.workedDays > 0 ? Math.round(stats.worked / stats.workedDays) : 0;
  return stats;
}

/** Aggregates the days of one year up to and including today (future days are ignored). */
export function computeYearStats(days: DaySummary[], today: string): YearStats {
  const months = Array.from({ length: 12 }, (_, i) => emptyMonth(i + 1));
  const total = emptyMonth(0);
  const absenceDays: Record<string, number> = {};

  for (const day of days) {
    if (day.date > today) continue;
    const month = months[Number(day.date.slice(5, 7)) - 1];
    for (const bucket of [month, total]) {
      bucket.target += day.targetMinutes;
      bucket.worked += day.workedMinutes;
      if (day.workedMinutes > 0) bucket.workedDays += 1;
    }
    if (day.absenceType && day.absenceType !== 'HOLIDAY') {
      absenceDays[day.absenceType] = (absenceDays[day.absenceType] ?? 0) + 1;
    }
  }

  months.forEach(finish);
  finish(total);
  return { months, total, absenceDays };
}
