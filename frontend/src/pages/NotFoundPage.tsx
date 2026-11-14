import { Link } from 'react-router';

export function NotFoundPage() {
  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-card__head">
          <h1 className="auth-card__title">Page not found</h1>
          <p className="auth-card__subtitle">The page you are looking for does not exist.</p>
        </div>
        <Link className="btn btn--primary btn--block" to="/">
          Go to Dashboard
        </Link>
      </div>
    </div>
  );
}
