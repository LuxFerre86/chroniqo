import { useEffect, useId, useState } from 'react';
import type { FormEvent } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import { ApiError } from '../api/http';
import { DAYS_OF_WEEK } from '../api/types';
import type { DayOfWeekName } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { Button } from '../components/Button';
import { NumberField, PasswordField, SelectField, TextField } from '../components/fields';
import { Modal } from '../components/Modal';
import { useToast } from '../components/Toast';
import { formatDateTime, weekdayOrder } from '../lib/dates';
import { passwordHelperText, validatePassword } from '../lib/password';
import { invalidateTimeData, useAppConfig, useCountries, useSubdivisions } from '../lib/queries';
import { isValidWeeklyHours } from '../lib/weeklyHours';
import '../styles/settings.css';

const DAY_LABELS: Record<DayOfWeekName, string> = {
  MONDAY: 'Monday',
  TUESDAY: 'Tuesday',
  WEDNESDAY: 'Wednesday',
  THURSDAY: 'Thursday',
  FRIDAY: 'Friday',
  SATURDAY: 'Saturday',
  SUNDAY: 'Sunday',
};

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Something went wrong. Please try again.';
}

export function SettingsPage() {
  useEffect(() => {
    document.title = 'Settings | chroniqo';
  }, []);

  return (
    <div className="page">
      <div className="page__header">
        <h1 className="page__title">Settings</h1>
      </div>
      <div className="settings">
        <div className="settings__col">
          <ProfileSection />
        </div>
        <div className="settings__col">
          <PasswordSection />
          <AccountSection />
          <DangerSection />
        </div>
      </div>
    </div>
  );
}

