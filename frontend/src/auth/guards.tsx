import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuth } from './AuthContext';

/** Renders the nested routes only for signed-in users; everyone else is sent to the login page. */
export function RequireAuth() {
  const { user, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="centered-state" role="status">
        <span className="spinner" aria-hidden="true" /> Loading…
      </div>
    );
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <Outlet />;
}

/** Renders public pages (login, register…) only for anonymous users; signed-in users go to the dashboard. */
export function RedirectIfAuthenticated() {
  const { user, isLoading } = useAuth();
  if (isLoading) {
    return (
      <div className="centered-state" role="status">
        <span className="spinner" aria-hidden="true" /> Loading…
      </div>
    );
  }
  if (user) {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}
