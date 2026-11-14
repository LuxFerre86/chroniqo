import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '../api/endpoints';
import type { Dashboard, DaySummary } from '../api/types';
import { ToastProvider } from '../components/Toast';
import { DashboardPage } from './DashboardPage';

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
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  it('renders the numbers of the dashboard', async () => {
    vi.mocked(api.getDashboard).mockResolvedValue(dashboard());
    renderPage();

    expect(await screen.findByTestId('today-worked')).toHaveTextContent('7:48');
    expect(screen.getByText('Saturday, November 14, 2026')).toBeInTheDocument();
    expect(screen.getByTestId('today-balance')).toHaveTextContent('+0:00');
    expect(screen.getByTestId('current-balance')).toHaveTextContent('+17 h 21 min');
    expect(screen.getByTestId('weekly-percentage')).toHaveTextContent('101%');
    expect(screen.getByText('39:25h / 39h')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Mon 8:10h' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Sat 0h' })).toBeInTheDocument();
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

  it('opens the time entry dialog for today and navigates to the month view', async () => {
    vi.mocked(api.getDashboard).mockResolvedValue(dashboard());
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole('button', { name: /Log Time/ }));
    expect(await screen.findByRole('dialog')).toHaveTextContent('14.11.2026');
    await waitFor(() => expect(api.getTimeEntries).toHaveBeenCalledWith('2026-11-14', '2026-11-14'));
    await user.click(screen.getByRole('button', { name: 'Close' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

    await user.click(screen.getByRole('button', { name: /View Month/ }));
    expect(await screen.findByText('Month route')).toBeInTheDocument();
  });
});
