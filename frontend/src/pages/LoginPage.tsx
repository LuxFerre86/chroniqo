import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router';
import { useMutation } from '@tanstack/react-query';
import { ApiError } from '../api/http';
import { useAuth } from '../auth/AuthContext';
import { Button } from '../components/Button';
import { Checkbox, PasswordField, TextField } from '../components/fields';
import { useToast } from '../components/Toast';
import { useAppConfig } from '../lib/queries';

const DEFAULT_LOGIN_ERROR = 'Incorrect email or password, or the account is locked or not verified.';

export function LoginPage() {
  const { login } = useAuth();
  const { appName } = useAppConfig();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();
  const [searchParams, setSearchParams] = useSearchParams();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    document.title = `Login | ${appName}`;
  }, [appName]);

  const accountDeleted = searchParams.has('accountDeleted');
  useEffect(() => {
    if (!accountDeleted) return;
    toast.success('Your account was deleted successfully.', 4000);
    const next = new URLSearchParams(searchParams);
    next.delete('accountDeleted');
    setSearchParams(next, { replace: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accountDeleted]);

  const mutation = useMutation({
    mutationFn: () => login(email.trim(), password, rememberMe),
    onSuccess: () => {
      const from = (location.state as { from?: string } | null)?.from;
      navigate(from || '/', { replace: true });
    },
    onError: (e: unknown) => {
      const hasDetail = e instanceof ApiError && !e.message.startsWith('Request failed (');
      setError(hasDetail ? e.message : DEFAULT_LOGIN_ERROR);
    },
  });

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (mutation.isPending) return;
    setError(null);
    mutation.mutate();
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-card__head">
          <img className="auth-card__logo" src="/icons/icon-48x48.png" alt="" />
          <h1 className="auth-card__title">{appName}</h1>
          <p className="auth-card__subtitle">Track your time with ease</p>
        </div>

        <form className="form" onSubmit={handleSubmit} noValidate>
          {error && (
            <div className="notice notice--error" role="alert">
              {error}
            </div>
          )}
          <TextField
            label="Email"
            type="email"
            name="email"
            autoComplete="username"
            required
            autoFocus
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <PasswordField
            label="Password"
            name="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <Checkbox label="Remember me" checked={rememberMe} onChange={(e) => setRememberMe(e.target.checked)} />
          <Button type="submit" variant="primary" block loading={mutation.isPending}>
            Log in
          </Button>
        </form>

        <Button variant="link" onClick={() => navigate('/reset-password')}>
          Forgot password
        </Button>

        <div className="auth-card__footer">
          <span>Don't have an account?</span>
          <Link className="btn btn--link" to="/register">
            Sign up
          </Link>
        </div>
      </div>
    </div>
  );
}