function ProfileSection() {
  const { user, refreshUser } = useAuth();
  const queryClient = useQueryClient();
  const toast = useToast();
  const workingDaysId = useId();

  const [firstName, setFirstName] = useState(user?.firstName ?? '');
  const [lastName, setLastName] = useState(user?.lastName ?? '');
  const [countryCode, setCountryCode] = useState(user?.countryCode ?? '');
  const [subdivisionCode, setSubdivisionCode] = useState(user?.subdivisionCode ?? '');
  const [weeklyHours, setWeeklyHours] = useState<number | null>(user?.weeklyTargetHours ?? 40);
  const [workingDays, setWorkingDays] = useState<DayOfWeekName[]>(user?.workingDays ?? []);
  const [errors, setErrors] = useState<{ firstName?: string; lastName?: string }>({});
  const [saving, setSaving] = useState(false);

  // Restore the saved values when the profile arrives after the first render.
  const loadedEmail = user?.email;
  useEffect(() => {
    if (!user) return;
    setFirstName(user.firstName);
    setLastName(user.lastName);
    setCountryCode(user.countryCode ?? '');
    setSubdivisionCode(user.subdivisionCode ?? '');
    setWeeklyHours(user.weeklyTargetHours);
    setWorkingDays(user.workingDays);
    // Keyed on the account so a refresh after saving does not overwrite edits.
  }, [loadedEmail]);

  const countries = useCountries();
  const subdivisions = useSubdivisions(countryCode || null);
  const subdivisionOptions = (subdivisions.data ?? []).map((s) => ({ value: s.code, label: s.name }));
  const showSubdivisions = !!countryCode && subdivisionOptions.length > 0;

  const toggleDay = (day: DayOfWeekName, checked: boolean) => {
    setWorkingDays((current) => (checked ? [...current, day] : current.filter((d) => d !== day)));
  };

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const nextErrors: typeof errors = {};
    if (!firstName.trim()) nextErrors.firstName = 'First name is required';
    if (!lastName.trim()) nextErrors.lastName = 'Last name is required';
    setErrors(nextErrors);
    if (nextErrors.firstName || nextErrors.lastName) return;

    if (!isValidWeeklyHours(weeklyHours)) {
      toast.error('Weekly target hours must be between 0 and 80 with at most one decimal place (e.g. 37.5).');
      return;
    }
    if (workingDays.length === 0) {
      toast.error('Please select at least one working day.');
      return;
    }

    setSaving(true);
    try {
      await api.updateProfile({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        weeklyTargetHours: weeklyHours,
        workingDays: DAYS_OF_WEEK.filter((d) => workingDays.includes(d)),
        countryCode: countryCode || null,
        subdivisionCode: showSubdivisions && subdivisionCode ? subdivisionCode : null,
      });
      toast.success('Profile updated successfully!', 3000);
      await Promise.all([refreshUser(), invalidateTimeData(queryClient)]);
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="card settings__section" aria-labelledby="settings-profile">
      <h3 className="settings__heading" id="settings-profile">
        Profile Information
      </h3>
      <form className="form" onSubmit={onSubmit} noValidate>
        <TextField
          label="First Name"
          value={firstName}
          onChange={(e) => setFirstName(e.target.value)}
          error={errors.firstName}
          autoComplete="given-name"
          required
        />
        <TextField
          label="Last Name"
          value={lastName}
          onChange={(e) => setLastName(e.target.value)}
          error={errors.lastName}
          autoComplete="family-name"
          required
        />
        <SelectField
          label="Country"
          value={countryCode}
          onChange={(e) => {
            setCountryCode(e.target.value);
            setSubdivisionCode('');
          }}
          options={(countries.data ?? []).map((c) => ({ value: c.code, label: c.name }))}
          placeholder="None — disable automatic public holidays"
          helperText="Used for automatic public holiday detection"
        />
        {showSubdivisions && (
          <SelectField
            label="State / Region"
            value={subdivisionCode}
            onChange={(e) => setSubdivisionCode(e.target.value)}
            options={subdivisionOptions}
            placeholder="Select your state / region (optional)"
          />
        )}
        <TextField label="Email" value={user?.email ?? ''} readOnly helperText="Email cannot be changed" />
        <NumberField
          label="Weekly Target Hours"
          value={weeklyHours}
          onChange={setWeeklyHours}
          min={0}
          max={80}
          step={0.5}
          inputStep={0.1}
          helperText="0–80 hours per week, e.g. 37.5 (0 = no target)"
        />
        <div className="settings__group" role="group" aria-labelledby={workingDaysId}>
          <span className="field__label" id={workingDaysId}>
            Working Days
          </span>
          <div className="day-chips">
            {weekdayOrder(user?.firstDayOfWeek).map((i) => {
              const day = DAYS_OF_WEEK[i];
              return (
                <label key={day} className="day-chip">
                  <input
                    type="checkbox"
                    aria-label={DAY_LABELS[day]}
                    checked={workingDays.includes(day)}
                    onChange={(e) => toggleDay(day, e.target.checked)}
                  />
                  <span aria-hidden="true">{DAY_LABELS[day].slice(0, 3)}</span>
                </label>
              );
            })}
          </div>
          <span className="field__helper">Select the days you are scheduled to work</span>
        </div>
        <div className="settings__actions">
          <Button type="submit" variant="primary" loading={saving}>
            Save Changes
          </Button>
        </div>
      </form>
    </section>
  );
}

