import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ToastProvider } from '../components/Toast';
import { PasswordResetRequestPage } from './PasswordResetRequestPage';

const mocks = vi.hoisted(() => ({ requestPasswordReset: vi.fn() }));

vi.mock('../api/endpoints', () => ({
  api: {
    getConfig: vi.fn().mockResolvedValue({
      appName: 'chroniqo',
      version: null,
      registrationEnabled: true,
      passwordPolicy: { minLength: 12, maxLength: 128, specialChars: '!@#' },
    }),
    requestPasswordReset: mocks.requestPasswordReset,
  },
}));

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <MemoryRouter>
          <PasswordResetRequestPage />
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  );
}

describe('PasswordResetRequestPage', () => {
  beforeEach(() => {
    mocks.requestPasswordReset.mockReset();
  });

  it('validates the email before sending', async () => {
    const user = userEvent.setup();
    renderPage();
    await user.click(screen.getByRole('button', { name: 'Send Reset Link' }));
    expect(await screen.findByText('Email is required')).toBeInTheDocument();
    await user.type(screen.getByLabelText(/^Email/), 'nope');
    await user.click(screen.getByRole('button', { name: 'Send Reset Link' }));
    expect(await screen.findByText('Please enter a valid email address')).toBeInTheDocument();
    expect(mocks.requestPasswordReset).not.toHaveBeenCalled();
  });

  it('requests a reset link and shows the generic success notice', async () => {
    mocks.requestPasswordReset.mockResolvedValue(undefined);
    const user = userEvent.setup();
    renderPage();
    await user.type(screen.getByLabelText(/^Email/), 'jane@example.com');
    await user.click(screen.getByRole('button', { name: 'Send Reset Link' }));

    await waitFor(() => expect(mocks.requestPasswordReset).toHaveBeenCalledWith('jane@example.com'));
    const notices = await screen.findAllByText('If an account exists for this email, a reset link has been sent.');
    expect(notices.length).toBeGreaterThan(0);
    expect(screen.getByLabelText(/^Email/)).toHaveValue('');
    expect(screen.getByRole('link', { name: 'Back to Login' })).toHaveAttribute('href', '/login');
  });
});
