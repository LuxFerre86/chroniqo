import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import type { UserProfile } from '../api/types';
import { AppShell } from './AppShell';

const logout = vi.fn().mockResolvedValue(undefined);
let currentUser: UserProfile | null;

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ user: currentUser, logout }),
}));

vi.mock('../lib/queries', () => ({
  useAppConfig: () => ({ appName: 'chroniqo', version: '2026.10.0' }),
}));

const profile: UserProfile = {
  email: 'ada@example.com',
  firstName: 'Ada',
  lastName: 'Lovelace',
  weeklyTargetHours: 40,
  workingDays: ['MONDAY'],
  firstDayOfWeek: 'MONDAY',
  countryCode: null,
  subdivisionCode: null,
  createdAt: null,
  lastLoginAt: null,
};

function renderShell(path = '/') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route element={<AppShell />}>
          <Route index element={<div>dashboard content</div>} />
          <Route path="/month" element={<div>month content</div>} />
          <Route path="/settings" element={<div>settings content</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.clear();
  currentUser = profile;
  window.scrollTo = vi.fn();
});

describe('AppShell', () => {
  it('renders the navigation links', () => {
    renderShell();

    const sidebar = screen.getByRole('complementary', { name: 'Main navigation' });
    expect(within(sidebar).getByRole('link', { name: 'Dashboard' })).toHaveAttribute('href', '/');
    expect(within(sidebar).getByRole('link', { name: 'Month' })).toHaveAttribute('href', '/month');
    expect(within(sidebar).getByRole('link', { name: 'Settings' })).toHaveAttribute('href', '/settings');
  });

  it('renders the phone tab bar with the shortened month label', () => {
    renderShell();

    const tabbar = screen.getByRole('navigation', { name: 'Main navigation' });
    expect(within(tabbar).getByRole('link', { name: /Dashboard/ })).toBeInTheDocument();
    expect(within(tabbar).getByRole('link', { name: /Month/ })).toHaveAttribute('href', '/month');
    expect(within(tabbar).getByRole('link', { name: /Settings/ })).toBeInTheDocument();
  });

  it('shows the user name, email and app version', () => {
    renderShell();

    expect(screen.getByText('Ada Lovelace')).toBeInTheDocument();
    expect(screen.getByText('AL')).toBeInTheDocument();
    expect(screen.getByText('ada@example.com')).toBeInTheDocument();
    expect(screen.getByText('2026.10.0')).toBeInTheDocument();
    expect(screen.getByText('chroniqo')).toBeInTheDocument();
  });

  it('renders the routed page and marks the active link', () => {
    renderShell('/settings');

    expect(screen.getByText('settings content')).toBeInTheDocument();
    const sidebar = screen.getByRole('complementary', { name: 'Main navigation' });
    expect(within(sidebar).getByRole('link', { name: 'Settings' })).toHaveAttribute('aria-current', 'page');
    expect(within(sidebar).getByRole('link', { name: 'Dashboard' })).not.toHaveAttribute('aria-current');
  });

  it('calls logout from the account menu', async () => {
    renderShell();

    await userEvent.click(screen.getByRole('button', { name: 'Account menu' }));
    expect(screen.getByRole('menuitem', { name: 'Settings' })).toHaveAttribute('href', '/settings');
    await userEvent.click(screen.getByRole('menuitem', { name: /Logout/ }));

    expect(logout).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('menu')).toBeNull();
  });

  it('closes the account menu with Escape', async () => {
    renderShell();

    await userEvent.click(screen.getByRole('button', { name: 'Account menu' }));
    expect(screen.getByRole('menu')).toBeInTheDocument();
    await userEvent.keyboard('{Escape}');
    expect(screen.queryByRole('menu')).toBeNull();
  });

  it('toggles and persists the collapsed sidebar', async () => {
    renderShell();

    await userEvent.click(screen.getByRole('button', { name: 'Hide navigation' }));

    expect(screen.getByRole('button', { name: 'Show navigation' })).toBeInTheDocument();
    expect(localStorage.getItem('chroniqo.sidebar.collapsed')).toBe('1');
  });

  it('renders without a user', () => {
    currentUser = null;
    renderShell();

    expect(screen.queryByText('Ada Lovelace')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Account menu' })).toBeInTheDocument();
  });
});
