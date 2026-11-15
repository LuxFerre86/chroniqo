import { useEffect, useId, useState } from 'react';
import type { FormEvent } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import { ApiError } from '../api/http';
import { DAYS_OF_WEEK } from '../api/types';
import type { DayOfWeekName } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { Button } from '../components/Button';
import { Checkbox, NumberField, PasswordField, SelectField, TextField } from '../components/fields';
import { Modal } from '../components/Modal';
import { useToast } from '../components/Toast';
import { formatDateTime, formatShortDate, weekdayOrder } from '../lib/dates';
import { passwordHelperText, validatePassword } from '../lib/password';
import {
  getCurrentSubscription,
  isPushSupported,
  subscribeThisDevice,
  toSubscriptionInput,
} from '../lib/push';
import { invalidateTimeData, queryKeys, useAppConfig, useCountries, useSubdivisions } from '../lib/queries';
import { isValidWeeklyHours } from '../lib/weeklyHours';
import { getLanguage, LANGUAGES, translate, useT } from '../i18n';
import type { Language } from '../i18n';
import '../styles/settings.css';

const LANGUAGE_NAMES: Record<Language, string> = { en: 'English', de: 'Deutsch' };

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : translate('settings.error.generic');
}

export function SettingsPage() {
  const { t } = useT();
  useEffect(() => {
    document.title = t('settings.pageTitle');
  }, [t]);

  return (
    <div className="page">
      <div className="page__header">
        <h1 className="page__title">{t('settings.title')}</h1>
      </div>
      <div className="settings">
        <div className="settings__col">
          <ProfileSection />
        </div>
        <div className="settings__col">
          <VacationSection />
          <CustomHolidaysSection />
          <RemindersSection />
          <PasswordSection />
          <AccountSection />
          <DangerSection />
        </div>
      </div>
    </div>
  );
}

