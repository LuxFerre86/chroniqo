import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '../api/endpoints';
import type { Dashboard, DaySummary } from '../api/types';
import { ToastProvider } from '../components/Toast';
import { DashboardPage } from './DashboardPage';
import { setActiveLanguage } from '../i18n';

vi.mock('../api/endpoints', () => ({
  api: {
    getDashboard: vi.fn(),
    getTimeEntries: vi.fn(),
    getAbsences: vi.fn(),
    createTimeEntry: vi.fn(),
    updateTimeEntry: vi.fn(),
    deleteTimeEntry: vi.fn(),
    saveAbsence: vi.fn(),
    deleteAbsences: vi.fn(),
    getVacationAccount: vi.fn(),
  },
}));

const day = (date: string, workedMinutes: number): DaySummary => ({
  date,
  workday: true,
  workedMinutes,
  targetMinutes: 468,
  balanceMinutes: workedMinutes - 468,
  absenceType: null,
  absenceLabel: null,
});

function dashboard(overrides: Partial<Dashboard> = {}): Dashboard {
  return {
    today: { ...day('2026-11-14', 468), balanceMinutes: 0 },
    week: [
      day('2026-11-09', 490),
      day('2026-11-10', 500),
      day('2026-11-11', 430),
      day('2026-11-12', 410),
      day('2026-11-13', 535),
      day('2026-11-14', 0),
      day('2026-11-15', 0),
    ],
    currentBalanceMinutes: 1041,
    weeklyProgress: { workedMinutes: 2365, targetMinutes: 2340, percentage: 101, hasTarget: true },
    ...overrides,
  };
}

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <ToastProvider>
          <Routes>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/month" element={<div>Month route</div>} />
          </Routes>
        </ToastProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 10, 14, 12, 0, 0));
    vi.mocked(api.getTimeEntries).mockResolvedValue([]);
    vi.mocked(api.getAbsences).mockResolvedValue([]);
    vi.mocked(api.getVacationAccount).mockResolvedValue({
      year: 2026,
      entitlement: 30,
      taken: 10,
      planned: 5,
      remaining: 15,
    });
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
    setActiveLanguage('en');
  });

  it('renders the dashboard in German', async () => {
    setActiveLanguage('de');
    vi.mocked(api.getDashboard).mockResolvedValue(dashboard());
    renderPage();

    expect(await screen.findByTestId('today-worked')).toHaveTextContent('7:48');
    expect(screen.getByText('Samstag, 14.11.2026')).toBeInTheDocument();
    expect(screen.getByText('von 7:48 h Soll')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Aktuelle Woche' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Zeit erfassen' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Mo 8:10h' })).toBeInTheDocument();
    expect(await screen.findByTestId('vacation-remaining')).toHaveTextContent('15 Tage übrig');
    expect(screen.getByText('10 genommen · 5 geplant · 30 gesamt')).toBeInTheDocument();
    expect(document.title).toBe('Dashboard | chroniqo');
  });

  it('renders the numbers of the dashboard', async () => {
    vi.mocked(api.getDashboard).mockResolvedValue(dashboard());
    renderPage();

    expect(await screen.findByTestId('today-worked')).toHaveTextContent('7:48');
    expect(screen.getByText('Saturday, 14/11/2026')).toBeInTheDocument();
    expect(screen.getByText('of 7:48 h target')).toBeInTheDocument();
    expect(screen.getByTestId('today-balance')).toHaveTextContent('+0:00');
    expect(screen.getByTestId('current-balance')).toHaveTextContent('+17 h 21 min');
    expect(screen.getByTestId('weekly-percentage')).toHaveTextContent('101%');
    expect(screen.getByText('39:25h / 39h')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Mon 8:10h' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Sat 0h' })).toBeInTheDocument();
  });

  it('shows the vacation account', async () => {
    vi.mocked(api.getDashboard).mockResolvedValue(dashboard());
    renderPage();

    expect(await screen.findByTestId('vacation-remaining')).toHaveTextContent('15 days left');
    expect(screen.getByText('10 taken · 5 planned · 30 total')).toBeInTheDocument();
  });

  it('shows the daily target legend when time was logged', async () => {
    vi.mocked(api.getDashboard).mockResolvedValue(dashboard());
    renderPage();

    expect(await screen.findByText('Daily target')).toBeInTheDocument();
    expect(screen.queryByText(/Nothing logged this week yet/)).not.toBeInTheDocument();
  });

  it('shows an empty state when nothing was logged this week', async () => {
    vi.mocked(api.getDashboard).mockResolvedValue(dashboard({ week: [day('2026-11-09', 0), day('2026-11-10', 0)] }));
    renderPage();

    expect(await screen.findByText(/Nothing logged this week yet/)).toBeInTheDocument();
  });

  it('shows negative values and a missing target', async () => {
    vi.mocked(api.getDashboard).mockResolvedValue(
      dashboard({
        today: { ...day('2026-11-14', 0), balanceMinutes: -58 },
        currentBalanceMinutes: -138,
        weeklyProgress: { workedMinutes: 0, targetMinutes: 0, percentage: 0, hasTarget: false },
      }),
    );
    renderPage();

    expect(await screen.findByTestId('today-balance')).toHaveTextContent('-0:58');
    expect(screen.getByTestId('current-balance')).toHaveTextContent('\u22122 h 18 min');
    expect(screen.getByTestId('weekly-percentage')).toHaveTextContent('–');
    expect(screen.getByText('No target this week')).toBeInTheDocument();
  });

  it('shows an error with retry', async () => {
    vi.mocked(api.getDashboard).mockRejectedValueOnce(new Error('boom')).mockResolvedValue(dashboard());
    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent('could not be loaded');
    await userEvent.setup().click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByTestId('today-worked')).toBeInTheDocument();
  });

  it('shows a running booking and stops it with the current time', async () => {
    vi.mocked(api.getDashboard).mockResolvedValue(dashboard());
    vi.mocked(api.getTimeEntries).mockResolvedValue([
      { id: 'e1', date: '2026-11-14', startTime: '09:00:00', endTime: null, breakMinutes: 30, notes: 'x' },
    ]);
    vi.mocked(api.updateTimeEntry).mockResolvedValue(undefined);
    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByTestId('running-entry')).toHaveTextContent('Running since 09:00');
    await user.click(screen.getByRole('button', { name: 'Stop' }));

    await waitFor(() =>
      expect(api.updateTimeEntry).toHaveBeenCalledWith('e1', {
        date: '2026-11-14',
        startTime: '09:00',
        endTime: '12:00',
        breakMinutes: 30,
        notes: 'x',
      }),
    );
    expect(await screen.findByText('Booking stopped at 12:00')).toBeInTheDocument();
  });

  it('does not show a running booking when all bookings are closed', async () => {
    vi.mocked(api.getDashboard).mockResolvedValue(dashboard());
    renderPage();

    await screen.findByTestId('today-worked');
    expect(screen.queryByTestId('running-entry')).not.toBeInTheDocument();
  });

  it('opens the time entry dialog for today and navigates to the month view', async () => {
    vi.mocked(api.getDashboard).mockResolvedValue(dashboard());
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole('button', { name: /Log Time/ }));
    expect(await screen.findByRole('dialog')).toHaveTextContent('14/11/2026');
    await waitFor(() => expect(api.getTimeEntries).toHaveBeenCalledWith('2026-11-14', '2026-11-14'));
    await user.click(screen.getByRole('button', { name: 'Close' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

    await user.click(screen.getByRole('button', { name: /View Month/ }));
    expect(await screen.findByText('Month route')).toBeInTheDocument();
  });
});
