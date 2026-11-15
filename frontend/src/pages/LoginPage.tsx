import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router';
import { useMutation } from '@tanstack/react-query';
import { ApiError } from '../api/http';
import { useAuth } from '../auth/AuthContext';
import { Button } from '../components/Button';
import { Checkbox, PasswordField, TextField } from '../components/fields';
import { useToast } from '../components/Toast';
import { useT } from '../i18n';
import { useAppConfig } from '../lib/queries';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function LoginPage() {
  const { t } = useT();
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
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({});
  useEffect(() => {
    document.title = t('auth.login.docTitle', { appName });
  }, [appName, t]);

  const accountDeleted = searchParams.has('accountDeleted');
  useEffect(() => {
    if (!accountDeleted) return;
    toast.success(t('auth.login.accountDeleted'), 4000);
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
      const hasDetail = e instanceof ApiError && e.message !== t('error.requestFailed', { status: e.status });
      setError(hasDetail ? e.message : t('auth.login.defaultError'));
    },
  });

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (mutation.isPending) return;
    setError(null);
    const nextErrors: { email?: string; password?: string } = {};
    if (!email.trim()) nextErrors.email = t('auth.login.emailRequired');
    else if (!EMAIL_PATTERN.test(email.trim())) nextErrors.email = t('auth.emailInvalid');
    if (!password) nextErrors.password = t('auth.login.passwordRequired');
    setFieldErrors(nextErrors);
    if (nextErrors.email || nextErrors.password) return;
    mutation.mutate();
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-card__head">
          <img className="auth-card__logo" src="/icons/icon-48x48.png" alt="" />
          <h1 className="auth-card__title">{appName}</h1>
          <p className="auth-card__subtitle">{t('auth.login.subtitle')}</p>
        </div>

        <form className="form" onSubmit={handleSubmit} noValidate>
          {error && (
            <div className="notice notice--error" role="alert">
              {error}
            </div>
          )}
          <TextField
            label={t('auth.email')}
            type="email"
            name="email"
            autoComplete="username"
            required
            autoFocus
            value={email}
            error={fieldErrors.email}
            onChange={(e) => {
              setEmail(e.target.value);
              setFieldErrors((c) => ({ ...c, email: undefined }));
            }}
          />
          <PasswordField
            label={t('auth.password')}
            name="password"
            autoComplete="current-password"
            required
            value={password}
            error={fieldErrors.password}
            onChange={(e) => {
              setPassword(e.target.value);
              setFieldErrors((c) => ({ ...c, password: undefined }));
            }}
          />
          <Checkbox label={t('auth.login.rememberMe')} checked={rememberMe} onChange={(e) => setRememberMe(e.target.checked)} />
          <Button type="submit" variant="primary" block loading={mutation.isPending}>
            {t('auth.login.submit')}
          </Button>
        </form>

        <Button variant="link" onClick={() => navigate('/reset-password')}>
          {t('auth.login.forgotPassword')}
        </Button>
        <Button
          variant="link"
          onClick={() => navigate(email.trim() ? `/resend-verification?email=${encodeURIComponent(email.trim())}` : '/resend-verification')}
        >
          {t('auth.login.resendVerification')}
        </Button>

        <div className="auth-card__footer">
          <span>{t('auth.login.noAccount')}</span>
          <Link className="btn btn--link" to="/register">
            {t('auth.login.signUp')}
          </Link>
        </div>
      </div>
    </div>
  );
}