function RemindersSection() {
  const { t } = useT();
  const { pushPublicKey } = useAppConfig();
  const toast = useToast();
  const queryClient = useQueryClient();
  const supported = isPushSupported();
  const available = supported && !!pushPublicKey;
  const settings = useQuery({
    queryKey: queryKeys.reminderSettings,
    queryFn: () => api.getReminderSettings(),
    enabled: available,
  });
  const [deviceActive, setDeviceActive] = useState(false);
  const [startEnabled, setStartEnabled] = useState(true);
  const [startTime, setStartTime] = useState('09:00');
  const [endEnabled, setEndEnabled] = useState(true);
  const [endAfterHours, setEndAfterHours] = useState('10');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!available) return;
    void (async () => {
      const existing = await getCurrentSubscription();
      setDeviceActive(existing !== null);
      if (existing) await api.subscribePush(toSubscriptionInput(existing)).catch(() => undefined);
    })();
  }, [available]);

  useEffect(() => {
    if (!settings.data) return;
    setStartEnabled(settings.data.startEnabled);
    setStartTime(settings.data.startTime.slice(0, 5));
    setEndEnabled(settings.data.endEnabled);
    setEndAfterHours(String(settings.data.endAfterHours));
  }, [settings.data]);

  if (!available) return null;

  async function enableDevice() {
    setBusy(true);
    try {
      const permission = await Notification.requestPermission();
      if (permission !== 'granted') {
        toast.error(t('settings.reminders.blocked'));
        return;
      }
      const subscription = await subscribeThisDevice(pushPublicKey as string);
      await api.subscribePush(toSubscriptionInput(subscription));
      setDeviceActive(true);
      toast.success(t('settings.reminders.enabled'), 3000);
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  async function disableDevice() {
    setBusy(true);
    try {
      const subscription = await getCurrentSubscription();
      if (subscription) {
        await api.unsubscribePush(subscription.endpoint);
        await subscription.unsubscribe();
      }
      setDeviceActive(false);
      toast.success(t('settings.reminders.disabled'), 3000);
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  async function handleSave(event: FormEvent) {
    event.preventDefault();
    const hours = Number(endAfterHours);
    if (!startTime || !Number.isInteger(hours) || hours < 1 || hours > 24) {
      toast.error(t('settings.reminders.invalid'));
      return;
    }
    setBusy(true);
    try {
      await api.updateReminderSettings({ startEnabled, startTime, endEnabled, endAfterHours: hours });
      await queryClient.invalidateQueries({ queryKey: queryKeys.reminderSettings });
      toast.success(t('settings.reminders.saved'), 3000);
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card settings__section" aria-labelledby="settings-reminders">
      <h3 id="settings-reminders" className="settings__heading">
        {t('settings.reminders.title')}
      </h3>
      <p className="settings__hint">{t('settings.reminders.hint')}</p>
      <div className="settings__actions">
        {deviceActive ? (
          <Button variant="outlined" disabled={busy} onClick={() => void disableDevice()}>
            {t('settings.reminders.disableDevice')}
          </Button>
        ) : (
          <Button variant="primary" disabled={busy} onClick={() => void enableDevice()}>
            {t('settings.reminders.enableDevice')}
          </Button>
        )}
      </div>
      <form className="form" onSubmit={(e) => void handleSave(e)} noValidate>
        <Checkbox
          label={t('settings.reminders.startEnabled')}
          checked={startEnabled}
          onChange={(e) => setStartEnabled(e.target.checked)}
        />
        <TextField
          label={t('settings.reminders.startTime')}
          type="time"
          value={startTime}
          disabled={!startEnabled}
          onChange={(e) => setStartTime(e.target.value)}
          helperText={t('settings.reminders.startTimeHelper')}
        />
        <Checkbox
          label={t('settings.reminders.endEnabled')}
          checked={endEnabled}
          onChange={(e) => setEndEnabled(e.target.checked)}
        />
        <TextField
          label={t('settings.reminders.endAfterHours')}
          type="number"
          min={1}
          max={24}
          step={1}
          value={endAfterHours}
          disabled={!endEnabled}
          onChange={(e) => setEndAfterHours(e.target.value)}
        />
        <div className="settings__actions">
          <Button type="submit" variant="primary" disabled={busy}>
            {t('settings.reminders.save')}
          </Button>
        </div>
      </form>
    </section>
  );
}

function CustomHolidaysSection() {
  const { t } = useT();
  const toast = useToast();
  const queryClient = useQueryClient();
  const holidays = useQuery({ queryKey: queryKeys.customHolidays, queryFn: () => api.getCustomHolidays() });
  const [name, setName] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [recurring, setRecurring] = useState(false);
  const [busy, setBusy] = useState(false);

  async function refresh() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.customHolidays }),
      invalidateTimeData(queryClient),
    ]);
  }

  async function handleAdd(event: FormEvent) {
    event.preventDefault();
    if (!name.trim() || !startDate) {
      toast.error(t('settings.holidays.required'));
      return;
    }
    setBusy(true);
    try {
      await api.createCustomHoliday({ name: name.trim(), startDate, endDate: endDate || startDate, recurring });
      toast.success(t('settings.holidays.added'), 3000);
      setName('');
      setStartDate('');
      setEndDate('');
      setRecurring(false);
      await refresh();
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  async function handleDelete(id: string) {
    setBusy(true);
    try {
      await api.deleteCustomHoliday(id);
      await refresh();
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  const items = holidays.data ?? [];
  return (
    <section className="card settings__section" aria-labelledby="settings-custom-holidays">
      <h3 id="settings-custom-holidays" className="settings__heading">
        {t('settings.holidays.title')}
      </h3>
      <p className="settings__hint">{t('settings.holidays.hint')}</p>
      {items.length > 0 && (
        <ul className="holiday-list" aria-label={t('settings.holidays.listLabel')}>
          {items.map((h) => (
            <li key={h.id} className="holiday-list__item">
              <div>
                <div className="holiday-list__name">{h.name}</div>
                <div className="holiday-list__dates">
                  {h.startDate === h.endDate
                    ? formatShortDate(h.startDate)
                    : `${formatShortDate(h.startDate)} – ${formatShortDate(h.endDate)}`}
                  {h.recurring && t('settings.holidays.everyYear')}
                </div>
              </div>
              <Button
                variant="tertiary"
                disabled={busy}
                aria-label={t('settings.holidays.deleteLabel', { name: h.name })}
                onClick={() => void handleDelete(h.id)}
              >
                {t('settings.holidays.delete')}
              </Button>
            </li>
          ))}
        </ul>
      )}
      <form className="form" onSubmit={(e) => void handleAdd(e)} noValidate>
        <TextField
          label={t('settings.holidays.name')}
          required
          maxLength={100}
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <TextField
          label={t('settings.holidays.startDate')}
          type="date"
          required
          value={startDate}
          onChange={(e) => {
            setStartDate(e.target.value);
            if (endDate && endDate < e.target.value) setEndDate('');
          }}
        />
        <TextField
          label={t('settings.holidays.endDate')}
          type="date"
          min={startDate || undefined}
          value={endDate}
          helperText={t('settings.holidays.endDateHelper')}
          onChange={(e) => setEndDate(e.target.value)}
        />
        <Checkbox
          label={t('settings.holidays.recurring')}
          checked={recurring}
          onChange={(e) => setRecurring(e.target.checked)}
        />
        <div className="settings__actions">
          <Button type="submit" variant="primary" disabled={busy}>
            {t('settings.holidays.add')}
          </Button>
        </div>
      </form>
    </section>
  );
}

function VacationSection() {
  const { t } = useT();
  const toast = useToast();
  const queryClient = useQueryClient();
  const currentYear = new Date().getFullYear();
  const [year, setYear] = useState(currentYear);
  const [days, setDays] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const account = useQuery({ queryKey: queryKeys.vacation(year), queryFn: () => api.getVacationAccount(year) });

  const loadedEntitlement = account.data?.entitlement;
  useEffect(() => {
    if (loadedEntitlement !== undefined) setDays(loadedEntitlement);
  }, [loadedEntitlement, year]);

  const years = [currentYear - 1, currentYear, currentYear + 1, currentYear + 2];

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (days === null || !Number.isInteger(days) || days < 0 || days > 366) {
      toast.error(t('settings.vacation.invalid'));
      return;
    }
    setSaving(true);
    try {
      await api.setVacationEntitlement(year, days);
      toast.success(t('settings.vacation.saved'), 3000);
      await queryClient.invalidateQueries({ queryKey: ['vacation'] });
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="card settings__section" aria-labelledby="settings-vacation">
      <h3 id="settings-vacation" className="settings__heading">
        {t('settings.vacation.title')}
      </h3>
      <form className="form" onSubmit={(e) => void handleSubmit(e)} noValidate>
        <SelectField
          label={t('settings.vacation.year')}
          value={String(year)}
          onChange={(e) => setYear(Number(e.target.value))}
          options={years.map((y) => ({ value: String(y), label: String(y) }))}
        />
        <NumberField
          label={t('settings.vacation.entitlement')}
          value={days}
          onChange={setDays}
          min={0}
          max={366}
          step={1}
          helperText={t('settings.vacation.entitlementHelper')}
        />
        <div className="settings__actions">
          <Button type="submit" variant="primary" disabled={saving}>
            {saving ? t('settings.vacation.saving') : t('settings.vacation.save')}
          </Button>
        </div>
      </form>
    </section>
  );
}

function ProfileSection() {
  const { t } = useT();
  const language = getLanguage();
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

  const onLanguageChange = async (next: Language) => {
    if (!user || next === language) return;
    try {
      await api.updateProfile({
        firstName: user.firstName,
        lastName: user.lastName,
        weeklyTargetHours: user.weeklyTargetHours,
        workingDays: user.workingDays,
        countryCode: user.countryCode,
        subdivisionCode: user.subdivisionCode,
        language: next,
      });
      // The refreshed user carries the new language; I18nProvider then remounts the tree.
      await refreshUser();
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const nextErrors: typeof errors = {};
    if (!firstName.trim()) nextErrors.firstName = t('settings.profile.firstNameRequired');
    if (!lastName.trim()) nextErrors.lastName = t('settings.profile.lastNameRequired');
    setErrors(nextErrors);
    if (nextErrors.firstName || nextErrors.lastName) return;

    if (!isValidWeeklyHours(weeklyHours)) {
      toast.error(t('settings.profile.weeklyHoursInvalid'));
      return;
    }
    if (workingDays.length === 0) {
      toast.error(t('settings.profile.workingDaysRequired'));
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
      toast.success(t('settings.profile.saved'), 3000);
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
        {t('settings.profile.title')}
      </h3>
      <form className="form" onSubmit={onSubmit} noValidate>
        <TextField
          label={t('settings.profile.firstName')}
          value={firstName}
          onChange={(e) => setFirstName(e.target.value)}
          error={errors.firstName}
          autoComplete="given-name"
          required
        />
        <TextField
          label={t('settings.profile.lastName')}
          value={lastName}
          onChange={(e) => setLastName(e.target.value)}
          error={errors.lastName}
          autoComplete="family-name"
          required
        />
        <SelectField
          label={t('settings.profile.country')}
          value={countryCode}
          onChange={(e) => {
            setCountryCode(e.target.value);
            setSubdivisionCode('');
          }}
          options={(countries.data ?? []).map((c) => ({ value: c.code, label: c.name }))}
          placeholder={t('settings.profile.countryPlaceholder')}
          helperText={t('settings.profile.countryHelper')}
        />
        {showSubdivisions && (
          <SelectField
            label={t('settings.profile.subdivision')}
            value={subdivisionCode}
            onChange={(e) => setSubdivisionCode(e.target.value)}
            options={subdivisionOptions}
            placeholder={t('settings.profile.subdivisionPlaceholder')}
          />
        )}
        <SelectField
          label={t('settings.language.label')}
          value={language}
          onChange={(e) => void onLanguageChange(e.target.value as Language)}
          options={LANGUAGES.map((l) => ({ value: l, label: LANGUAGE_NAMES[l] }))}
        />
        <TextField
          label={t('settings.profile.email')}
          value={user?.email ?? ''}
          readOnly
          helperText={t('settings.profile.emailHelper')}
        />
        <NumberField
          label={t('settings.profile.weeklyHours')}
          value={weeklyHours}
          onChange={setWeeklyHours}
          min={0}
          max={80}
          step={0.5}
          inputStep={0.1}
          helperText={t('settings.profile.weeklyHoursHelper')}
        />
        <div className="settings__group" role="group" aria-labelledby={workingDaysId}>
          <span className="field__label" id={workingDaysId}>
            {t('settings.profile.workingDays')}
          </span>
          <div className="day-chips">
            {weekdayOrder(user?.firstDayOfWeek).map((i) => {
              const day = DAYS_OF_WEEK[i];
              return (
                <label key={day} className="day-chip">
                  <input
                    type="checkbox"
                    aria-label={t(`settings.day.${day}`)}
                    checked={workingDays.includes(day)}
                    onChange={(e) => toggleDay(day, e.target.checked)}
                  />
                  <span aria-hidden="true">{t(`settings.dayShort.${day}`)}</span>
                </label>
              );
            })}
          </div>
          <span className="field__helper">{t('settings.profile.workingDaysHelper')}</span>
        </div>
        <div className="settings__actions">
          <Button type="submit" variant="primary" loading={saving}>
            {t('settings.profile.save')}
          </Button>
        </div>
      </form>
    </section>
  );
}

function PasswordSection() {
  const { t } = useT();
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
    if (!currentPassword) nextErrors.current = t('settings.password.currentRequired');
    const policyError = validatePassword(newPassword, passwordPolicy);
    if (policyError) nextErrors.next = policyError;
    if (!confirmPassword) nextErrors.confirm = t('settings.password.confirmRequired');
    else if (confirmPassword !== newPassword) nextErrors.confirm = t('settings.password.mismatch');
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    setSaving(true);
    try {
      await api.changePassword(currentPassword, newPassword);
      toast.success(t('settings.password.saved'), 3000);
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
        {t('settings.password.title')}
      </h3>
      <form className="form" onSubmit={onSubmit} noValidate>
        <PasswordField
          label={t('settings.password.current')}
          value={currentPassword}
          onChange={(e) => setCurrentPassword(e.target.value)}
          error={errors.current}
          autoComplete="current-password"
          required
        />
        <PasswordField
          label={t('settings.password.new')}
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          error={errors.next}
          helperText={passwordHelperText(passwordPolicy)}
          autoComplete="new-password"
          required
        />
        <PasswordField
          label={t('settings.password.confirm')}
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          error={errors.confirm}
          autoComplete="new-password"
          required
        />
        <div className="settings__actions">
          <Button type="submit" variant="primary" loading={saving}>
            {t('settings.password.submit')}
          </Button>
        </div>
      </form>
    </section>
  );
}

function AccountSection() {
  const { t } = useT();
  const { user } = useAuth();

  return (
    <section className="card settings__section" aria-labelledby="settings-account">
      <h3 className="settings__heading" id="settings-account">
        {t('settings.account.title')}
      </h3>
      <div className="settings__stats">
        <div className="settings__stat">
          <div className="settings__stat-label">{t('settings.account.memberSince')}</div>
          <div className="settings__stat-value">{formatDateTime(user?.createdAt, t('settings.account.unknown'))}</div>
        </div>
        <div className="settings__stat">
          <div className="settings__stat-label">{t('settings.account.lastLogin')}</div>
          <div className="settings__stat-value">{formatDateTime(user?.lastLoginAt, t('settings.account.never'))}</div>
        </div>
      </div>
    </section>
  );
}

function DangerSection() {
  const { t } = useT();
  const confirmWord = t('settings.danger.confirmWord');
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
    if (confirmation !== confirmWord) {
      toast.error(t('settings.danger.typeWord', { word: confirmWord }));
      return;
    }
    if (!password) {
      toast.error(t('settings.danger.needPassword'));
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
        {t('settings.danger.title')}
      </h3>
      <p className="settings__danger-text">{t('settings.danger.text')}</p>
      <div className="settings__actions">
        <Button variant="error" onClick={() => setOpen(true)}>
          {t('settings.danger.delete')}
        </Button>
      </div>

      <Modal
        open={open}
        onClose={close}
        title={t('settings.danger.delete')}
        narrow
        footer={
          <>
            <Button variant="tertiary" onClick={close} disabled={deleting}>
              {t('settings.danger.cancel')}
            </Button>
            <Button variant="error" onClick={() => void onDelete()} loading={deleting}>
              {t('settings.danger.delete')}
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
          <p className="settings__dialog-text">{t('settings.danger.dialogText', { word: confirmWord })}</p>
          <TextField
            label={t('settings.danger.confirmation')}
            value={confirmation}
            onChange={(e) => setConfirmation(e.target.value)}
            placeholder={confirmWord}
            helperText={t('settings.danger.confirmationHelper', { word: confirmWord })}
            autoComplete="off"
          />
          <PasswordField
            label={t('settings.danger.currentPassword')}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />
        </form>
      </Modal>
    </section>
  );
}
