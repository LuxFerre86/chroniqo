export type AbsenceType =
  | 'VACATION'
  | 'SICK'
  | 'FLEX_DAY'
  | 'PARENTAL_LEAVE'
  | 'BUSINESS_TRIP'
  | 'SPECIAL_LEAVE';
export type DayAbsenceType = AbsenceType | 'HOLIDAY';
export type DayOfWeekName =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export const DAYS_OF_WEEK: DayOfWeekName[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];

export interface PasswordPolicy {
  minLength: number;
  maxLength: number;
  specialChars: string;
}

export interface AppConfig {
  appName: string;
  version: string | null;
  registrationEnabled: boolean;
  passwordPolicy: PasswordPolicy;
}

export interface LookupItem {
  code: string;
  name: string;
}

export interface UserProfile {
  email: string;
  firstName: string;
  lastName: string;
  weeklyTargetHours: number;
  workingDays: DayOfWeekName[];
  firstDayOfWeek: DayOfWeekName;
  countryCode: string | null;
  subdivisionCode: string | null;
  createdAt: string | null;
  lastLoginAt: string | null;
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  weeklyTargetHours: number;
  workingDays: DayOfWeekName[];
  countryCode: string | null;
  subdivisionCode: string | null;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  weeklyTargetHours: number;
  countryCode: string | null;
  subdivisionCode: string | null;
}

/** Time of day as "HH:mm" or "HH:mm:ss"; date as "YYYY-MM-DD". */
export interface TimeEntry {
  id: string;
  date: string;
  startTime: string | null;
  endTime: string | null;
  breakMinutes: number | null;
  notes: string | null;
}

export interface TimeEntryInput {
  date: string;
  startTime: string;
  endTime: string | null;
  breakMinutes: number | null;
  notes: string | null;
}

export interface Absence {
  date: string;
  type: AbsenceType;
}

export interface DaySummary {
  date: string;
  workday: boolean;
  workedMinutes: number;
  targetMinutes: number;
  balanceMinutes: number;
  absenceType: DayAbsenceType | null;
  absenceLabel: string | null;
}

export interface WeeklyProgress {
  workedMinutes: number;
  targetMinutes: number;
  percentage: number;
  hasTarget: boolean;
}

export interface CustomHolidayInput {
  name: string;
  startDate: string;
  endDate: string;
  recurring: boolean;
}

export interface CustomHoliday extends CustomHolidayInput {
  id: string;
}

export interface VacationAccount {
  year: number;
  entitlement: number;
  taken: number;
  planned: number;
  remaining: number;
}

export interface Dashboard {
  today: DaySummary | null;
  week: DaySummary[];
  currentBalanceMinutes: number;
  weeklyProgress: WeeklyProgress;
}

export type VerifyEmailStatus = 'INVALID' | 'VERIFIED_LOGGED_IN' | 'VERIFIED_LOGIN_REQUIRED';
