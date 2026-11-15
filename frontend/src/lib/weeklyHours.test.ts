import { describe, expect, it } from 'vitest';
import { isValidWeeklyHours } from './weeklyHours';

describe('isValidWeeklyHours', () => {
  it.each([0, 20, 37.5, 39.5, 40, 80, 0.1])('accepts %s', (value) => {
    expect(isValidWeeklyHours(value)).toBe(true);
  });

  it.each([null, NaN, -1, 80.1, 81, 37.55, 37.25])('rejects %s', (value) => {
    expect(isValidWeeklyHours(value)).toBe(false);
  });
});
