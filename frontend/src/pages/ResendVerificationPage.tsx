import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router';
import { useMutation } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import { ApiError } from '../api/http';
import { Button } from '../components/Button';
import { TextField } from '../components/fields';
import { isValidEmail } from '../lib/password';
import { useAppConfig } from '../lib/queries';
import { translate, useT } from '../i18n';

function validateEmail(value: string): string | null {
  if (!value.trim()) return translate('auth.emailRequired');
  return isValidEmail(value) ? null : translate('auth.emailInvalid');
}

export function ResendVerificationPage() {
  const { t } = useT();
  const { appName } = useAppConfig();
  const [searchParams] = useSearchParams();
  const [email, setEmail] = useState(searchParams.get('email') ?? '');
  const [emailError, setEmailError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);

  useEffect(() => {
    document.title = t('auth.resend.docTitle', { appName });
  }, [appName, t]);

  const mutation = useMutation({
    mutationFn: () => api.resendVerificationEmail(email.trim()),
    onSuccess: () => {
      setSent(true);
    },
    onError: (e: unknown) => {
      setError(e instanceof ApiError ? e.message : t('auth.somethingWrong'));
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
          <h1 className="auth-card__title">{t('auth.resend.title')}</h1>
          <p className="auth-card__subtitle">{t('auth.resend.subtitle')}</p>
        </div>

        <form className="form" onSubmit={handleSubmit} noValidate>
          {sent && (
            <div className="notice notice--success" role="status">
              {t('auth.resend.sent')}
            </div>
          )}
          {error && (
            <div className="notice notice--error" role="alert">
              {error}
            </div>
          )}
          <TextField
            label={t('auth.email')}
            type="email"
            name="email"
            autoComplete="email"
            placeholder={t('auth.emailPlaceholder')}
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
            {t('auth.resend.submit')}
          </Button>
          <Link className="btn btn--tertiary btn--block" to="/login">
            {t('auth.backToLogin')}
          </Link>
        </form>
      </div>
    </div>
  );
}
