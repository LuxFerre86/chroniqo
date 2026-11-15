import { auth as enAuth } from './en/auth';
import { common as enCommon } from './en/common';
import { dashboard as enDashboard } from './en/dashboard';
import { entry as enEntry } from './en/entry';
import { month as enMonth } from './en/month';
import { settings as enSettings } from './en/settings';
import { year as enYear } from './en/year';
import { auth as deAuth } from './de/auth';
import { common as deCommon } from './de/common';
import { dashboard as deDashboard } from './de/dashboard';
import { entry as deEntry } from './de/entry';
import { month as deMonth } from './de/month';
import { settings as deSettings } from './de/settings';
import { year as deYear } from './de/year';

export const en = {
  ...enCommon,
  ...enAuth,
  ...enDashboard,
  ...enEntry,
  ...enMonth,
  ...enSettings,
  ...enYear,
} as const;

export type TranslationKey = keyof typeof en;

/** The German dictionary is typed against the English keys, so a missing translation fails the type check. */
export const de: Record<TranslationKey, string> = {
  ...deCommon,
  ...deAuth,
  ...deDashboard,
  ...deEntry,
  ...deMonth,
  ...deSettings,
  ...deYear,
};

export const dictionaries = { en, de } as const;
