import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ApiError } from '../api/http';
import type { UserProfile } from '../api/types';
import { ToastProvider } from '../components/Toast';
import { SettingsPage } from './SettingsPage';
import { setActiveLanguage } from '../i18n';

const refreshUser = vi.fn();
const clearSession = vi.fn();
let currentUser: UserProfile;

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ user: currentUser, refreshUser, clearSession }),
}));

vi.mock('../api/endpoints', () => ({
  api: {
    getConfig: vi.fn(),
    getCountries: vi.fn(),
    getSubdivisions: vi.fn(),
    updateProfile: vi.fn(),
    changePassword: vi.fn(),
    deleteAccount: vi.fn(),
    getVacationAccount: vi.fn(),
    setVacationEntitlement: vi.fn(),
    getCustomHolidays: vi.fn(),
    createCustomHoliday: vi.fn(),
    deleteCustomHoliday: vi.fn(),
  },
}));

import { api } from '../api/endpoints';

const mocked = vi.mocked(api);

const profile: UserProfile = {
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

const VALID_PASSWORD = 'NewPassword1234!';

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

function section(heading: string): HTMLElement {
  return screen.getByRole('heading', { name: heading }).closest('section') as HTMLElement;
}

beforeEach(() => {
  vi.clearAllMocks();
  currentUser = { ...profile };
  refreshUser.mockResolvedValue(profile);
  mocked.getConfig.mockResolvedValue({
    appName: 'chroniqo',
    version: '2026.10.0',
    registrationEnabled: true,
    passwordPolicy: { minLength: 12, maxLength: 128, specialChars: '!@#$%^&*()_+-=[]{}|;:,.<>?' },
  });
  mocked.getCountries.mockResolvedValue([
    { code: 'DE', name: 'Germany' },
    { code: 'AT', name: 'Austria' },
    { code: 'MC', name: 'Monaco' },
  ]);
  mocked.getSubdivisions.mockImplementation(async (code: string) =>
    code === 'DE'
      ? [
          { code: 'BY', name: 'Bavaria' },
          { code: 'HH', name: 'Hamburg' },
        ]
      : [],
  );
  mocked.updateProfile.mockResolvedValue(profile);
  mocked.changePassword.mockResolvedValue(undefined);
  mocked.deleteAccount.mockResolvedValue(undefined);
  mocked.getVacationAccount.mockImplementation(async (year: number) => ({
    year,
    entitlement: 30,
    taken: 0,
    planned: 0,
    remaining: 30,
  }));
  mocked.setVacationEntitlement.mockResolvedValue(undefined);
  mocked.getCustomHolidays.mockResolvedValue([
    { id: 'h1', name: 'Bridge day', startDate: '2026-05-15', endDate: '2026-05-15', recurring: false },
    { id: 'h2', name: 'Christmas break', startDate: '2026-12-24', endDate: '2026-12-31', recurring: true },
  ]);
  mocked.createCustomHoliday.mockResolvedValue({
    id: 'h3',
    name: 'x',
    startDate: '2026-06-01',
    endDate: '2026-06-01',
    recurring: false,
  });
  mocked.deleteCustomHoliday.mockResolvedValue(undefined);
});

describe('SettingsPage – custom days off', () => {
  it('lists the entries with their dates', async () => {
    renderPage();
    const holidays = section('Custom Days Off');

    expect(await within(holidays).findByText('Bridge day')).toBeInTheDocument();
    expect(within(holidays).getByText('15/05/2026')).toBeInTheDocument();
    expect(within(holidays).getByText('24/12/2026 – 31/12/2026 · every year')).toBeInTheDocument();
  });

  it('adds a single-day entry', async () => {
    renderPage();
    const holidays = section('Custom Days Off');

    await userEvent.type(within(holidays).getByLabelText(/^Name/), 'Company day');
    await userEvent.type(within(holidays).getByLabelText(/^Start date/), '2026-06-01');
    await userEvent.click(within(holidays).getByLabelText('Repeat every year'));
    await userEvent.click(within(holidays).getByRole('button', { name: 'Add Day Off' }));

    await waitFor(() =>
      expect(mocked.createCustomHoliday).toHaveBeenCalledWith({
        name: 'Company day',
        startDate: '2026-06-01',
        endDate: '2026-06-01',
        recurring: true,
      }),
    );
  });

  it('requires name and start date', async () => {
    renderPage();
    await userEvent.click(within(section('Custom Days Off')).getByRole('button', { name: 'Add Day Off' }));

    expect(mocked.createCustomHoliday).not.toHaveBeenCalled();
    expect(await screen.findByText('Name and start date are required.')).toBeInTheDocument();
  });

  it('deletes an entry', async () => {
    renderPage();
    await userEvent.click(await screen.findByRole('button', { name: 'Delete Bridge day' }));

    await waitFor(() => expect(mocked.deleteCustomHoliday).toHaveBeenCalledWith('h1'));
  });
});

describe('SettingsPage – vacation', () => {
  it('loads and saves the annual entitlement', async () => {
    renderPage();
    const vacation = section('Vacation');
    const input = await within(vacation).findByLabelText(/^Annual Entitlement/);
    await waitFor(() => expect(input).toHaveValue(30));

    await userEvent.clear(input);
    await userEvent.type(input, '28');
    await userEvent.click(within(vacation).getByRole('button', { name: 'Save Vacation' }));

    await waitFor(() =>
      expect(mocked.setVacationEntitlement).toHaveBeenCalledWith(new Date().getFullYear(), 28),
    );
  });
});

describe('SettingsPage – profile', () => {
  it('sets the document title and renders the four sections', async () => {
    renderPage();

    expect(document.title).toBe('Settings | chroniqo');
    expect(screen.getByRole('heading', { name: 'Settings', level: 1 })).toBeInTheDocument();
    for (const name of ['Profile Information', 'Change Password', 'Account Information', 'Danger Zone']) {
      expect(screen.getByRole('heading', { name })).toBeInTheDocument();
    }
    await screen.findByRole('option', { name: 'Hamburg' });
  });

  it('loads the saved profile values', async () => {
    renderPage();

    expect(screen.getByLabelText(/^First Name/)).toHaveValue('Ada');
    expect(screen.getByLabelText(/^Last Name/)).toHaveValue('Lovelace');
    expect(screen.getByLabelText(/^Email/)).toHaveValue('ada@example.com');
    expect(screen.getByLabelText(/^Email/)).toHaveAttribute('readonly');
    expect(screen.getByLabelText(/^Weekly Target Hours/)).toHaveValue(38);
    expect(screen.getByRole('checkbox', { name: 'Monday' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Friday' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Saturday' })).not.toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Sunday' })).not.toBeChecked();

    await waitFor(() => expect(screen.getByLabelText('Country')).toHaveValue('DE'));
    await waitFor(() => expect(screen.getByLabelText('State / Region')).toHaveValue('HH'));
  });

  it('hides the region select when the country has no subdivisions or none is selected', async () => {
    renderPage();
    await screen.findByLabelText('State / Region');

    await userEvent.selectOptions(screen.getByLabelText('Country'), 'MC');
    await waitFor(() => expect(screen.queryByLabelText('State / Region')).not.toBeInTheDocument());

    await userEvent.selectOptions(screen.getByLabelText('Country'), '');
    expect(screen.queryByLabelText('State / Region')).not.toBeInTheDocument();
  });

  it('resets the region when the country changes', async () => {
    mocked.getSubdivisions.mockImplementation(async (code: string) => [{ code: `${code}-1`, name: `Region ${code}` }]);
    renderPage();
    await waitFor(() => expect(screen.getByLabelText('State / Region')).toHaveValue(''));

    await userEvent.selectOptions(screen.getByLabelText('Country'), 'AT');

    await screen.findByRole('option', { name: 'Region AT' });
    expect(screen.getByLabelText('State / Region')).toHaveValue('');
  });

  it('sends the expected payload, refreshes the user and shows a success toast', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByLabelText('State / Region')).toHaveValue('HH'));

    const firstName = screen.getByLabelText(/^First Name/);
    await userEvent.clear(firstName);
    await userEvent.type(firstName, '  Augusta ');
    const hours = screen.getByLabelText(/^Weekly Target Hours/);
    await userEvent.clear(hours);
    await userEvent.type(hours, '30');
    await userEvent.click(screen.getByRole('checkbox', { name: 'Friday' }));
    await userEvent.click(screen.getByRole('checkbox', { name: 'Sunday' }));
    await userEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    await waitFor(() => expect(mocked.updateProfile).toHaveBeenCalledTimes(1));
    expect(mocked.updateProfile).toHaveBeenCalledWith({
      firstName: 'Augusta',
      lastName: 'Lovelace',
      weeklyTargetHours: 30,
      workingDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'SUNDAY'],
      countryCode: 'DE',
      subdivisionCode: 'HH',
    });
    expect(await screen.findByText('Profile updated successfully!')).toBeInTheDocument();
    expect(refreshUser).toHaveBeenCalledTimes(1);
  });

  it('sends null for country and region when no country is selected', async () => {
    currentUser = { ...profile, countryCode: null, subdivisionCode: null };
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    await waitFor(() => expect(mocked.updateProfile).toHaveBeenCalled());
    expect(mocked.updateProfile).toHaveBeenCalledWith(
      expect.objectContaining({ countryCode: null, subdivisionCode: null }),
    );
  });

  it('requires first and last name', async () => {
    renderPage();

    await userEvent.clear(screen.getByLabelText(/^First Name/));
    await userEvent.clear(screen.getByLabelText(/^Last Name/));
    await userEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    expect(await screen.findByText('First name is required')).toBeInTheDocument();
    expect(screen.getByText('Last name is required')).toBeInTheDocument();
    expect(mocked.updateProfile).not.toHaveBeenCalled();
  });

  it('requires at least one working day', async () => {
    currentUser = { ...profile, workingDays: ['MONDAY'] };
    renderPage();

    await userEvent.click(screen.getByRole('checkbox', { name: 'Monday' }));
    await userEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    expect(await screen.findByText('Please select at least one working day.')).toBeInTheDocument();
    expect(mocked.updateProfile).not.toHaveBeenCalled();
  });

  it('validates the weekly target hours range', async () => {
    renderPage();

    const hours = screen.getByLabelText(/^Weekly Target Hours/);
    await userEvent.clear(hours);
    await userEvent.type(hours, '81');
    await userEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    expect(await screen.findByText(/^Weekly target hours must be between 0 and 80/)).toBeInTheDocument();
    expect(mocked.updateProfile).not.toHaveBeenCalled();
  });

  it('accepts a decimal weekly target like 37.5', async () => {
    renderPage();

    const hours = screen.getByLabelText(/^Weekly Target Hours/);
    await userEvent.clear(hours);
    await userEvent.type(hours, '37.5');
    await userEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    await waitFor(() => expect(mocked.updateProfile).toHaveBeenCalledWith(expect.objectContaining({ weeklyTargetHours: 37.5 })));
  });

  it('rejects more than one decimal place', async () => {
    renderPage();

    const hours = screen.getByLabelText(/^Weekly Target Hours/);
    await userEvent.clear(hours);
    await userEvent.type(hours, '37.55');
    await userEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    expect(await screen.findByText(/^Weekly target hours must be between 0 and 80/)).toBeInTheDocument();
    expect(mocked.updateProfile).not.toHaveBeenCalled();
  });

  it('shows server errors as toast', async () => {
    mocked.updateProfile.mockRejectedValue(new ApiError(400, 'Invalid subdivision'));
    renderPage();

    await userEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    expect(await screen.findByText('Invalid subdivision')).toBeInTheDocument();
    expect(refreshUser).not.toHaveBeenCalled();
  });
});

