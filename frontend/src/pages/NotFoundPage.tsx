import { Link } from 'react-router';
import { useT } from '../i18n';

export function NotFoundPage() {
  const { t } = useT();
  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-card__head">
          <h1 className="auth-card__title">{t('notFound.title')}</h1>
          <p className="auth-card__subtitle">{t('notFound.text')}</p>
        </div>
        <Link className="btn btn--primary btn--block" to="/">
          {t('notFound.back')}
        </Link>
      </div>
    </div>
  );
}
