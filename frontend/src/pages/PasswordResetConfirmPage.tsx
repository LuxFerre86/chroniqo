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
import { translate, useT } from '../i18n';

export function PasswordResetConfirmPage() {
  const { t } = useT();
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
    document.title = t('auth.resetConfirm.docTitle', { appName });
  }, [appName, t]);

  const mutation = useMutation({
    mutationFn: () => api.confirmPasswordReset(token as string, password),
    onSuccess: () => {
      toast.success(t('auth.resetConfirm.success'));
      navigate('/login');
    },
    onError: (e: unknown) => {
      if (e instanceof ApiError && e.errors?.newPassword) {
        setPasswordError(e.errors.newPassword);
      } else {
        setError(e instanceof ApiError ? e.message : t('auth.somethingWrong'));
      }
    },
  });

  const confirmMessage = (pw: string, cf: string) => {
    if (!cf) return translate('auth.confirmPasswordRequired');
    return cf === pw ? null : translate('auth.passwordsMismatch');
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
          <h1 className="auth-card__title">{t('auth.resetConfirm.title')}</h1>
          <p className="auth-card__subtitle">{t('auth.resetConfirm.subtitle')}</p>
        </div>

        {!token ? (
          <>
            <div className="notice notice--error" role="alert">
              {t('auth.resetConfirm.invalidLink')}
            </div>
            <Link className="btn btn--tertiary btn--block" to="/login">
              {t('auth.backToLogin')}
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
              label={t('auth.resetConfirm.newPassword')}
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
              label={t('auth.confirmPassword')}
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
              {t('auth.resetConfirm.submit')}
            </Button>
          </form>
        )}
      </div>
    </div>
  );
}
