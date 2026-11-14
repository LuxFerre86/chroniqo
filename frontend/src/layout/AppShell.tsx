import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router';
import { Button } from '../components/Button';
import { Icon } from '../components/Icon';
import type { IconName } from '../components/Icon';
import { UserMenu } from './UserMenu';
import { useAuth } from '../auth/AuthContext';
import { useAppConfig } from '../lib/queries';

const SIDEBAR_KEY = 'chroniqo.sidebar.collapsed';

const NAV_ITEMS: { to: string; label: string; icon: IconName; end?: boolean }[] = [
  { to: '/', label: 'Dashboard', icon: 'dashboard', end: true },
  { to: '/month', label: 'Month', icon: 'calendar' },
  { to: '/settings', label: 'Settings', icon: 'settings' },
];

/**
 * Layout of all signed-in pages: top bar, collapsible sidebar on larger
 * screens and a bottom tab bar on phones.
 */
export function AppShell() {
  const { user, logout } = useAuth();
  const config = useAppConfig();
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(() => localStorage.getItem(SIDEBAR_KEY) === '1');

  useEffect(() => {
    localStorage.setItem(SIDEBAR_KEY, collapsed ? '1' : '0');
  }, [collapsed]);

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);

  return (
    <div className={collapsed ? 'shell shell--collapsed' : 'shell'}>
      <header className="navbar">
        <Button
          variant="tertiary"
          icon
          className="navbar__toggle"
          aria-label={collapsed ? 'Show navigation' : 'Hide navigation'}
          aria-expanded={!collapsed}
          onClick={() => setCollapsed((c) => !c)}
        >
          <Icon name="menu" />
        </Button>
        <Link to="/" className="navbar__brand">
          <img className="navbar__logo" src="/icons/icon-48x48.png" alt="" width={32} height={32} />
          <span className="navbar__title">{config.appName}</span>
        </Link>
        <UserMenu user={user ?? null} onLogout={() => void logout()} />
      </header>

      <aside className="sidebar" aria-label="Main navigation">
        <nav className="sidebar__nav">
          {NAV_ITEMS.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end} className="nav-link" title={item.label}>
              <Icon name={item.icon} />
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar__footer">
          {user && <span>{user.email}</span>}
          {config.version && <span>{config.version}</span>}
        </div>
      </aside>

      <main className="shell__main">
        <Outlet />
      </main>

      <nav className="tabbar" aria-label="Main navigation">
        {NAV_ITEMS.map((item) => (
          <NavLink key={item.to} to={item.to} end={item.end} className="tabbar__item">
            <Icon name={item.icon} />
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
