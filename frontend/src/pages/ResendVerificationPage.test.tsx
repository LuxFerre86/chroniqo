import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ToastProvider } from '../components/Toast';
import { ResendVerificationPage } from './ResendVerificationPage';

const mocks = vi.hoisted(() => ({ resendVerificationEmail: vi.fn() }));

vi.mock('../api/endpoints', () => ({
  api: {
    getConfig: vi.fn().mockResolvedValue({
      appName: 'chroniqo',
      version: null,
      registrationEnabled: true,
      passwordPolicy: { minLength: 12, maxLength: 128, specialChars: '!@#' },
    }),
    resendVerificationEmail: mocks.resendVerificationEmail,
  },
}));

function renderPage(url = '/resend-verification') {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <MemoryRouter initialEntries={[url]}>
          <ResendVerificationPage />
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  );
}

describe('ResendVerificationPage', () => {
  beforeEach(() => {
    mocks.resendVerificationEmail.mockReset();
  });

  it('validates the email before sending', async () => {
    const user = userEvent.setup();
    renderPage();
    await user.click(screen.getByRole('button', { name: 'Send Verification Email' }));
    expect(await screen.findByText('Email is required')).toBeInTheDocument();
    await user.type(screen.getByLabelText(/^Email/), 'nope');
    await user.click(screen.getByRole('button', { name: 'Send Verification Email' }));
    expect(await screen.findByText('Please enter a valid email address')).toBeInTheDocument();
    expect(mocks.resendVerificationEmail).not.toHaveBeenCalled();
  });

  it('sends the request and shows the generic success notice', async () => {
    mocks.resendVerificationEmail.mockResolvedValue(undefined);
    const user = userEvent.setup();
    renderPage();
    await user.type(screen.getByLabelText(/^Email/), 'jane@example.com');
    await user.click(screen.getByRole('button', { name: 'Send Verification Email' }));

    await waitFor(() => expect(mocks.resendVerificationEmail).toHaveBeenCalledWith('jane@example.com'));
    const notices = await screen.findAllByText(/a new verification email has been sent/);
    expect(notices.length).toBeGreaterThan(0);
  });

  it('prefills the email from the query string', () => {
    renderPage('/resend-verification?email=jane%40example.com');
    expect(screen.getByLabelText(/^Email/)).toHaveValue('jane@example.com');
  });
});
