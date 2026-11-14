import { describe, expect, it } from 'vitest';
import {
  formatBalance,
  formatClock,
  formatHours,
  formatMinutes,
  formatRunningBalance,
  formatSignedClock,
  trimSeconds,
  worked,
} from './format';

describe('formatMinutes', () => {
  it.each([
    [0, '0:00 h'],
    [5, '0:05 h'],
    [468, '7:48 h'],
    [600, '10:00 h'],
    [-58, '-0:58 h'],
    [-125, '-2:05 h'],
  ])('formats %i minutes as %s', (minutes, expected) => {
    expect(formatMinutes(minutes)).toBe(expected);
  });
});

describe('formatBalance', () => {
  it.each([
    [22, '+0:22 h'],
    [0, '+0:00 h'],
    [-58, '-0:58 h'],
    [90, '+1:30 h'],
  ])('formats %i minutes as %s', (minutes, expected) => {
    expect(formatBalance(minutes)).toBe(expected);
  });
});

describe('formatClock', () => {
  it('renders hours and minutes without unit', () => {
    expect(formatClock(468)).toBe('7:48');
    expect(formatClock(0)).toBe('0:00');
  });

  it('drops the sign for negative values', () => {
    expect(formatClock(-58)).toBe('0:58');
  });
});

describe('formatSignedClock', () => {
  it.each([
    [22, '+0:22'],
    [0, '+0:00'],
    [-58, '-0:58'],
    [-600, '-10:00'],
  ])('formats %i minutes as %s', (minutes, expected) => {
    expect(formatSignedClock(minutes)).toBe(expected);
  });
});

describe('formatRunningBalance', () => {
  it('renders exactly zero without a sign', () => {
    expect(formatRunningBalance(0)).toBe('0:00');
  });

  it('renders non-zero values signed', () => {
    expect(formatRunningBalance(75)).toBe('+1:15');
    expect(formatRunningBalance(-75)).toBe('-1:15');
  });
});

describe('formatHours', () => {
  it('omits minutes when they are zero', () => {
    expect(formatHours(480)).toBe('8h');
    expect(formatHours(0)).toBe('0h');
  });

  it('shows padded minutes otherwise', () => {
    expect(formatHours(490)).toBe('8:10h');
    expect(formatHours(485)).toBe('8:05h');
  });
});

describe('trimSeconds', () => {
  it('normalizes HH:mm:ss to HH:mm', () => {
    expect(trimSeconds('08:30:00')).toBe('08:30');
  });

  it('keeps HH:mm untouched', () => {
    expect(trimSeconds('08:30')).toBe('08:30');
  });

  it('returns an empty string for missing values', () => {
    expect(trimSeconds(null)).toBe('');
    expect(trimSeconds(undefined)).toBe('');
    expect(trimSeconds('')).toBe('');
  });
});

describe('worked', () => {
  it('subtracts the break from the time span', () => {
    expect(worked('08:00', '16:30', 30)).toBe(480);
  });

  it('treats a missing break as zero', () => {
    expect(worked('08:00', '12:00', null)).toBe(240);
  });

  it('accepts times with seconds', () => {
    expect(worked('08:00:00', '17:00:00', 60)).toBe(480);
  });

  it('returns zero for open entries', () => {
    expect(worked('08:00', null, 0)).toBe(0);
    expect(worked(null, '17:00', 0)).toBe(0);
  });

  it('never returns a negative duration', () => {
    expect(worked('08:00', '08:30', 60)).toBe(0);
    expect(worked('17:00', '08:00', 0)).toBe(0);
  });
});
