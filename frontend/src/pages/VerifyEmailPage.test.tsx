import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { StrictMode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/http';
import { VerifyEmailPage } from './VerifyEmailPage';

const mocks = vi.hoisted(() => ({ verifyEmail: vi.fn(), refreshUser: vi.fn() }));

vi.mock('../api/endpoints', () => ({
  api: {
    getConfig: vi.fn().mockResolvedValue({
      appName: 'chroniqo',
      version: null,
      registrationEnabled: true,
      passwordPolicy: { minLength: 12, maxLength: 128, specialChars: '!@#' },
    }),
    verifyEmail: mocks.verifyEmail,
  },
}));

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ refreshUser: mocks.refreshUser }),
}));

function renderPage(url: string) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <StrictMode>
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={[url]}>
          <Routes>
            <Route path="/verify-email" element={<VerifyEmailPage />} />
            <Route path="/login" element={<div>login page</div>} />
            <Route path="/" element={<div>dashboard page</div>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    </StrictMode>,
  );
}

// Every test uses its own token because the single-use guard is cached per token for the module lifetime.
describe('VerifyEmailPage', () => {
  beforeEach(() => {
    mocks.verifyEmail.mockReset();
    mocks.refreshUser.mockReset();
  });

  it('calls the API only once per token (StrictMode) and offers the dashboard when logged in', async () => {
    mocks.verifyEmail.mockResolvedValue({ status: 'VERIFIED_LOGGED_IN' });
    mocks.refreshUser.mockResolvedValue(null);
    const user = userEvent.setup();
    renderPage('/verify-email?token=t-logged-in');

    expect(screen.getByText(/Verifying/)).toBeInTheDocument();
    expect(await screen.findByText('Email Verified!')).toBeInTheDocument();
    expect(screen.getByText("Your email has been successfully verified. You're now logged in!")).toBeInTheDocument();
    expect(mocks.verifyEmail).toHaveBeenCalledTimes(1);
    expect(mocks.verifyEmail).toHaveBeenCalledWith('t-logged-in');

    await user.click(screen.getByRole('button', { name: 'Go to Dashboard' }));
    await waitFor(() => expect(mocks.refreshUser).toHaveBeenCalledTimes(1));
    expect(await screen.findByText('dashboard page')).toBeInTheDocument();
  });

  it('asks the user to log in when verification does not log in', async () => {
    mocks.verifyEmail.mockResolvedValue({ status: 'VERIFIED_LOGIN_REQUIRED' });
    const user = userEvent.setup();
    renderPage('/verify-email?token=t-login-required');

    expect(
      await screen.findByText('Your email has been successfully verified. Please log in to continue.'),
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Go to Login' }));
    expect(await screen.findByText('login page')).toBeInTheDocument();
    expect(mocks.verifyEmail).toHaveBeenCalledTimes(1);
  });

  it('shows a failure for an INVALID status', async () => {
    mocks.verifyEmail.mockResolvedValue({ status: 'INVALID' });
    renderPage('/verify-email?token=t-invalid');
    expect(await screen.findByText('Verification Failed')).toBeInTheDocument();
    expect(mocks.verifyEmail).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('button', { name: 'Request New Verification Email' })).toBeInTheDocument();
  });

  it('shows a failure when the request errors', async () => {
    mocks.verifyEmail.mockRejectedValue(new ApiError(500, 'boom'));
    renderPage('/verify-email?token=t-error');
    expect(await screen.findByText('Verification Failed')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Go to Login' })).toBeInTheDocument();
  });

  it('shows a failure without calling the API when the token is missing', async () => {
    renderPage('/verify-email');
    expect(await screen.findByText('Verification Failed')).toBeInTheDocument();
    expect(mocks.verifyEmail).not.toHaveBeenCalled();
  });
});
