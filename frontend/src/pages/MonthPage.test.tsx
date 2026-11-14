import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '../api/endpoints';
import type { DayOfWeekName, DaySummary } from '../api/types';
import { ToastProvider } from '../components/Toast';
import { MonthPage } from './MonthPage';

const auth = vi.hoisted(() => ({ firstDayOfWeek: 'MONDAY' as DayOfWeekName }));

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ user: { firstDayOfWeek: auth.firstDayOfWeek } }),
}));

vi.mock('../api/endpoints', () => ({
  api: {
    getSummary: vi.fn(),
    getTimeEntries: vi.fn(),
    getAbsences: vi.fn(),
    createTimeEntry: vi.fn(),
    updateTimeEntry: vi.fn(),
    deleteTimeEntry: vi.fn(),
    saveAbsence: vi.fn(),
    deleteAbsences: vi.fn(),
  },
}));

const day = (date: string, overrides: Partial<DaySummary> = {}): DaySummary => ({
  date,
  workday: true,
  workedMinutes: 0,
  targetMinutes: 468,
  balanceMinutes: -468,
  absenceType: null,
  absenceLabel: null,
  ...overrides,
});

const SUMMARIES: DaySummary[] = [
  day('2026-10-30', { workedMinutes: 500, balanceMinutes: 32 }),
  day('2026-11-02', { workedMinutes: 410, balanceMinutes: -58 }),
  day('2026-11-03', { workedMinutes: 468, balanceMinutes: 0 }),
  day('2026-11-07', { workday: false, targetMinutes: 0, balanceMinutes: 0 }),
  day('2026-11-11', {
    workday: false,
    targetMinutes: 0,
    balanceMinutes: 0,
    absenceType: 'HOLIDAY',
    absenceLabel: 'St. Martin',
  }),
  day('2026-11-16'),
];

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <ToastProvider>
          <MonthPage />
        </ToastProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('MonthPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 10, 14, 12, 0, 0));
    vi.mocked(api.getSummary).mockResolvedValue(SUMMARIES);
    vi.mocked(api.getTimeEntries).mockResolvedValue([]);
    vi.mocked(api.getAbsences).mockResolvedValue([]);
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
    auth.firstDayOfWeek = 'MONDAY';
  });

  const headerLabels = () =>
    Array.from(document.querySelectorAll('.cal__head')).map((el) => el.textContent);

  it('starts the calendar on Monday by default', async () => {
    renderPage();

    await screen.findByRole('button', { name: /^02\.11\.2026/ });
    expect(headerLabels()).toEqual(['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']);
    // 1 Nov 2026 is a Sunday: six empty cells precede it
    expect(document.querySelectorAll('.cal__pad')).toHaveLength(6);
  });

  it("starts the calendar on the user's first day of the week", async () => {
    auth.firstDayOfWeek = 'SUNDAY';
    renderPage();

    await screen.findByRole('button', { name: /^02\.11\.2026/ });
    expect(headerLabels()).toEqual(['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']);
    expect(document.querySelectorAll('.cal__pad')).toHaveLength(0);
    const weekend = Array.from(document.querySelectorAll('.cal__head--weekend')).map((el) => el.textContent);
    expect(weekend).toEqual(['Sun', 'Sat']);
  });

  it('computes month and year statistics from the days up to today', async () => {
    renderPage();

    const stats = await screen.findByLabelText('Statistics');
    expect(api.getSummary).toHaveBeenCalledTimes(1);
    expect(api.getSummary).toHaveBeenCalledWith('2026-01-01', '2026-12-31');

    // month: Nov 2 + Nov 3 (the 16th is in the future); year additionally Oct 30
    expect(within(stats).getByText('15:36 h')).toBeInTheDocument();
    expect(within(stats).getByText('23:24 h')).toBeInTheDocument();
    expect(within(stats).getByText('14:38 h')).toBeInTheDocument();
    expect(within(stats).getByText('22:58 h')).toBeInTheDocument();
    expect(within(stats).getByText('-0:58 h')).toHaveClass('is-negative');
    expect(within(stats).getByText('-0:26 h')).toHaveClass('is-negative');
  });

  it('renders the calendar cells with badges and balance chips', async () => {
    renderPage();

    const cell = await screen.findByRole('button', { name: /^02\.11\.2026/ });
    expect(within(cell).getByText('6:50 h')).toBeInTheDocument();
    expect(within(cell).getByText('-0:58 h')).toHaveClass('balance-negative');

    const holiday = screen.getByRole('button', { name: /^11\.11\.2026/ });
    expect(within(holiday).getByText('St. Martin')).toHaveClass('badge--holiday');
    expect(within(holiday).queryByText(/h$/)).not.toBeInTheDocument();

    const today = screen.getByRole('button', { name: /^14\.11\.2026/ });
    expect(today).toHaveClass('day--today');
    expect(screen.getAllByRole('button', { name: /^\d\d\.11\.2026/ })).toHaveLength(30);
  });

  it('navigates between months without refetching the year and offers a Today shortcut', async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByLabelText('Statistics');

    expect(screen.queryByRole('button', { name: 'Today' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Next month' }));
    expect(screen.getByRole('button', { name: 'December 2026' })).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /^\d\d\.12\.2026/ })).toHaveLength(31);
    expect(api.getSummary).toHaveBeenCalledTimes(1);

    await user.click(screen.getByRole('button', { name: 'Today' }));
    expect(screen.getByRole('button', { name: 'November 2026' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Today' })).not.toBeInTheDocument();
  });

  it('selects a month in the picker', async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByLabelText('Statistics');

    await user.click(screen.getByRole('button', { name: 'November 2026' }));
    const dialog = await screen.findByRole('dialog', { name: 'Select Month' });
    await user.selectOptions(within(dialog).getByLabelText('Year'), '2027');
    await user.click(within(dialog).getByRole('button', { name: 'Mar' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(screen.getByRole('button', { name: 'March 2027' })).toBeInTheDocument();
    await waitFor(() => expect(api.getSummary).toHaveBeenCalledWith('2027-01-01', '2027-12-31'));
  });

  it('opens the dialog for the clicked day (mouse and keyboard)', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole('button', { name: /^02\.11\.2026/ }));
    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent('02.11.2026');
    await waitFor(() => expect(api.getTimeEntries).toHaveBeenCalledWith('2026-11-02', '2026-11-02'));
    await user.click(within(dialog).getByRole('button', { name: 'Close' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

    screen.getByRole('button', { name: /^03\.11\.2026/ }).focus();
    await user.keyboard('{Enter}');
    expect(await screen.findByRole('dialog')).toHaveTextContent('03.11.2026');
  });
});
