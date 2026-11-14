import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/http';
import { ToastProvider } from '../components/Toast';
import { LoginPage } from './LoginPage';

const login = vi.fn();

vi.mock('../api/endpoints', () => ({
  api: {
    getConfig: vi.fn().mockResolvedValue({
      appName: 'chroniqo',
      version: null,
      registrationEnabled: true,
      passwordPolicy: { minLength: 12, maxLength: 128, specialChars: '!@#' },
    }),
  },
}));

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ login }),
}));

function Where() {
  const location = useLocation();
  return <div data-testid="where">{location.pathname}</div>;
}

function renderLogin(initialEntry: string | { pathname: string; state?: unknown } = '/login') {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <MemoryRouter initialEntries={[initialEntry]}>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="*" element={<Where />} />
          </Routes>
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    login.mockReset();
  });

  it('logs in with the entered credentials and navigates to the dashboard', async () => {
    login.mockResolvedValue(undefined);
    const user = userEvent.setup();
    renderLogin();

    await user.type(screen.getByLabelText(/^Email/), 'jane@example.com');
    await user.type(screen.getByLabelText(/^Password/), 'secret');
    await user.click(screen.getByLabelText('Remember me'));
    await user.click(screen.getByRole('button', { name: 'Log in' }));

    await waitFor(() => expect(login).toHaveBeenCalledWith('jane@example.com', 'secret', true));
    expect(await screen.findByTestId('where')).toHaveTextContent('/');
  });

  it('returns to the originally requested page after login', async () => {
    login.mockResolvedValue(undefined);
    const user = userEvent.setup();
    renderLogin({ pathname: '/login', state: { from: '/month' } });

    await user.type(screen.getByLabelText(/^Email/), 'a@b.co');
    await user.type(screen.getByLabelText(/^Password/), 'x');
    await user.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByTestId('where')).toHaveTextContent('/month');
  });

  it('shows an error notice when login fails', async () => {
    login.mockRejectedValue(new ApiError(401, 'Request failed (401)', 'LOGIN_FAILED'));
    const user = userEvent.setup();
    renderLogin();

    await user.type(screen.getByLabelText(/^Email/), 'a@b.co');
    await user.type(screen.getByLabelText(/^Password/), 'wrong');
    await user.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Incorrect email or password, or the account is locked or not verified.',
    );
    expect(screen.queryByTestId('where')).not.toBeInTheDocument();
  });

  it('shows a toast after an account was deleted', async () => {
    renderLogin('/login?accountDeleted');
    expect(await screen.findByText('Your account was deleted successfully.')).toBeInTheDocument();
  });

  it('links to the reset-password and register pages', async () => {
    const user = userEvent.setup();
    renderLogin();
    expect(screen.getByRole('link', { name: 'Sign up' })).toHaveAttribute('href', '/register');
    await user.click(screen.getByRole('button', { name: 'Forgot password' }));
    expect(await screen.findByTestId('where')).toHaveTextContent('/reset-password');
  });
});
