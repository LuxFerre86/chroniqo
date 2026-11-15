import type { PasswordPolicy } from '../api/types';
import { translate } from '../i18n';

/** Mirrors the server-side password policy (PasswordValidator.java) for instant feedback. */
export const DEFAULT_PASSWORD_POLICY: PasswordPolicy = {
  minLength: 12,
  maxLength: 72,
  specialChars: '!@#$%^&*()_+-=[]{}|;:,.<>?',
};

export function passwordHelperText(policy: PasswordPolicy): string {
  return translate('password.helper', { min: policy.minLength, special: policy.specialChars });
}

/** Returns the first violated rule or null when the password is acceptable. */
export function validatePassword(value: string, policy: PasswordPolicy = DEFAULT_PASSWORD_POLICY): string | null {
  if (!value || !value.trim()) return translate('password.required');
  if (value.length < policy.minLength) return translate('password.minLength', { min: policy.minLength });
  if (value.length > policy.maxLength) return translate('password.maxLength', { max: policy.maxLength });
  if (new TextEncoder().encode(value).length > policy.maxLength) {
    return translate('password.maxBytes', { max: policy.maxLength });
  }
  if (!/\p{Lu}/u.test(value)) return translate('password.upper');
  if (!/\p{Ll}/u.test(value)) return translate('password.lower');
  if (!/\d/.test(value)) return translate('password.digit');
  if (![...value].some((c) => policy.specialChars.includes(c))) {
    return translate('password.special');
  }
  return null;
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function isValidEmail(value: string): boolean {
  return EMAIL_PATTERN.test(value.trim());
}