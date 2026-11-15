import { describe, expect, it } from 'vitest';
import type { DaySummary } from '../api/types';
import { computeYearStats } from './yearStats';

const day = (date: string, overrides: Partial<DaySummary> = {}): DaySummary => ({
  date,
  workday: true,
  workedMinutes: 0,
  targetMinutes: 480,
  balanceMinutes: -480,
  absenceType: null,
  absenceLabel: null,
  ...overrides,
});

describe('computeYearStats', () => {
  it('sums worked and target per month and in total up to today', () => {
    const stats = computeYearStats(
      [
        day('2026-01-05', { workedMinutes: 500 }),
        day('2026-01-06', { workedMinutes: 460 }),
        day('2026-02-02', { workedMinutes: 480 }),
        day('2026-03-02', { workedMinutes: 999 }),
      ],
      '2026-02-28',
    );

    expect(stats.months[0]).toMatchObject({ worked: 960, target: 960, balance: 0, workedDays: 2, average: 480 });
    expect(stats.months[1]).toMatchObject({ worked: 480, target: 480, balance: 0 });
    expect(stats.months[2]).toMatchObject({ worked: 0, target: 0 });
    expect(stats.total).toMatchObject({ worked: 1440, target: 1440, workedDays: 3, average: 480 });
  });

  it('computes the daily average only over days with bookings', () => {
    const stats = computeYearStats(
      [day('2026-01-05', { workedMinutes: 400 }), day('2026-01-06'), day('2026-01-07', { workedMinutes: 500 })],
      '2026-01-31',
    );

    expect(stats.total.average).toBe(450);
    expect(stats.total.balance).toBe(900 - 1440);
  });

  it('has an average of zero without bookings', () => {
    expect(computeYearStats([day('2026-01-05')], '2026-01-31').total.average).toBe(0);
  });

  it('counts absence days per type and ignores holidays and future days', () => {
    const stats = computeYearStats(
      [
        day('2026-01-05', { absenceType: 'SICK' }),
        day('2026-01-06', { absenceType: 'SICK' }),
        day('2026-01-07', { absenceType: 'HOLIDAY' }),
        day('2026-01-08', { absenceType: 'FLEX_DAY' }),
        day('2026-12-24', { absenceType: 'VACATION' }),
      ],
      '2026-06-01',
    );

    expect(stats.absenceDays).toEqual({ SICK: 2, FLEX_DAY: 1 });
  });
});
