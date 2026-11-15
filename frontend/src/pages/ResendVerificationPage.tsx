import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router';
import { useMutation } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import { ApiError } from '../api/http';
import { Button } from '../components/Button';
import { TextField } from '../components/fields';
import { useToast } from '../components/Toast';
import { isValidEmail } from '../lib/password';
import { useAppConfig } from '../lib/queries';

const SENT_MESSAGE = 'If this account exists and is not yet verified, a new verification email has been sent.';

function validateEmail(value: string): string | null {
  if (!value.trim()) return 'Email is required';
  return isValidEmail(value) ? null : 'Please enter a valid email address';
}

export function ResendVerificationPage() {
  const { appName } = useAppConfig();
  const toast = useToast();
  const [searchParams] = useSearchParams();
  const [email, setEmail] = useState(searchParams.get('email') ?? '');
  const [emailError, setEmailError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);

  useEffect(() => {
    document.title = `Resend Verification | ${appName}`;
  }, [appName]);

  const mutation = useMutation({
    mutationFn: () => api.resendVerificationEmail(email.trim()),
    onSuccess: () => {
      setSent(true);
      toast.success(SENT_MESSAGE);
    },
    onError: (e: unknown) => {
      setError(e instanceof ApiError ? e.message : 'Something went wrong. Please try again.');
    },
  });

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (mutation.isPending) return;
    setError(null);
    setSent(false);
    const message = validateEmail(email);
    setEmailError(message);
    if (message) return;
    mutation.mutate();
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-card__head">
          <div className="auth-card__emoji" aria-hidden="true">
            ✉️
          </div>
          <h1 className="auth-card__title">Resend Verification Email</h1>
          <p className="auth-card__subtitle">Enter your email and we'll send you a new verification link</p>
        </div>

        <form className="form" onSubmit={handleSubmit} noValidate>
          {sent && (
            <div className="notice notice--success" role="status">
              {SENT_MESSAGE}
            </div>
          )}
          {error && (
            <div className="notice notice--error" role="alert">
              {error}
            </div>
          )}
          <TextField
            label="Email"
            type="email"
            name="email"
            autoComplete="email"
            placeholder="your@email.com"
            required
            value={email}
            error={emailError}
            onChange={(e) => {
              setEmail(e.target.value);
              if (emailError) setEmailError(validateEmail(e.target.value));
            }}
            onBlur={() => email && setEmailError(validateEmail(email))}
          />
          <Button type="submit" variant="primary" block loading={mutation.isPending}>
            Send Verification Email
          </Button>
          <Link className="btn btn--tertiary btn--block" to="/login">
            Back to Login
          </Link>
        </form>
      </div>
    </div>
  );
}
