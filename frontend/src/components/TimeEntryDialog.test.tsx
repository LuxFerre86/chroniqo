import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '../api/endpoints';
import { ApiError } from '../api/http';
import type { TimeEntry } from '../api/types';
import { ToastProvider } from './Toast';
import { TimeEntryDialog } from './TimeEntryDialog';

vi.mock('../api/endpoints', () => ({
  api: {
    getTimeEntries: vi.fn(),
    getAbsences: vi.fn(),
    createTimeEntry: vi.fn(),
    updateTimeEntry: vi.fn(),
    deleteTimeEntry: vi.fn(),
    saveAbsence: vi.fn(),
    deleteAbsences: vi.fn(),
    getDashboard: vi.fn(),
    getSummary: vi.fn(),
  },
}));

const DATE = '2026-11-14';

const entry = (overrides: Partial<TimeEntry> = {}): TimeEntry => ({
  id: 'e1',
  date: DATE,
  startTime: '09:00:00',
  endTime: '12:30:00',
  breakMinutes: 30,
  notes: 'Standup and planning',
  ...overrides,
});

function renderDialog(onClose = vi.fn()) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <ToastProvider>
          <TimeEntryDialog date={DATE} onClose={onClose} />
        </ToastProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
  return { onClose, client };
}

describe('TimeEntryDialog', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(api.getTimeEntries).mockResolvedValue([]);
    vi.mocked(api.getAbsences).mockResolvedValue([]);
    vi.mocked(api.createTimeEntry).mockResolvedValue(undefined);
    vi.mocked(api.updateTimeEntry).mockResolvedValue(undefined);
    vi.mocked(api.deleteTimeEntry).mockResolvedValue(undefined);
    vi.mocked(api.saveAbsence).mockResolvedValue(undefined);
    vi.mocked(api.deleteAbsences).mockResolvedValue(undefined);
  });

  it('rejects times that are not valid 24-hour values', async () => {
    const user = userEvent.setup();
    renderDialog();

    await screen.findByText('New booking');
    fireEvent.change(screen.getByLabelText(/Start time/), { target: { value: '25:99' } });
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('HH:MM (24-hour)');
    expect(api.createTimeEntry).not.toHaveBeenCalled();
  });

  it('renders nothing without a date', () => {
    render(<TimeEntryDialog date={null} onClose={() => undefined} />);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('shows the header and an empty state in working mode', async () => {
    renderDialog();

    expect(await screen.findByText('New booking')).toBeInTheDocument();
    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveTextContent('14.11.2026');
    expect(dialog).toHaveTextContent('Working time');
    expect(dialog).toHaveTextContent('No bookings yet for this day');
    expect(screen.getByText(/Add the first booking for this day/)).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Bookings' })).toBeNull();
    expect(screen.queryByLabelText(/^Date/)).toBeNull();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Change date' }));
    expect(screen.getByLabelText(/^Date/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Break \(min\)/)).toHaveValue(30);
    expect(screen.queryByRole('button', { name: 'Delete booking' })).not.toBeInTheDocument();
  });

  it('saves a new booking with HH:mm times and an open end', async () => {
    const user = userEvent.setup();
    renderDialog();
    await screen.findByText('New booking');

    fireEvent.change(screen.getByLabelText(/Start time/), { target: { value: '09:00' } });
    await user.type(screen.getByLabelText(/Notes/), '  Focus work  ');
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() =>
      expect(api.createTimeEntry).toHaveBeenCalledWith({
        date: DATE,
        startTime: '09:00',
        endTime: null,
        breakMinutes: 30,
        notes: 'Focus work',
      }),
    );
    expect(await screen.findByText('Time entry saved successfully')).toBeInTheDocument();
    expect(screen.getByText('New booking')).toBeInTheDocument();
    expect(screen.getByLabelText(/Start time/)).toHaveValue('');
  });

  it('requires a start time', async () => {
    const user = userEvent.setup();
    renderDialog();
    await screen.findByText('New booking');

    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Start time is mandatory');
    expect(api.createTimeEntry).not.toHaveBeenCalled();
  });

  it('lists bookings sorted by start time and updates the selected one', async () => {
    vi.mocked(api.getTimeEntries).mockResolvedValue([
      entry({ id: 'late', startTime: '13:00:00', endTime: null, breakMinutes: null, notes: null }),
      entry(),
    ]);
    const user = userEvent.setup();
    renderDialog();

    const items = await screen.findAllByRole('listitem');
    expect(items[0]).toHaveTextContent('09:00 - 12:30');
    expect(items[0]).toHaveTextContent('Break 30m · Standup and planning');
    expect(items[1]).toHaveTextContent('13:00 - OPEN');
    expect(items[1]).toHaveTextContent('No extra details');
    expect(screen.getByRole('dialog')).toHaveTextContent('2 bookings · 3:00 h worked · 1 open booking');

    await user.click(within(items[0]).getByRole('button', { name: /09:00 - 12:30/, pressed: false }));
    expect(screen.getByText('Editing booking')).toBeInTheDocument();
    expect(screen.getByLabelText(/Start time/)).toHaveValue('09:00');
    expect(screen.getByLabelText(/End time/)).toHaveValue('12:30');

    fireEvent.change(screen.getByLabelText(/End time/), { target: { value: '13:00' } });
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() =>
      expect(api.updateTimeEntry).toHaveBeenCalledWith('e1', {
        date: DATE,
        startTime: '09:00',
        endTime: '13:00',
        breakMinutes: 30,
        notes: 'Standup and planning',
      }),
    );
    expect(api.createTimeEntry).not.toHaveBeenCalled();
  });

  it('deletes a booking directly from the list', async () => {
    vi.mocked(api.getTimeEntries).mockResolvedValue([entry()]);
    const user = userEvent.setup();
    renderDialog();

    await user.click(await screen.findByRole('button', { name: /Delete booking 09:00 - 12:30/ }));

    await waitFor(() => expect(api.deleteTimeEntry).toHaveBeenCalledWith('e1'));
    expect(await screen.findByText('Time entry deleted')).toBeInTheDocument();
  });

  it('deletes the booking that is being edited via the footer', async () => {
    vi.mocked(api.getTimeEntries).mockResolvedValue([entry()]);
    const user = userEvent.setup();
    renderDialog();

    await user.click(await screen.findByRole('button', { name: 'Edit booking 09:00 - 12:30' }));
    await user.click(screen.getByRole('button', { name: 'Delete booking' }));

    await waitFor(() => expect(api.deleteTimeEntry).toHaveBeenCalledWith('e1'));
    expect(await screen.findByText('New booking')).toBeInTheDocument();
  });

  it('opens directly in absence mode and saves a date range', async () => {
    vi.mocked(api.getAbsences).mockResolvedValue([{ date: DATE, type: 'VACATION' }]);
    const user = userEvent.setup();
    const { onClose } = renderDialog();

    expect(await screen.findByRole('button', { name: 'Remove vacation' })).toBeInTheDocument();
    expect(screen.getByRole('dialog')).toHaveTextContent('Vacation');
    expect(screen.getByLabelText(/Start date/)).toHaveValue(DATE);
    expect(screen.getByLabelText(/End date/)).toHaveValue(DATE);

    fireEvent.change(screen.getByLabelText(/End date/), { target: { value: '2026-11-18' } });
    expect(screen.getByRole('dialog')).toHaveTextContent('14.11.2026 – 18.11.2026');
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() => expect(api.saveAbsence).toHaveBeenCalledWith(DATE, '2026-11-18', 'VACATION'));
    expect(await screen.findByText('Vacation booked')).toBeInTheDocument();
    await waitFor(() => expect(onClose).toHaveBeenCalled());
  });

  it('moves the end date along with the start date and validates the range', async () => {
    const user = userEvent.setup();
    renderDialog();
    await screen.findByText('New booking');

    await user.click(screen.getByRole('button', { name: 'Sick' }));
    fireEvent.change(screen.getByLabelText(/Start date/), { target: { value: '2026-11-10' } });
    expect(screen.getByLabelText(/End date/)).toHaveValue('2026-11-10');

    fireEvent.change(screen.getByLabelText(/End date/), { target: { value: '2026-11-09' } });
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('End date must be on or after start date');
    expect(api.saveAbsence).not.toHaveBeenCalled();
  });

  it('removes an absence', async () => {
    vi.mocked(api.getAbsences).mockResolvedValue([{ date: DATE, type: 'SICK' }]);
    const user = userEvent.setup();
    const { onClose } = renderDialog();

    await user.click(await screen.findByRole('button', { name: 'Remove sick leave' }));

    await waitFor(() => expect(api.deleteAbsences).toHaveBeenCalledWith(DATE, DATE));
    expect(await screen.findByText('Sick leave removed')).toBeInTheDocument();
    await waitFor(() => expect(onClose).toHaveBeenCalled());
  });

  it('prefers working mode when the day has bookings, even with an absence', async () => {
    vi.mocked(api.getTimeEntries).mockResolvedValue([entry()]);
    vi.mocked(api.getAbsences).mockResolvedValue([{ date: DATE, type: 'SICK' }]);
    renderDialog();

    expect(await screen.findByText('New booking')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Remove sick leave' })).not.toBeInTheDocument();
  });

  it('shows server errors inline and clears them on the next input', async () => {
    vi.mocked(api.createTimeEntry).mockRejectedValue(
      new ApiError(422, 'Time range overlaps with existing entry (09:00 - 12:00)'),
    );
    const user = userEvent.setup();
    renderDialog();
    await screen.findByText('New booking');

    fireEvent.change(screen.getByLabelText(/Start time/), { target: { value: '10:00' } });
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Time range overlaps with existing entry (09:00 - 12:00)');

    fireEvent.change(screen.getByLabelText(/Start time/), { target: { value: '12:00' } });
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('shows a generic message for unexpected errors', async () => {
    vi.mocked(api.createTimeEntry).mockRejectedValue(new Error('network down'));
    const user = userEvent.setup();
    renderDialog();
    await screen.findByText('New booking');

    fireEvent.change(screen.getByLabelText(/Start time/), { target: { value: '10:00' } });
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('An unexpected error occurred. Please try again.');
  });
});
