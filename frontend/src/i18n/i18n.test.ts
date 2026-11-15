import { afterEach, describe, expect, it } from 'vitest';
import { normalizeLanguage, setActiveLanguage, translate, translatePlural } from './index';
import { de, en } from './messages';
import { formatLongDate, formatShortDate } from '../lib/dates';

afterEach(() => setActiveLanguage('en'));

describe('dictionaries', () => {
  it('have identical key sets', () => {
    expect(Object.keys(de).sort()).toEqual(Object.keys(en).sort());
  });

  it('have no empty values', () => {
    for (const [key, value] of Object.entries(de)) {
      expect(value.trim(), `de:${key}`).not.toBe('');
    }
    for (const [key, value] of Object.entries(en)) {
      expect(value.trim(), `en:${key}`).not.toBe('');
    }
  });
});

describe('translate', () => {
  it('interpolates placeholders', () => {
    expect(translate('error.requestFailed', { status: 500 }, 'en')).toBe('Request failed (500)');
    expect(translate('error.requestFailed', { status: 500 }, 'de')).toBe('Anfrage fehlgeschlagen (500)');
  });

  it('uses the active language by default', () => {
    setActiveLanguage('de');
    expect(translate('nav.month')).toBe('Monat');
    setActiveLanguage('en');
    expect(translate('nav.month')).toBe('Month');
  });

  it('keeps unknown placeholders untouched', () => {
    expect(translate('error.requestFailed', {}, 'en')).toBe('Request failed ({status})');
  });
});

describe('translatePlural', () => {
  const pluralBases = [...new Set(Object.keys(en).filter((k) => k.endsWith('.one')).map((k) => k.slice(0, -4)))];

  it('picks .one for exactly 1 and .other otherwise', () => {
    for (const base of pluralBases) {
      expect(translatePlural(base, 1, {}, 'en')).toBe(translate(`${base}.one` as keyof typeof en, { count: 1 }, 'en'));
      expect(translatePlural(base, 2, {}, 'en')).toBe(translate(`${base}.other` as keyof typeof en, { count: 2 }, 'en'));
      expect(translatePlural(base, 0, {}, 'de')).toBe(translate(`${base}.other` as keyof typeof en, { count: 0 }, 'de'));
    }
  });

  it('every .one key has a matching .other key', () => {
    for (const base of pluralBases) {
      expect(Object.keys(en)).toContain(`${base}.other`);
    }
  });
});

describe('normalizeLanguage', () => {
  it('maps locales to supported languages', () => {
    expect(normalizeLanguage('de')).toBe('de');
    expect(normalizeLanguage('de-AT')).toBe('de');
    expect(normalizeLanguage('DE_ch')).toBe('de');
    expect(normalizeLanguage('en-US')).toBe('en');
    expect(normalizeLanguage('fr')).toBe('en');
    expect(normalizeLanguage(null)).toBe('en');
    expect(normalizeLanguage(undefined)).toBe('en');
  });
});

describe('date formatting per language', () => {
  it('formats en as dd/mm/yyyy and de as dd.mm.yyyy', () => {
    setActiveLanguage('en');
    expect(formatShortDate('2026-11-14')).toBe('14/11/2026');
    expect(formatLongDate(new Date(2026, 10, 14))).toBe('Saturday, 14/11/2026');
    setActiveLanguage('de');
    expect(formatShortDate('2026-11-14')).toBe('14.11.2026');
    expect(formatLongDate(new Date(2026, 10, 14))).toBe('Samstag, 14.11.2026');
  });
});
