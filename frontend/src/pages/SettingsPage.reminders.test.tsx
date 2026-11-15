import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { UserProfile } from '../api/types';
import { ToastProvider } from '../components/Toast';
import { SettingsPage } from './SettingsPage';

let currentUser: UserProfile;

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ user: currentUser, refreshUser: vi.fn(), clearSession: vi.fn() }),
}));

vi.mock('../api/endpoints', () => ({
  api: {
    getConfig: vi.fn(),
    getCountries: vi.fn(),
    getSubdivisions: vi.fn(),
    getVacationAccount: vi.fn(),
    getCustomHolidays: vi.fn(),
    getReminderSettings: vi.fn(),
    updateReminderSettings: vi.fn(),
    subscribePush: vi.fn(),
    unsubscribePush: vi.fn(),
  },
}));

import { api } from '../api/endpoints';

const mocked = vi.mocked(api);

const baseConfig = {
  appName: 'chroniqo',
  version: '2026.10.0',
  registrationEnabled: true,
  passwordPolicy: { minLength: 12, maxLength: 128, specialChars: '!@#' },
};

const subscriptionJson = { endpoint: 'https://push.example/abc', keys: { p256dh: 'pk', auth: 'ak' } };

let existingSubscription: unknown = null;
const subscribe = vi.fn();

function stubPushSupport(permission: NotificationPermission = 'granted') {
  const registration = {
    pushManager: {
      getSubscription: vi.fn(async () => existingSubscription),
      subscribe,
    },
  };
  Object.defineProperty(navigator, 'serviceWorker', {
    configurable: true,
    value: { ready: Promise.resolve(registration) },
  });
  vi.stubGlobal('PushManager', class {});
  vi.stubGlobal('Notification', { requestPermission: vi.fn(async () => permission) });
}

function makeSubscription() {
  return {
    endpoint: subscriptionJson.endpoint,
    toJSON: () => subscriptionJson,
    unsubscribe: vi.fn(async () => true),
  };
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <SettingsPage />
      </ToastProvider>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  existingSubscription = null;
  currentUser = {
    email: 'ada@example.com',
    firstName: 'Ada',
    lastName: 'Lovelace',
    weeklyTargetHours: 38,
    workingDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'],
    firstDayOfWeek: 'MONDAY',
    countryCode: 'DE',
    subdivisionCode: 'HH',
    createdAt: '2025-03-04T10:15:30Z',
    lastLoginAt: null,
  };
  mocked.getCountries.mockResolvedValue([{ code: 'DE', name: 'Germany' }]);
  mocked.getSubdivisions.mockResolvedValue([]);
  mocked.getCustomHolidays.mockResolvedValue([]);
  mocked.getReminderSettings.mockResolvedValue({
    startEnabled: true,
    startTime: '09:00:00',
    endEnabled: true,
    endAfterHours: 10,
  });
  mocked.subscribePush.mockResolvedValue(undefined);
  mocked.unsubscribePush.mockResolvedValue(undefined);
  mocked.updateReminderSettings.mockImplementation(async (s) => s);
});

afterEach(() => {
  vi.unstubAllGlobals();
  Reflect.deleteProperty(navigator, 'serviceWorker');
});

describe('SettingsPage – reminders', () => {
  it('is hidden when the server has no push key', async () => {
    stubPushSupport();
    mocked.getConfig.mockResolvedValue({ ...baseConfig, pushPublicKey: null });
    renderPage();

    await screen.findByText('Custom Days Off');
    expect(screen.queryByRole('heading', { name: 'Reminders' })).not.toBeInTheDocument();
  });

  it('is hidden when the browser lacks push support', async () => {
    mocked.getConfig.mockResolvedValue({ ...baseConfig, pushPublicKey: 'BKey' });
    renderPage();

    await screen.findByText('Custom Days Off');
    expect(screen.queryByRole('heading', { name: 'Reminders' })).not.toBeInTheDocument();
  });

  it('enables reminders on this device and stores the subscription', async () => {
    stubPushSupport();
    subscribe.mockResolvedValue(makeSubscription());
    mocked.getConfig.mockResolvedValue({ ...baseConfig, pushPublicKey: 'BKey' });
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Enable on this device' }));

    await waitFor(() =>
      expect(mocked.subscribePush).toHaveBeenCalledWith({
        endpoint: 'https://push.example/abc',
        p256dh: 'pk',
        auth: 'ak',
      }),
    );
    expect(await screen.findByRole('button', { name: 'Disable on this device' })).toBeInTheDocument();
  });

  it('does not subscribe when the permission is denied', async () => {
    stubPushSupport('denied');
    mocked.getConfig.mockResolvedValue({ ...baseConfig, pushPublicKey: 'BKey' });
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Enable on this device' }));

    expect(await screen.findByText(/Notifications are blocked/)).toBeInTheDocument();
    expect(subscribe).not.toHaveBeenCalled();
    expect(mocked.subscribePush).not.toHaveBeenCalled();
  });

  it('disables reminders on this device', async () => {
    const existing = makeSubscription();
    existingSubscription = existing;
    stubPushSupport();
    mocked.getConfig.mockResolvedValue({ ...baseConfig, pushPublicKey: 'BKey' });
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Disable on this device' }));

    await waitFor(() => expect(mocked.unsubscribePush).toHaveBeenCalledWith('https://push.example/abc'));
    expect(existing.unsubscribe).toHaveBeenCalled();
    expect(await screen.findByRole('button', { name: 'Enable on this device' })).toBeInTheDocument();
  });

  it('loads and saves the schedule', async () => {
    stubPushSupport();
    mocked.getConfig.mockResolvedValue({ ...baseConfig, pushPublicKey: 'BKey' });
    renderPage();

    const time = await screen.findByLabelText('Start reminder time');
    await waitFor(() => expect(time).toHaveValue('09:00'));
    const hours = screen.getByLabelText('Running entry reminder after (hours)');
    expect(hours).toHaveValue(10);

    await userEvent.clear(hours);
    await userEvent.type(hours, '8');
    await userEvent.click(screen.getByRole('button', { name: 'Save Reminders' }));

    await waitFor(() =>
      expect(mocked.updateReminderSettings).toHaveBeenCalledWith({
        startEnabled: true,
        startTime: '09:00',
        endEnabled: true,
        endAfterHours: 8,
      }),
    );
  });

  it('rejects an out-of-range duration', async () => {
    stubPushSupport();
    mocked.getConfig.mockResolvedValue({ ...baseConfig, pushPublicKey: 'BKey' });
    renderPage();

    const hours = await screen.findByLabelText('Running entry reminder after (hours)');
    await userEvent.clear(hours);
    await userEvent.type(hours, '30');
    await userEvent.click(screen.getByRole('button', { name: 'Save Reminders' }));

    expect(await screen.findByText(/between 1 and 24 hours/)).toBeInTheDocument();
    expect(mocked.updateReminderSettings).not.toHaveBeenCalled();
  });
});