function PasswordSection() {
  const toast = useToast();
  const { passwordPolicy } = useAppConfig();

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState<{ current?: string; next?: string; confirm?: string }>({});
  const [saving, setSaving] = useState(false);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const nextErrors: typeof errors = {};
    if (!currentPassword) nextErrors.current = 'Current password is required';
    const policyError = validatePassword(newPassword, passwordPolicy);
    if (policyError) nextErrors.next = policyError;
    if (!confirmPassword) nextErrors.confirm = 'Please confirm your new password';
    else if (confirmPassword !== newPassword) nextErrors.confirm = 'Passwords do not match';
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    setSaving(true);
    try {
      await api.changePassword(currentPassword, newPassword);
      toast.success('Password changed successfully!', 3000);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (error) {
      const message = errorMessage(error);
      if (error instanceof ApiError && (error.errors?.newPassword || error.errors?.currentPassword)) {
        setErrors({ next: error.errors.newPassword, current: error.errors.currentPassword });
      } else if (error instanceof ApiError && /current password/i.test(message)) {
        setErrors({ current: message });
      } else {
        toast.error(message);
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="card settings__section" aria-labelledby="settings-password">
      <h3 className="settings__heading" id="settings-password">
        Change Password
      </h3>
      <form className="form" onSubmit={onSubmit} noValidate>
        <PasswordField
          label="Current Password"
          value={currentPassword}
          onChange={(e) => setCurrentPassword(e.target.value)}
          error={errors.current}
          autoComplete="current-password"
          required
        />
        <PasswordField
          label="New Password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          error={errors.next}
          helperText={passwordHelperText(passwordPolicy)}
          autoComplete="new-password"
          required
        />
        <PasswordField
          label="Confirm New Password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          error={errors.confirm}
          autoComplete="new-password"
          required
        />
        <div className="settings__actions">
          <Button type="submit" variant="primary" loading={saving}>
            Change Password
          </Button>
        </div>
      </form>
    </section>
  );
}

function AccountSection() {
  const { user } = useAuth();

  return (
    <section className="card settings__section" aria-labelledby="settings-account">
      <h3 className="settings__heading" id="settings-account">
        Account Information
      </h3>
      <div className="settings__stats">
        <div className="settings__stat">
          <div className="settings__stat-label">Member Since</div>
          <div className="settings__stat-value">{formatDateTime(user?.createdAt, 'Unknown')}</div>
        </div>
        <div className="settings__stat">
          <div className="settings__stat-label">Last Login</div>
          <div className="settings__stat-value">{formatDateTime(user?.lastLoginAt, 'Never')}</div>
        </div>
      </div>
    </section>
  );
}

function DangerSection() {
  const { clearSession } = useAuth();
  const toast = useToast();
  const [open, setOpen] = useState(false);
  const [confirmation, setConfirmation] = useState('');
  const [password, setPassword] = useState('');
  const [deleting, setDeleting] = useState(false);

  const close = () => {
    if (deleting) return;
    setOpen(false);
    setConfirmation('');
    setPassword('');
  };

  const onDelete = async () => {
    if (confirmation !== 'DELETE') {
      toast.error('Please type DELETE to confirm account removal.');
      return;
    }
    if (!password) {
      toast.error('Please enter your current password.');
      return;
    }

    setDeleting(true);
    try {
      await api.deleteAccount(password);
      clearSession();
      // Full reload so cookies and in-memory state are reset.
      window.location.assign('/login?accountDeleted');
    } catch (error) {
      toast.error(errorMessage(error));
      setDeleting(false);
    }
  };

  return (
    <section className="card settings__section settings__danger" aria-labelledby="settings-danger">
      <h3 className="settings__heading" id="settings-danger">
        Danger Zone
      </h3>
      <p className="settings__danger-text">
        Delete your account permanently. This removes your profile, time entries, absences, and saved settings.
      </p>
      <div className="settings__actions">
        <Button variant="error" onClick={() => setOpen(true)}>
          Delete Account
        </Button>
      </div>

      <Modal
        open={open}
        onClose={close}
        title="Delete Account"
        narrow
        footer={
          <>
            <Button variant="tertiary" onClick={close} disabled={deleting}>
              Cancel
            </Button>
            <Button variant="error" onClick={() => void onDelete()} loading={deleting}>
              Delete Account
            </Button>
          </>
        }
      >
        <form
          className="form"
          onSubmit={(e) => {
            e.preventDefault();
            void onDelete();
          }}
        >
          <p className="settings__dialog-text">
            This action cannot be undone. Type DELETE and enter your current password to permanently remove your
            account.
          </p>
          <TextField
            label="Confirmation"
            value={confirmation}
            onChange={(e) => setConfirmation(e.target.value)}
            placeholder="DELETE"
            helperText="Type DELETE in uppercase to confirm"
            autoComplete="off"
          />
          <PasswordField
            label="Current Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />
        </form>
      </Modal>
    </section>
  );
}
