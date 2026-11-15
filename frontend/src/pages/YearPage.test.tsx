import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '../api/endpoints';
import type { DaySummary } from '../api/types';
import { setActiveLanguage } from '../i18n';
import { YearPage } from './YearPage';

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ user: { firstDayOfWeek: 'MONDAY' } }),
}));

vi.mock('../api/endpoints', () => ({
  api: { getSummary: vi.fn(), getVacationAccount: vi.fn() },
}));

const day = (date: string, overrides: Partial<DaySummary> = {}): DaySummary => ({
  date,
  workday: true,
  workedMinutes: 0,
  targetMinutes: 480,
  balanceMinutes: -480,
  absenceType: null,
  absenceLabel: null,
  ...overrides,
});

const SUMMARIES: DaySummary[] = [
  day('2026-01-05', { workedMinutes: 540, balanceMinutes: 60 }),
  day('2026-01-06', { workedMinutes: 420, balanceMinutes: -60 }),
  day('2026-01-07', { absenceType: 'SICK', absenceLabel: 'Sick leave', balanceMinutes: 0 }),
  day('2026-01-08', { absenceType: 'VACATION', absenceLabel: 'Vacation', balanceMinutes: 0 }),
  day('2026-12-24', { absenceType: 'VACATION', absenceLabel: 'Vacation', balanceMinutes: 0 }),
];

function LocationProbe() {
  const location = useLocation();
  return <div data-testid="location">{location.pathname + location.search}</div>;
}

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/year']}>
        <Routes>
          <Route path="/year" element={<YearPage />} />
          <Route path="/month" element={<LocationProbe />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('YearPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 5, 15, 12, 0, 0));
    vi.mocked(api.getSummary).mockResolvedValue(SUMMARIES);
    vi.mocked(api.getVacationAccount).mockResolvedValue({
      year: 2026,
      entitlement: 30,
      taken: 1,
      planned: 1,
      remaining: 28,
    });
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
    setActiveLanguage('en');
  });

  it('shows totals, vacation account and absences for the current year', async () => {
    renderPage();

    const stats = await screen.findByRole('region', { name: 'Yearly statistics' });
    expect(within(stats).getByText('Worked').nextSibling).toHaveTextContent('16:00 h');
    expect(within(stats).getByText('Vacation').nextSibling).toHaveTextContent('1 of 30 days');
    expect(within(stats).getByText('28 remaining, 1 planned')).toBeInTheDocument();
    expect(within(stats).getByText('Sick leave: 1 day')).toBeInTheDocument();
    expect(within(stats).getByText('2 days with bookings')).toBeInTheDocument();
    expect(api.getSummary).toHaveBeenCalledWith('2026-01-01', '2026-12-31');
    expect(document.querySelectorAll('.year-month')).toHaveLength(12);
  });

  it('opens the month page when a day is clicked', async () => {
    renderPage();

    await userEvent.setup({ advanceTimers: vi.advanceTimersByTime }).click(await screen.findByRole('link', { name: /^05\/01\/2026/ }));

    expect(await screen.findByTestId('location')).toHaveTextContent('/month?month=2026-01');
  });

  it('switches the year and offers a way back', async () => {
    renderPage();
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });

    await screen.findByRole('region', { name: 'Yearly statistics' });
    await user.click(screen.getByRole('button', { name: 'Previous year' }));

    expect(api.getSummary).toHaveBeenCalledWith('2025-01-01', '2025-12-31');
    await user.click(await screen.findByRole('button', { name: 'This year' }));
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('2026');
  });

  it('renders in German', async () => {
    setActiveLanguage('de');
    renderPage();

    const stats = await screen.findByRole('region', { name: 'Jahresstatistik' });
    expect(within(stats).getByText('Urlaub').nextSibling).toHaveTextContent('1 von 30 Tagen');
    expect(screen.getByRole('link', { name: /^05\.01\.2026/ })).toBeInTheDocument();
    expect(screen.getByText('Monatsvergleich')).toBeInTheDocument();
    expect(document.title).toBe('Jahr | chroniqo');
  });
});
