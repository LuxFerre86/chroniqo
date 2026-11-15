import { request } from './http';
import type {
  Absence,
  AbsenceType,
  AppConfig,
  Dashboard,
  DaySummary,
  LookupItem,
  RegisterRequest,
  TimeEntry,
  TimeEntryInput,
  UpdateProfileRequest,
  UserProfile,
  VerifyEmailStatus,
} from './types';

export const api = {
  // ----- public -----
  getConfig: () => request<AppConfig>('GET', '/api/config'),
  getCountries: () => request<LookupItem[]>('GET', '/api/countries'),
  getSubdivisions: (countryCode: string) =>
    request<LookupItem[]>('GET', `/api/countries/${encodeURIComponent(countryCode)}/subdivisions`),

  // ----- auth -----
  login: (email: string, password: string, rememberMe: boolean) =>
    request<void>('POST', '/api/auth/login', {
      form: { username: email, password, ...(rememberMe ? { 'remember-me': 'true' } : {}) },
      suppressUnauthorizedHandler: true,
    }),
  logout: () => request<void>('POST', '/api/auth/logout', { suppressUnauthorizedHandler: true }),
  register: (data: RegisterRequest) => request<void>('POST', '/api/auth/register', { body: data }),
  verifyEmail: (token: string) =>
    request<{ status: VerifyEmailStatus }>('POST', '/api/auth/verify-email', { body: { token } }),
  resendVerificationEmail: (email: string) =>
    request<void>('POST', '/api/auth/verification-email/resend', { body: { email } }),
  requestPasswordReset: (email: string) =>
    request<void>('POST', '/api/auth/password-reset/request', { body: { email } }),
  confirmPasswordReset: (token: string, newPassword: string) =>
    request<void>('POST', '/api/auth/password-reset/confirm', { body: { token, newPassword } }),

  // ----- me -----
  getMe: () => request<UserProfile>('GET', '/api/me', { suppressUnauthorizedHandler: true }),
  updateProfile: (data: UpdateProfileRequest) => request<UserProfile>('PUT', '/api/me', { body: data }),
  changePassword: (currentPassword: string, newPassword: string) =>
    request<void>('PUT', '/api/me/password', { body: { currentPassword, newPassword } }),
  deleteAccount: (password: string) => request<void>('DELETE', '/api/me', { body: { password } }),

  // ----- time data -----
  getDashboard: () => request<Dashboard>('GET', '/api/dashboard'),
  getSummary: (from: string, to: string) => request<DaySummary[]>('GET', '/api/summary', { query: { from, to } }),
  getTimeEntries: (from: string, to: string) =>
    request<TimeEntry[]>('GET', '/api/time-entries', { query: { from, to } }),
  createTimeEntry: (data: TimeEntryInput) => request<void>('POST', '/api/time-entries', { body: data }),
  updateTimeEntry: (id: string, data: TimeEntryInput) =>
    request<void>('PUT', `/api/time-entries/${encodeURIComponent(id)}`, { body: data }),
  deleteTimeEntry: (id: string) => request<void>('DELETE', `/api/time-entries/${encodeURIComponent(id)}`),
  getAbsences: (from: string, to: string) => request<Absence[]>('GET', '/api/absences', { query: { from, to } }),
  saveAbsence: (startDate: string, endDate: string, absenceType: AbsenceType) =>
    request<void>('PUT', '/api/absences', { body: { startDate, endDate, absenceType } }),
  deleteAbsences: (from: string, to: string, type?: AbsenceType) =>
    request<void>('DELETE', '/api/absences', { query: { from, to, type } }),
};