describe('SettingsPage – password', () => {
  async function fillPasswords(current: string, next: string, confirm: string) {
    const form = section('Change Password');
    await userEvent.type(within(form).getByLabelText(/^Current Password/), current);
    await userEvent.type(within(form).getByLabelText(/^New Password/), next);
    await userEvent.type(within(form).getByLabelText(/^Confirm New Password/), confirm);
    await userEvent.click(within(form).getByRole('button', { name: 'Change Password' }));
  }

  it('shows the policy hint from the app config', async () => {
    renderPage();

    expect(within(section('Change Password')).getByText(/At least 12 characters/)).toBeInTheDocument();
    await screen.findByRole('option', { name: 'Hamburg' });
  });

  it('validates required fields', async () => {
    renderPage();

    await userEvent.click(within(section('Change Password')).getByRole('button', { name: 'Change Password' }));

    expect(await screen.findByText('Current password is required')).toBeInTheDocument();
    expect(screen.getByText('Password is required')).toBeInTheDocument();
    expect(screen.getByText('Please confirm your new password')).toBeInTheDocument();
    expect(mocked.changePassword).not.toHaveBeenCalled();
  });

  it('rejects a new password that violates the policy', async () => {
    renderPage();

    await fillPasswords('OldPassword1!', 'short', 'short');

    expect(await screen.findByText('Password must be at least 12 characters')).toBeInTheDocument();
    expect(mocked.changePassword).not.toHaveBeenCalled();
  });

  it('rejects mismatching confirmation', async () => {
    renderPage();

    await fillPasswords('OldPassword1!', VALID_PASSWORD, 'Different1234!');

    expect(await screen.findByText('Passwords do not match')).toBeInTheDocument();
    expect(mocked.changePassword).not.toHaveBeenCalled();
  });

  it('changes the password and clears the fields', async () => {
    renderPage();

    await fillPasswords('OldPassword1!', VALID_PASSWORD, VALID_PASSWORD);

    expect(await screen.findByText('Password changed successfully!')).toBeInTheDocument();
    expect(mocked.changePassword).toHaveBeenCalledWith('OldPassword1!', VALID_PASSWORD);
    const form = section('Change Password');
    expect(within(form).getByLabelText(/^Current Password/)).toHaveValue('');
    expect(within(form).getByLabelText(/^New Password/)).toHaveValue('');
    expect(within(form).getByLabelText(/^Confirm New Password/)).toHaveValue('');
  });

  it('shows a wrong current password as field error', async () => {
    mocked.changePassword.mockRejectedValue(new ApiError(400, 'Current password is incorrect'));
    renderPage();

    await fillPasswords('WrongPassword1!', VALID_PASSWORD, VALID_PASSWORD);

    const form = section('Change Password');
    expect(await within(form).findByText('Current password is incorrect')).toBeInTheDocument();
    expect(within(form).getByLabelText(/^New Password/)).toHaveValue(VALID_PASSWORD);
  });

  it('shows other server errors as toast', async () => {
    mocked.changePassword.mockRejectedValue(new ApiError(500, 'Boom'));
    renderPage();

    await fillPasswords('OldPassword1!', VALID_PASSWORD, VALID_PASSWORD);

    expect(await screen.findByText('Boom')).toBeInTheDocument();
  });
});

