import { useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { useMutation, useQuery } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import { ApiError } from '../api/http';
import type { PasswordPolicy } from '../api/types';
import { Button } from '../components/Button';
import { NumberField, PasswordField, SelectField, TextField } from '../components/fields';
import { useToast } from '../components/Toast';
import { isValidEmail, passwordHelperText, validatePassword } from '../lib/password';
import { queryKeys, useAppConfig, useCountries, useSubdivisions } from '../lib/queries';
import { isValidWeeklyHours } from '../lib/weeklyHours';

type FieldName =
  | 'firstName'
  | 'lastName'
  | 'countryCode'
  | 'subdivisionCode'
  | 'weeklyTargetHours'
  | 'email'
  | 'password'
  | 'confirmPassword';

interface FormValues {
  firstName: string;
  lastName: string;
  countryCode: string;
  subdivisionCode: string;
  weeklyTargetHours: number | null;
  email: string;
  password: string;
  confirmPassword: string;
}

type FieldErrors = Partial<Record<FieldName, string>>;

const EMPTY: FormValues = {
  firstName: '',
  lastName: '',
  countryCode: '',
  subdivisionCode: '',
  weeklyTargetHours: null,
  email: '',
  password: '',
  confirmPassword: '',
};

const VALIDATED_FIELDS: FieldName[] = [
  'firstName',
  'lastName',
  'weeklyTargetHours',
  'email',
  'password',
  'confirmPassword',
];

const SERVER_FIELDS: FieldName[] = [
  'email',
  'password',
  'firstName',
  'lastName',
  'weeklyTargetHours',
  'countryCode',
  'subdivisionCode',
];

function validateField(field: FieldName, values: FormValues, policy: PasswordPolicy): string | null {
  switch (field) {
    case 'firstName':
      return values.firstName.trim() ? null : 'First name is required';
    case 'lastName':
      return values.lastName.trim() ? null : 'Last name is required';
    case 'weeklyTargetHours': {
      const hours = values.weeklyTargetHours;
      if (hours === null || Number.isNaN(hours)) return 'Weekly Target Hours are required';
      return isValidWeeklyHours(hours) ? null : 'Please enter a value between 0 and 80 (one decimal place, e.g. 37.5)';
    }
    case 'email':
      if (!values.email.trim()) return 'Email is required';
      return isValidEmail(values.email) ? null : 'Please enter a valid email address';
    case 'password':
      return validatePassword(values.password, policy);
    case 'confirmPassword':
      if (!values.confirmPassword) return 'Please confirm your password';
      return values.confirmPassword === values.password ? null : 'Passwords do not match';
    default:
      return null;
  }
}

export function RegisterPage() {
  const config = useAppConfig();
  const { appName, passwordPolicy } = config;
  // useAppConfig() falls back to "disabled" while loading; only block once the real value is known.
  const configQuery = useQuery({ queryKey: queryKeys.config, queryFn: api.getConfig, staleTime: Infinity });
  const registrationDisabled = configQuery.isSuccess && !configQuery.data.registrationEnabled;

  const navigate = useNavigate();
  const toast = useToast();

  const [values, setValues] = useState<FormValues>(EMPTY);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);

  const countries = useCountries();
  const subdivisions = useSubdivisions(values.countryCode || null);

  useEffect(() => {
    document.title = `Sign Up | ${appName}`;
  }, [appName]);

  const countryOptions = useMemo(
    () => (countries.data ?? []).map((c) => ({ value: c.code, label: c.name })),
    [countries.data],
  );
  const subdivisionOptions = useMemo(
    () => (subdivisions.data ?? []).map((s) => ({ value: s.code, label: s.name })),
    [subdivisions.data],
  );
  const showSubdivisions = !!values.countryCode && subdivisionOptions.length > 0;

  const mutation = useMutation({
    mutationFn: () =>
      api.register({
        email: values.email.trim(),
        password: values.password,
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        weeklyTargetHours: values.weeklyTargetHours as number,
        countryCode: values.countryCode || null,
        subdivisionCode: showSubdivisions && values.subdivisionCode ? values.subdivisionCode : null,
      }),
    onSuccess: () => {
      toast.success('Registration successful! Please check your email to verify your account.', 5000);
      navigate('/login');
    },
    onError: (e: unknown) => {
      if (!(e instanceof ApiError)) {
        setFormError('Registration failed. Please try again.');
        return;
      }
      if (e.code === 'REGISTRATION_DISABLED') {
        setFormError('Registration is currently disabled. Please contact the administrator.');
        return;
      }
      const fieldErrors: FieldErrors = {};
      for (const field of SERVER_FIELDS) {
        const message = e.errors?.[field];
        if (message) fieldErrors[field] = message;
      }
      if (e.field && (SERVER_FIELDS as string[]).includes(e.field) && !fieldErrors[e.field as FieldName]) {
        fieldErrors[e.field as FieldName] = e.message;
      }
      if (Object.keys(fieldErrors).length > 0) {
        setErrors((current) => ({ ...current, ...fieldErrors }));
        setFormError(null);
      } else {
        setFormError(e.message);
      }
    },
  });

  function update(patch: Partial<FormValues>) {
    const next = { ...values, ...patch };
    setValues(next);
    // Re-validate fields that currently show an error so it disappears as soon as it is fixed.
    setErrors((current) => {
      const updated = { ...current };
      const touched = new Set<FieldName>(Object.keys(patch) as FieldName[]);
      if ('password' in patch && current.confirmPassword) touched.add('confirmPassword');
      for (const field of touched) {
        if (current[field]) {
          const message = validateField(field, next, passwordPolicy);
          if (message) updated[field] = message;
          else delete updated[field];
        }
      }
      return updated;
    });
  }

  function handleBlur(field: FieldName) {
    return () => {
      const message = validateField(field, values, passwordPolicy);
      setErrors((current) => {
        const updated = { ...current };
        if (message) updated[field] = message;
        else delete updated[field];
        return updated;
      });
    };
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (mutation.isPending || registrationDisabled) return;
    setFormError(null);
    const found: FieldErrors = {};
    for (const field of VALIDATED_FIELDS) {
      const message = validateField(field, values, passwordPolicy);
      if (message) found[field] = message;
    }
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    mutation.mutate();
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-card__head">
          <img className="auth-card__logo" src="/icons/icon-48x48.png" alt="" />
          <h1 className="auth-card__title">Create Account</h1>
          <p className="auth-card__subtitle">Start tracking your time today</p>
        </div>

        <form className="form" onSubmit={handleSubmit} noValidate>
          {registrationDisabled && (
            <div className="notice notice--error" role="alert">
              Registration is currently disabled. Please contact the administrator.
            </div>
          )}
          {formError && !registrationDisabled && (
            <div className="notice notice--error" role="alert">
              {formError}
            </div>
          )}

          <TextField
            label="First Name"
            name="firstName"
            autoComplete="given-name"
            required
            value={values.firstName}
            error={errors.firstName}
            onChange={(e) => update({ firstName: e.target.value })}
            onBlur={handleBlur('firstName')}
          />
          <TextField
            label="Last Name"
            name="lastName"
            autoComplete="family-name"
            required
            value={values.lastName}
            error={errors.lastName}
            onChange={(e) => update({ lastName: e.target.value })}
            onBlur={handleBlur('lastName')}
          />
          <SelectField
            label="Country"
            name="country"
            autoComplete="country"
            placeholder="Select your country (optional)"
            helperText="Used to automatically detect public holidays in your calendar"
            options={countryOptions}
            value={values.countryCode}
            error={errors.countryCode}
            onChange={(e) => update({ countryCode: e.target.value, subdivisionCode: '' })}
          />
          {showSubdivisions && (
            <SelectField
              label="State / Region"
              name="subdivision"
              placeholder="Select your state / region (optional)"
              options={subdivisionOptions}
              value={values.subdivisionCode}
              error={errors.subdivisionCode}
              onChange={(e) => update({ subdivisionCode: e.target.value })}
            />
          )}
          <NumberField
            label="Weekly Target Hours"
            required
            min={0}
            max={80}
            step={0.5}
            inputStep={0.1}
            helperText="0–80 hours per week, e.g. 37.5 (0 = no target)"
            value={values.weeklyTargetHours}
            error={errors.weeklyTargetHours}
            onChange={(hours) => update({ weeklyTargetHours: hours })}
          />
          <TextField
            label="Email"
            type="email"
            name="email"
            autoComplete="email"
            required
            value={values.email}
            error={errors.email}
            onChange={(e) => update({ email: e.target.value })}
            onBlur={handleBlur('email')}
          />
          <PasswordField
            label="Password"
            name="password"
            autoComplete="new-password"
            required
            helperText={passwordHelperText(passwordPolicy)}
            value={values.password}
            error={errors.password}
            onChange={(e) => update({ password: e.target.value })}
            onBlur={handleBlur('password')}
          />
          <PasswordField
            label="Confirm Password"
            name="confirmPassword"
            autoComplete="new-password"
            required
            value={values.confirmPassword}
            error={errors.confirmPassword}
            onChange={(e) => update({ confirmPassword: e.target.value })}
            onBlur={handleBlur('confirmPassword')}
          />

          <Button type="submit" variant="primary" block loading={mutation.isPending} disabled={registrationDisabled}>
            Create Account
          </Button>
        </form>

        <div className="auth-card__footer">
          <span>Already have an account?</span>
          <Link className="btn btn--link" to="/login">
            Sign in
          </Link>
        </div>
      </div>
    </div>
  );
}
