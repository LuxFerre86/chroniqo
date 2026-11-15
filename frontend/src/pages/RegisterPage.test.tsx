import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/http';
import { ToastProvider } from '../components/Toast';
import { RegisterPage } from './RegisterPage';

const mocks = vi.hoisted(() => ({
  register: vi.fn(),
  getConfig: vi.fn(),
}));

vi.mock('../api/endpoints', () => ({
  api: {
    getConfig: mocks.getConfig,
    getCountries: vi.fn().mockResolvedValue([
      { code: 'DE', name: 'Germany' },
      { code: 'AT', name: 'Austria' },
    ]),
    getSubdivisions: vi.fn((code: string) =>
      Promise.resolve(code === 'DE' ? [{ code: 'DE-BY', name: 'Bavaria' }] : []),
    ),
    register: mocks.register,
  },
}));

const config = {
  appName: 'chroniqo',
  version: null,
  registrationEnabled: true,
  passwordPolicy: { minLength: 12, maxLength: 128, specialChars: '!@#$%^&*()_+-=[]{}|;:,.<>?' },
};

function renderRegister() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <MemoryRouter initialEntries={['/register']}>
          <Routes>
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/login" element={<div>login page</div>} />
          </Routes>
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  );
}

async function fillValid(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText(/^First Name/), 'Jane');
  await user.type(screen.getByLabelText(/^Last Name/), 'Doe');
  await user.type(screen.getByLabelText(/^Weekly Target Hours/), '40');
  await user.type(screen.getByLabelText(/^Email/), 'jane@example.com');
  await user.type(screen.getByLabelText(/^Password/), 'Str0ng!Passw0rd');
  await user.type(screen.getByLabelText(/^Confirm Password/), 'Str0ng!Passw0rd');
}

describe('RegisterPage', () => {
  beforeEach(() => {
    mocks.register.mockReset();
    mocks.getConfig.mockReset();
    mocks.getConfig.mockResolvedValue(config);
  });

  it('shows required-field messages on submit and does not call the API', async () => {
    const user = userEvent.setup();
    renderRegister();
    await user.click(screen.getByRole('button', { name: 'Create Account' }));

    expect(await screen.findByText('First name is required')).toBeInTheDocument();
    expect(screen.getByText('Last name is required')).toBeInTheDocument();
    expect(screen.getByText('Weekly Target Hours are required')).toBeInTheDocument();
    expect(screen.getByText('Email is required')).toBeInTheDocument();
    expect(screen.getByText('Password is required')).toBeInTheDocument();
    expect(screen.getByText('Please confirm your password')).toBeInTheDocument();
    expect(mocks.register).not.toHaveBeenCalled();
  });

  it('validates email format, hours range and password confirmation', async () => {
    const user = userEvent.setup();
    renderRegister();
    await user.type(screen.getByLabelText(/^Email/), 'not-an-email');
    await user.type(screen.getByLabelText(/^Weekly Target Hours/), '99');
    await user.type(screen.getByLabelText(/^Password/), 'Str0ng!Passw0rd');
    await user.type(screen.getByLabelText(/^Confirm Password/), 'different');
    await user.click(screen.getByRole('button', { name: 'Create Account' }));

    expect(await screen.findByText('Please enter a valid email address')).toBeInTheDocument();
    expect(screen.getByText(/^Please enter a value between 0 and 80/)).toBeInTheDocument();
    expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
    expect(mocks.register).not.toHaveBeenCalled();
  });

  it('validates a field on blur', async () => {
    const user = userEvent.setup();
    renderRegister();
    await user.click(screen.getByLabelText(/^First Name/));
    await user.tab();
    expect(await screen.findByText('First name is required')).toBeInTheDocument();
  });

  it('submits the expected payload and navigates to the login page', async () => {
    mocks.register.mockResolvedValue(undefined);
    const user = userEvent.setup();
    renderRegister();

    await fillValid(user);
    await user.selectOptions(await screen.findByLabelText('Country'), 'DE');
    await user.selectOptions(await screen.findByLabelText('State / Region'), 'DE-BY');
    await user.click(screen.getByRole('button', { name: 'Create Account' }));

    await waitFor(() =>
      expect(mocks.register).toHaveBeenCalledWith({
        email: 'jane@example.com',
        password: 'Str0ng!Passw0rd',
        firstName: 'Jane',
        lastName: 'Doe',
        weeklyTargetHours: 40,
        countryCode: 'DE',
        subdivisionCode: 'DE-BY',
      }),
    );
    expect(await screen.findByText('login page')).toBeInTheDocument();
    expect(
      screen.getByText('Registration successful! Please check your email to verify your account.'),
    ).toBeInTheDocument();
  });

  it('sends null country/subdivision when none selected and hides the subdivision field', async () => {
    mocks.register.mockResolvedValue(undefined);
    const user = userEvent.setup();
    renderRegister();
    await fillValid(user);
    expect(screen.queryByLabelText('State / Region')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Create Account' }));
    await waitFor(() =>
      expect(mocks.register).toHaveBeenCalledWith(
        expect.objectContaining({ countryCode: null, subdivisionCode: null }),
      ),
    );
  });

  it('maps server field errors onto the fields', async () => {
    mocks.register.mockRejectedValue(
      new ApiError(400, 'Validation failed', 'VALIDATION_FAILED', undefined, { email: 'Email already registered' }),
    );
    const user = userEvent.setup();
    renderRegister();
    await fillValid(user);
    await user.click(screen.getByRole('button', { name: 'Create Account' }));
    expect(await screen.findByText('Email already registered')).toBeInTheDocument();
  });

  it('shows a notice and disables submit when registration is disabled', async () => {
    mocks.getConfig.mockResolvedValue({ ...config, registrationEnabled: false });
    renderRegister();
    expect(await screen.findByText(/Registration is currently disabled/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Create Account' })).toBeDisabled();
  });
});