describe('SettingsPage – account information', () => {
  it('shows the date parts but not the email and version shown in the sidebar', () => {
    renderPage();

    const account = section('Account Information');
    expect(within(account).getByText('04/03/2025, 10:15')).toBeInTheDocument();
    expect(within(account).getByText('Never')).toBeInTheDocument();
    expect(within(account).queryByText('ada@example.com')).toBeNull();
    expect(within(account).queryByText('2026.10.0')).toBeNull();
  });

  it('falls back to Unknown when the creation date is missing', () => {
    currentUser = { ...profile, createdAt: null, lastLoginAt: '2026-11-14T08:00:00' };
    renderPage();

    const account = section('Account Information');
    expect(within(account).getByText('Unknown')).toBeInTheDocument();
    expect(within(account).getByText('14/11/2026, 08:00')).toBeInTheDocument();
  });
});

describe('SettingsPage – delete account', () => {
  const assign = vi.fn();

  beforeEach(() => {
    assign.mockReset();
    vi.stubGlobal('location', { ...window.location, assign });
  });

  async function openDialog() {
    await userEvent.click(within(section('Danger Zone')).getByRole('button', { name: 'Delete Account' }));
    return screen.getByRole('dialog', { name: 'Delete Account' });
  }

  it('opens a dialog explaining the consequences', async () => {
    renderPage();

    const dialog = await openDialog();

    expect(within(dialog).getByText(/This action cannot be undone/)).toBeInTheDocument();
    expect(within(dialog).getByPlaceholderText('DELETE')).toBeInTheDocument();
    expect(within(dialog).getByText('Type DELETE in uppercase to confirm')).toBeInTheDocument();
  });

  it('closes on Cancel and discards the input', async () => {
    renderPage();
    let dialog = await openDialog();
    await userEvent.type(within(dialog).getByPlaceholderText('DELETE'), 'DEL');

    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    dialog = await openDialog();
    expect(within(dialog).getByPlaceholderText('DELETE')).toHaveValue('');
  });

  it('requires the DELETE confirmation', async () => {
    renderPage();
    const dialog = await openDialog();

    await userEvent.type(within(dialog).getByPlaceholderText('DELETE'), 'delete');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Delete Account' }));

    expect(await screen.findByText('Please type DELETE to confirm account removal.')).toBeInTheDocument();
    expect(mocked.deleteAccount).not.toHaveBeenCalled();
  });

  it('requires the current password', async () => {
    renderPage();
    const dialog = await openDialog();

    await userEvent.type(within(dialog).getByPlaceholderText('DELETE'), 'DELETE');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Delete Account' }));

    expect(await screen.findByText('Please enter your current password.')).toBeInTheDocument();
    expect(mocked.deleteAccount).not.toHaveBeenCalled();
  });

  it('deletes the account, clears the session and reloads to the login page', async () => {
    renderPage();
    const dialog = await openDialog();

    await userEvent.type(within(dialog).getByPlaceholderText('DELETE'), 'DELETE');
    await userEvent.type(within(dialog).getByLabelText(/^Current Password/), 'OldPassword1!');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Delete Account' }));

    await waitFor(() => expect(assign).toHaveBeenCalledWith('/login?accountDeleted'));
    expect(mocked.deleteAccount).toHaveBeenCalledWith('OldPassword1!');
    expect(clearSession).toHaveBeenCalledTimes(1);
  });

  it('shows server errors and keeps the dialog open', async () => {
    mocked.deleteAccount.mockRejectedValue(new ApiError(400, 'Password is incorrect'));
    renderPage();
    const dialog = await openDialog();

    await userEvent.type(within(dialog).getByPlaceholderText('DELETE'), 'DELETE');
    await userEvent.type(within(dialog).getByLabelText(/^Current Password/), 'wrong');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Delete Account' }));

    expect(await screen.findByText('Password is incorrect')).toBeInTheDocument();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(clearSession).not.toHaveBeenCalled();
    expect(assign).not.toHaveBeenCalled();
  });
});

