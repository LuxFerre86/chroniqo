/**
 * Minimal i18n layer (English and German, no dependency).
 *
 * - {@link translate} works anywhere (also outside React) and uses the active language,
 * - {@link useT} gives components `t` / `tp` plus the active language,
 * - {@link I18nProvider} resolves the language (profile of the signed-in user, otherwise the
 *   browser language, fallback English) and remounts the tree when it changes so that every
 *   language-dependent value (dates, numbers, texts) is recomputed.
 *
 * Placeholders look like `{name}`. Plurals use the keys `<key>.one` and `<key>.other`.
 */
import { createContext, Fragment, useContext, useMemo } from 'react';
import type { ReactNode } from 'react';
import { useAuth } from '../auth/AuthContext';
import { dictionaries, type TranslationKey } from './messages';

export type Language = 'en' | 'de';
export type { TranslationKey };

export const LANGUAGES: readonly Language[] = ['en', 'de'];

type Params = Record<string, string | number>;

export function normalizeLanguage(value: string | null | undefined): Language {
  const primary = (value ?? '').toLowerCase().split(/[-_]/)[0];
  return primary === 'de' ? 'de' : 'en';
}

export function detectBrowserLanguage(): Language {
  return normalizeLanguage(typeof navigator === 'undefined' ? null : navigator.language);
}

let activeLanguage: Language = detectBrowserLanguage();

export function getLanguage(): Language {
  return activeLanguage;
}

/** Test helper / manual override of the active language outside React. */
export function setActiveLanguage(language: Language): void {
  activeLanguage = language;
}

/** BCP 47 locale used for date and number formatting. */
export function getLocale(language: Language = activeLanguage): string {
  return language === 'de' ? 'de-DE' : 'en-GB';
}

function interpolate(template: string, params?: Params): string {
  if (!params) return template;
  return template.replace(/\{(\w+)\}/g, (match, name: string) =>
    name in params ? String(params[name]) : match,
  );
}

export function translate(key: TranslationKey, params?: Params, language: Language = activeLanguage): string {
  const template = dictionaries[language][key] ?? dictionaries.en[key] ?? key;
  return interpolate(template, params);
}

/** Plural-aware lookup: uses `<key>.one` for exactly 1, otherwise `<key>.other`; `{count}` is filled in. */
export function translatePlural(
  key: string,
  count: number,
  params?: Params,
  language: Language = activeLanguage,
): string {
  const suffix = count === 1 ? 'one' : 'other';
  return translate(`${key}.${suffix}` as TranslationKey, { count, ...params }, language);
}

interface I18nContextValue {
  language: Language;
  t: (key: TranslationKey, params?: Params) => string;
  tp: (key: string, count: number, params?: Params) => string;
}

function buildValue(language: Language): I18nContextValue {
  return {
    language,
    t: (key, params) => translate(key, params, language),
    tp: (key, count, params) => translatePlural(key, count, params, language),
  };
}

const I18nContext = createContext<I18nContextValue | null>(null);

/** Provides the language of the signed-in user, or the browser language while anonymous. */
export function I18nProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const language = user?.language ? normalizeLanguage(user.language) : detectBrowserLanguage();
  activeLanguage = language;
  if (typeof document !== 'undefined') document.documentElement.lang = language;
  const value = useMemo(() => buildValue(language), [language]);
  return (
    <I18nContext.Provider value={value}>
      <Fragment key={language}>{children}</Fragment>
    </I18nContext.Provider>
  );
}

/** Works without a provider (falls back to the active module language). */
export function useT(): I18nContextValue {
  const ctx = useContext(I18nContext);
  const fallback = getLanguage();
  return useMemo(() => ctx ?? buildValue(fallback), [ctx, fallback]);
}
