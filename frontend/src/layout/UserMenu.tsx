import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router';
import { Icon } from '../components/Icon';
import type { UserProfile } from '../api/types';
import { useT } from '../i18n';

function initials(user: UserProfile | null): string {
  if (!user) return '?';
  const letters = `${user.firstName.trim().charAt(0)}${user.lastName.trim().charAt(0)}`.toUpperCase();
  return letters || user.email.charAt(0).toUpperCase() || '?';
}

/** Avatar button with a dropdown for settings and logout. */
export function UserMenu({ user, onLogout }: { user: UserProfile | null; onLogout: () => void }) {
  const { t } = useT();
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const fullName = user ? `${user.firstName} ${user.lastName}`.trim() : '';

  useEffect(() => {
    if (!open) return;
    const onPointer = (e: MouseEvent) => {
      if (!rootRef.current?.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false);
    };
    document.addEventListener('mousedown', onPointer);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onPointer);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  return (
    <div className="user-menu" ref={rootRef}>
      <button
        type="button"
        className="user-menu__trigger"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={t('userMenu.trigger')}
        onClick={() => setOpen((v) => !v)}
      >
        <span className="user-menu__avatar" aria-hidden="true">
          {initials(user)}
        </span>
        <span className="user-menu__name">{fullName}</span>
        <Icon name="chevron-down" className="user-menu__chevron" />
      </button>
      {open && (
        <div className="user-menu__panel" role="menu" aria-label={t('userMenu.panel')}>
          <div className="user-menu__who">
            <span className="user-menu__who-name">{fullName}</span>
            {user && <span className="user-menu__who-mail">{user.email}</span>}
          </div>
          <Link to="/settings" role="menuitem" className="user-menu__item" onClick={() => setOpen(false)}>
            <Icon name="settings" />
            {t('userMenu.settings')}
          </Link>
          <button
            type="button"
            role="menuitem"
            className="user-menu__item user-menu__item--danger"
            onClick={() => {
              setOpen(false);
              onLogout();
            }}
          >
            <Icon name="logout" />
            {t('userMenu.logout')}
          </button>
        </div>
      )}
    </div>
  );
}
