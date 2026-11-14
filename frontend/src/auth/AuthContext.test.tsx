import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ApiError } from '../api/http';
import type { UserProfile } from '../api/types';
import { AuthProvider, useAuth } from './AuthContext';

vi.mock('../api/endpoints', () => ({
  api: {
    getMe: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
  },
}));

import { api } from '../api/endpoints';

const getMe = vi.mocked(api.getMe);
const login = vi.mocked(api.login);
const logout = vi.mocked(api.logout);

const profile: UserProfile = {
  email: 'ada@example.com',
  firstName: 'Ada',
  lastName: 'Lovelace',
  weeklyTargetHours: 40,
  workingDays: ['MONDAY', 'TUESDAY'],
  countryCode: null,
  subdivisionCode: null,
  createdAt: null,
  lastLoginAt: null,
};

function Probe() {
  const { user, isLoading, login: doLogin, logout: doLogout } = useAuth();
  const state = isLoading ? 'loading' : user ? `user:${user.email}` : 'anonymous';
  return (
    <div>
      <span data-testid="state">{state}</span>
      <button onClick={() => doLogin('ada@example.com', 'secret', true).catch(() => {})}>login</button>
      <button onClick={() => doLogout().catch(() => {})}>logout</button>
    </div>
  );
}

function renderProbe() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: Infinity } } });
  render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <Probe />
      </AuthProvider>
    </QueryClientProvider>,
  );
  return queryClient;
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('AuthProvider', () => {
  it('exposes the signed-in user', async () => {
    getMe.mockResolvedValue(profile);
    renderProbe();

    expect(screen.getByTestId('state')).toHaveTextContent('loading');
    await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent('user:ada@example.com'));
  });

  it('treats a 401 from getMe as anonymous', async () => {
    getMe.mockRejectedValue(new ApiError(401, 'Unauthorized'));
    renderProbe();

    await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent('anonymous'));
  });

  it('login calls the API and refreshes the user', async () => {
    getMe.mockRejectedValueOnce(new ApiError(401, 'Unauthorized'));
    login.mockResolvedValue(undefined);
    renderProbe();
    await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent('anonymous'));

    getMe.mockResolvedValue(profile);
    await userEvent.click(screen.getByRole('button', { name: 'login' }));

    await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent('user:ada@example.com'));
    expect(login).toHaveBeenCalledWith('ada@example.com', 'secret', true);
    expect(getMe).toHaveBeenCalledTimes(2);
  });

  it('keeps the user anonymous when login fails', async () => {
    getMe.mockRejectedValue(new ApiError(401, 'Unauthorized'));
    login.mockRejectedValue(new ApiError(401, 'Bad credentials'));
    renderProbe();
    await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent('anonymous'));

    await userEvent.click(screen.getByRole('button', { name: 'login' }));

    await waitFor(() => expect(login).toHaveBeenCalled());
    expect(screen.getByTestId('state')).toHaveTextContent('anonymous');
    expect(getMe).toHaveBeenCalledTimes(1);
  });

  it('logout calls the API and clears the user and cached data', async () => {
    getMe.mockResolvedValue(profile);
    logout.mockResolvedValue(undefined);
    const queryClient = renderProbe();
    await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent('user:ada@example.com'));
    queryClient.setQueryData(['dashboard'], { secret: true });
    queryClient.setQueryData(['countries'], [{ code: 'DE', name: 'Germany' }]);

    await userEvent.click(screen.getByRole('button', { name: 'logout' }));

    await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent('anonymous'));
    expect(logout).toHaveBeenCalledTimes(1);
    expect(queryClient.getQueryData(['dashboard'])).toBeUndefined();
    expect(queryClient.getQueryData(['countries'])).toBeDefined();
  });

  it('logout clears the session even when the API call fails', async () => {
    getMe.mockResolvedValue(profile);
    logout.mockRejectedValue(new ApiError(0, 'offline', 'NETWORK'));
    renderProbe();
    await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent('user:ada@example.com'));

    await userEvent.click(screen.getByRole('button', { name: 'logout' }));

    await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent('anonymous'));
  });
});

describe('useAuth', () => {
  it('throws outside of an AuthProvider', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<Probe />)).toThrow('useAuth must be used within an AuthProvider');
    spy.mockRestore();
  });
});