describe('SettingsPage – working day chips', () => {
  it('orders the days by the regional week start', () => {
    currentUser = { ...profile, firstDayOfWeek: 'SUNDAY' };
    renderPage();

    const group = screen.getByRole('group', { name: 'Working Days' });
    const names = within(group).getAllByRole('checkbox').map((c) => c.getAttribute('aria-label'));
    expect(names).toEqual(['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']);
  });
});

describe('SettingsPage – language', () => {
  it('saves the chosen language and refreshes the user', async () => {
    renderPage();
    const select = await screen.findByLabelText('Language');
    expect(select).toHaveValue('en');
    expect(within(select).getByRole('option', { name: 'Deutsch' })).toBeInTheDocument();

    await userEvent.selectOptions(select, 'de');

    await waitFor(() => expect(mocked.updateProfile).toHaveBeenCalledWith(expect.objectContaining({ language: 'de' })));
    expect(refreshUser).toHaveBeenCalled();
  });

  it('shows an error toast when saving the language fails', async () => {
    mocked.updateProfile.mockRejectedValue(new ApiError(500, 'Boom'));
    renderPage();
    await userEvent.selectOptions(await screen.findByLabelText('Language'), 'de');

    expect(await screen.findByText('Boom')).toBeInTheDocument();
    expect(refreshUser).not.toHaveBeenCalled();
  });

  it('renders German texts when the active language is German', async () => {
    setActiveLanguage('de');
    renderPage();

    expect(await screen.findByRole('heading', { name: 'Einstellungen' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Passwort ändern' })).toBeInTheDocument();
    expect(screen.getByLabelText('Sprache')).toHaveValue('de');
    const group = screen.getByRole('group', { name: 'Arbeitstage' });
    expect(within(group).getAllByRole('checkbox').map((c) => c.getAttribute('aria-label'))[0]).toBe('Montag');
    expect(document.title).toBe('Einstellungen | chroniqo');
  });
});
