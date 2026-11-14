import { useEffect } from 'react';
import { Route, Routes } from 'react-router';
import { RedirectIfAuthenticated, RequireAuth } from './auth/guards';
import { AppShell } from './layout/AppShell';
import { DashboardPage } from './pages/DashboardPage';
import { LoginPage } from './pages/LoginPage';
import { MonthPage } from './pages/MonthPage';
import { PasswordResetConfirmPage } from './pages/PasswordResetConfirmPage';
import { PasswordResetRequestPage } from './pages/PasswordResetRequestPage';
import { RegisterPage } from './pages/RegisterPage';
import { SettingsPage } from './pages/SettingsPage';
import { VerifyEmailPage } from './pages/VerifyEmailPage';
import { NotFoundPage } from './pages/NotFoundPage';

export function App() {
  // Keep the document title in sync with the routes' page titles.
  useEffect(() => {
    document.title = 'chroniqo';
  }, []);

  return (
    <Routes>
      <Route element={<RedirectIfAuthenticated />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/reset-password" element={<PasswordResetRequestPage />} />
      </Route>

      {/* Reachable regardless of login state (links from emails). */}
      <Route path="/reset-password-confirm" element={<PasswordResetConfirmPage />} />
      <Route path="/verify-email" element={<VerifyEmailPage />} />

      <Route element={<RequireAuth />}>
        <Route element={<AppShell />}>
          <Route index element={<DashboardPage />} />
          <Route path="/month" element={<MonthPage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
