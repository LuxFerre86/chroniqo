import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/http';
import { ToastProvider } from '../components/Toast';
import { PasswordResetConfirmPage } from './PasswordResetConfirmPage';

const mocks = vi.hoisted(() => ({ confirmPasswordReset: vi.fn() }));

vi.mock('../api/endpoints', () => ({
  api: {
    getConfig: vi.fn().mockResolvedValue({
      appName: 'chroniqo',
      version: null,
      registrationEnabled: true,
      passwordPolicy: { minLength: 12, maxLength: 128, specialChars: '!@#$%^&*()_+-=[]{}|;:,.<>?' },
    }),
    confirmPasswordReset: mocks.confirmPasswordReset,
  },
}));

function renderPage(url: string) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <MemoryRouter initialEntries={[url]}>
          <Routes>
            <Route path="/reset-password-confirm" element={<PasswordResetConfirmPage />} />
            <Route path="/login" element={<div>login page</div>} />
          </Routes>
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  );
}

describe('PasswordResetConfirmPage', () => {
  beforeEach(() => {
    mocks.confirmPasswordReset.mockReset();
  });

  it('shows an invalid-link notice without a token', () => {
    renderPage('/reset-password-confirm');
    expect(screen.getByText('Invalid reset link')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to Login' })).toHaveAttribute('href', '/login');
    expect(screen.queryByLabelText(/New Password/)).not.toBeInTheDocument();
  });

  it('validates the password and confirmation', async () => {
    const user = userEvent.setup();
    renderPage('/reset-password-confirm?token=abc');
    await user.type(screen.getByLabelText(/^New Password/), 'short');
    await user.type(screen.getByLabelText(/^Confirm Password/), 'other');
    await user.click(screen.getByRole('button', { name: 'Set New Password' }));

    expect(await screen.findByText('Password must be at least 12 characters')).toBeInTheDocument();
    expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
    expect(mocks.confirmPasswordReset).not.toHaveBeenCalled();
  });

  it('confirms the reset with token and new password, then navigates to login', async () => {
    mocks.confirmPasswordReset.mockResolvedValue(undefined);
    const user = userEvent.setup();
    renderPage('/reset-password-confirm?token=abc');
    await user.type(screen.getByLabelText(/^New Password/), 'Str0ng!Passw0rd');
    await user.type(screen.getByLabelText(/^Confirm Password/), 'Str0ng!Passw0rd');
    await user.click(screen.getByRole('button', { name: 'Set New Password' }));

    await waitFor(() => expect(mocks.confirmPasswordReset).toHaveBeenCalledWith('abc', 'Str0ng!Passw0rd'));
    expect(await screen.findByText('login page')).toBeInTheDocument();
    expect(screen.getByText('Password reset successful. You can now log in.')).toBeInTheDocument();
  });

  it('shows the server detail for an invalid token', async () => {
    mocks.confirmPasswordReset.mockImplementation(() =>
      Promise.reject(new ApiError(400, 'The reset link is invalid or has expired.', 'INVALID_TOKEN')),
    );
    const user = userEvent.setup();
    renderPage('/reset-password-confirm?token=abc');
    await user.type(screen.getByLabelText(/^New Password/), 'Str0ng!Passw0rd');
    await user.type(screen.getByLabelText(/^Confirm Password/), 'Str0ng!Passw0rd');
    await user.click(screen.getByRole('button', { name: 'Set New Password' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('The reset link is invalid or has expired.');
  });
});
