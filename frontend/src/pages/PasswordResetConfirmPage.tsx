import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { useMutation } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import { ApiError } from '../api/http';
import { Button } from '../components/Button';
import { PasswordField } from '../components/fields';
import { useToast } from '../components/Toast';
import { passwordHelperText, validatePassword } from '../lib/password';
import { useAppConfig } from '../lib/queries';

export function PasswordResetConfirmPage() {
  const { appName, passwordPolicy } = useAppConfig();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();
  const toast = useToast();

  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [confirmError, setConfirmError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    document.title = `Set New Password | ${appName}`;
  }, [appName]);

  const mutation = useMutation({
    mutationFn: () => api.confirmPasswordReset(token as string, password),
    onSuccess: () => {
      toast.success('Password reset successful. You can now log in.');
      navigate('/login');
    },
    onError: (e: unknown) => {
      if (e instanceof ApiError && e.errors?.newPassword) {
        setPasswordError(e.errors.newPassword);
      } else {
        setError(e instanceof ApiError ? e.message : 'Something went wrong. Please try again.');
      }
    },
  });

  const confirmMessage = (pw: string, cf: string) => {
    if (!cf) return 'Please confirm your password';
    return cf === pw ? null : 'Passwords do not match';
  };

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (mutation.isPending) return;
    setError(null);
    const pwMessage = validatePassword(password, passwordPolicy);
    const cfMessage = confirmMessage(password, confirm);
    setPasswordError(pwMessage);
    setConfirmError(cfMessage);
    if (pwMessage || cfMessage) return;
    mutation.mutate();
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-card__head">
          <div className="auth-card__emoji" aria-hidden="true">
            🔐
          </div>
          <h1 className="auth-card__title">Set New Password</h1>
          <p className="auth-card__subtitle">Choose a strong password</p>
        </div>

        {!token ? (
          <>
            <div className="notice notice--error" role="alert">
              Invalid reset link
            </div>
            <Link className="btn btn--tertiary btn--block" to="/login">
              Back to Login
            </Link>
          </>
        ) : (
          <form className="form" onSubmit={handleSubmit} noValidate>
            {error && (
              <div className="notice notice--error" role="alert">
                {error}
              </div>
            )}
            <PasswordField
              label="New Password"
              name="newPassword"
              autoComplete="new-password"
              required
              helperText={passwordHelperText(passwordPolicy)}
              value={password}
              error={passwordError}
              onChange={(e) => {
                setPassword(e.target.value);
                if (passwordError) setPasswordError(validatePassword(e.target.value, passwordPolicy));
                if (confirmError) setConfirmError(confirmMessage(e.target.value, confirm));
              }}
              onBlur={() => password && setPasswordError(validatePassword(password, passwordPolicy))}
            />
            <PasswordField
              label="Confirm Password"
              name="confirmPassword"
              autoComplete="new-password"
              required
              value={confirm}
              error={confirmError}
              onChange={(e) => {
                setConfirm(e.target.value);
                if (confirmError) setConfirmError(confirmMessage(password, e.target.value));
              }}
              onBlur={() => confirm && setConfirmError(confirmMessage(password, confirm))}
            />
            <Button type="submit" variant="primary" block loading={mutation.isPending}>
              Set New Password
            </Button>
          </form>
        )}
      </div>
    </div>
  );
}
