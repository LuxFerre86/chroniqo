import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { api } from '../api/endpoints';
import type { VerifyEmailStatus } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { Button } from '../components/Button';
import { useAppConfig } from '../lib/queries';
import { useT } from '../i18n';

/**
 * Verification tokens are single-use, so the request must be sent exactly once per token even when
 * React StrictMode mounts the effect twice. The in-flight/settled promise is shared per token.
 */
const verifications = new Map<string, Promise<VerifyEmailStatus>>();

function verifyOnce(token: string): Promise<VerifyEmailStatus> {
  let pending = verifications.get(token);
  if (!pending) {
    pending = api
      .verifyEmail(token)
      .then((result) => result.status)
      .catch((): VerifyEmailStatus => 'INVALID');
    verifications.set(token, pending);
  }
  return pending;
}

export function VerifyEmailPage() {
  const { t } = useT();
  const { appName } = useAppConfig();
  const { refreshUser } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [result, setResult] = useState<{ token: string; status: VerifyEmailStatus } | null>(null);
  const [continuing, setContinuing] = useState(false);

  useEffect(() => {
    document.title = t('auth.verify.docTitle', { appName });
  }, [appName, t]);

  useEffect(() => {
    if (!token) return;
    let cancelled = false;
    verifyOnce(token).then((status) => {
      if (!cancelled) setResult({ token, status });
    });
    return () => {
      cancelled = true;
    };
  }, [token]);

  const status: VerifyEmailStatus | null = !token ? 'INVALID' : result?.token === token ? result.status : null;

  async function goToDashboard() {
    setContinuing(true);
    try {
      await refreshUser();
    } finally {
      navigate('/', { replace: true });
    }
  }

  if (status === null) {
    return (
      <div className="auth-page">
        <div className="centered-state" role="status">
          <span className="spinner" aria-hidden="true" /> {t('auth.verify.verifying')}
        </div>
      </div>
    );
  }

  const verified = status !== 'INVALID';
  const loggedIn = status === 'VERIFIED_LOGGED_IN';

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-card__head">
          <div className="auth-card__emoji" aria-hidden="true">
            {verified ? '✅' : '❌'}
          </div>
          <h1 className="auth-card__title">{verified ? t('auth.verify.successTitle') : t('auth.verify.failedTitle')}</h1>
        </div>
        <p className="auth-card__message">
          {!verified
            ? t('auth.verify.invalid')
            : loggedIn
              ? t('auth.verify.successLoggedIn')
              : t('auth.verify.successLogin')}
        </p>
        {loggedIn ? (
          <Button variant="primary" block loading={continuing} onClick={goToDashboard}>
            {t('auth.verify.goToDashboard')}
          </Button>
        ) : (
          <>
            {!verified && (
              <Button variant="primary" block onClick={() => navigate('/resend-verification')}>
                {t('auth.verify.requestNew')}
              </Button>
            )}
            <Button variant={verified ? 'primary' : 'tertiary'} block onClick={() => navigate('/login')}>
              {t('auth.verify.goToLogin')}
            </Button>
          </>
        )}
      </div>
    </div>
  );
}
