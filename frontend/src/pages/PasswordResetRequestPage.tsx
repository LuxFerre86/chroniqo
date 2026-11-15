import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router';
import { useMutation } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import { ApiError } from '../api/http';
import { Button } from '../components/Button';
import { TextField } from '../components/fields';
import { isValidEmail } from '../lib/password';
import { useAppConfig } from '../lib/queries';

function validateEmail(value: string): string | null {
  if (!value.trim()) return 'Email is required';
  return isValidEmail(value) ? null : 'Please enter a valid email address';
}

export function PasswordResetRequestPage() {
  const { appName } = useAppConfig();
  const [email, setEmail] = useState('');
  const [emailError, setEmailError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);

  useEffect(() => {
    document.title = `Reset Password | ${appName}`;
  }, [appName]);

  const mutation = useMutation({
    mutationFn: () => api.requestPasswordReset(email.trim()),
    onSuccess: () => {
      setEmail('');
      setSent(true);
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
            🔑
          </div>
          <h1 className="auth-card__title">Reset Password</h1>
          <p className="auth-card__subtitle">Enter your email and we'll send you a reset link</p>
        </div>

        <form className="form" onSubmit={handleSubmit} noValidate>
          {sent && (
            <div className="notice notice--success" role="status">
              If an account exists for this email, a reset link has been sent.
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
            Send Reset Link
          </Button>
          <Link className="btn btn--tertiary btn--block" to="/login">
            Back to Login
          </Link>
        </form>
      </div>
    </div>
  );
}
