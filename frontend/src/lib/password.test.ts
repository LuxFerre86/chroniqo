import { describe, expect, it } from 'vitest';
import { DEFAULT_PASSWORD_POLICY, isValidEmail, passwordHelperText, validatePassword } from './password';

describe('DEFAULT_PASSWORD_POLICY', () => {
  it('mirrors the server policy', () => {
    expect(DEFAULT_PASSWORD_POLICY).toEqual({
      minLength: 12,
      maxLength: 72,
      specialChars: '!@#$%^&*()_+-=[]{}|;:,.<>?',
    });
  });
});

describe('validatePassword', () => {
  it('accepts a compliant password', () => {
    expect(validatePassword('Abcdefghij1!')).toBeNull();
    expect(validatePassword('Correct-Horse-Battery-9')).toBeNull();
  });

  it.each([['', 'Password is required'], ['   ', 'Password is required']])(
    'rejects blank input %j',
    (value, message) => {
      expect(validatePassword(value)).toBe(message);
    },
  );

  it('enforces the minimum length', () => {
    expect(validatePassword('Abcdefghi1!')).toBe('Password must be at least 12 characters');
    expect(validatePassword('Abcdefghij1!')).toBeNull();
  });

  it('enforces the maximum length', () => {
    const base = 'Aa1!';
    expect(validatePassword(base + 'a'.repeat(68))).toBeNull();
    expect(validatePassword(base + 'a'.repeat(69))).toBe('Password must not exceed 72 characters');
  });

  it('counts multi-byte characters towards the byte limit', () => {
    expect(validatePassword('Aa1!' + 'ü'.repeat(36))).toBe(
      'Password must not exceed 72 bytes (non-ASCII characters count as several)',
    );
    expect(validatePassword('Aa1!' + 'ü'.repeat(34))).toBeNull();
  });

  it('requires an uppercase letter', () => {
    expect(validatePassword('abcdefghij1!')).toBe('Password must contain at least one uppercase letter');
  });

  it('requires a lowercase letter', () => {
    expect(validatePassword('ABCDEFGHIJ1!')).toBe('Password must contain at least one lowercase letter');
  });

  it('requires a digit', () => {
    expect(validatePassword('Abcdefghijk!')).toBe('Password must contain at least one digit');
  });

  it('requires a special character', () => {
    expect(validatePassword('Abcdefghijk1')).toBe('Password must contain at least one special character');
  });

  it.each([...'!@#$%^&*()_+-=[]{}|;:,.<>?'])('accepts the special character %s', (special) => {
    expect(validatePassword(`Abcdefghijk1${special}`)).toBeNull();
  });

  it('does not treat other characters as special', () => {
    expect(validatePassword('Abcdefghijk1~')).toBe('Password must contain at least one special character');
    expect(validatePassword('Abcdefghijk1 ')).toBe('Password must contain at least one special character');
  });

  it('accepts non-ASCII letters as upper and lower case', () => {
    expect(validatePassword('Ärgerlichkeit1!')).toBeNull();
  });

  it('reports the first violated rule', () => {
    expect(validatePassword('short')).toBe('Password must be at least 12 characters');
  });

  it('honours a custom policy', () => {
    const policy = { minLength: 6, maxLength: 8, specialChars: '#' };
    expect(validatePassword('Abc12#', policy)).toBeNull();
    expect(validatePassword('Abc12!', policy)).toBe('Password must contain at least one special character');
    expect(validatePassword('Abc12#xyz', policy)).toBe('Password must not exceed 8 characters');
  });
});

describe('passwordHelperText', () => {
  it('describes the policy', () => {
    expect(passwordHelperText(DEFAULT_PASSWORD_POLICY)).toBe(
      'At least 12 characters, including uppercase, lowercase, a digit and a special character (!@#$%^&*()_+-=[]{}|;:,.<>?)',
    );
  });
});

describe('isValidEmail', () => {
  it('accepts plain addresses and trims whitespace', () => {
    expect(isValidEmail('user@example.com')).toBe(true);
    expect(isValidEmail('  user@example.com  ')).toBe(true);
  });

  it.each(['', 'user', 'user@', '@example.com', 'user@example', 'us er@example.com'])('rejects %j', (value) => {
    expect(isValidEmail(value)).toBe(false);
  });
});
