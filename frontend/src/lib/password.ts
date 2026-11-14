import type { PasswordPolicy } from '../api/types';

/** Mirrors the server-side password policy (PasswordValidator.java) for instant feedback. */
export const DEFAULT_PASSWORD_POLICY: PasswordPolicy = {
  minLength: 12,
  maxLength: 72,
  specialChars: '!@#$%^&*()_+-=[]{}|;:,.<>?',
};

export function passwordHelperText(policy: PasswordPolicy): string {
  return `At least ${policy.minLength} characters, including uppercase, lowercase, a digit and a special character (${policy.specialChars})`;
}

/** Returns the first violated rule or null when the password is acceptable. */
export function validatePassword(value: string, policy: PasswordPolicy = DEFAULT_PASSWORD_POLICY): string | null {
  if (!value || !value.trim()) return 'Password is required';
  if (value.length < policy.minLength) return `Password must be at least ${policy.minLength} characters`;
  if (value.length > policy.maxLength) return `Password must not exceed ${policy.maxLength} characters`;
  if (new TextEncoder().encode(value).length > policy.maxLength) {
    return `Password must not exceed ${policy.maxLength} bytes (non-ASCII characters count as several)`;
  }
  if (!/\p{Lu}/u.test(value)) return 'Password must contain at least one uppercase letter';
  if (!/\p{Ll}/u.test(value)) return 'Password must contain at least one lowercase letter';
  if (!/\d/.test(value)) return 'Password must contain at least one digit';
  if (![...value].some((c) => policy.specialChars.includes(c))) {
    return 'Password must contain at least one special character';
  }
  return null;
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function isValidEmail(value: string): boolean {
  return EMAIL_PATTERN.test(value.trim());
}
