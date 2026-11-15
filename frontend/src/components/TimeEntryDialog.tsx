import { useEffect, useId, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import { ApiError } from '../api/http';
import type { AbsenceType, TimeEntry } from '../api/types';
import { getAbsenceTypes, absenceLabel } from '../lib/absence';
import { countDays, formatShortDate, todayIso } from '../lib/dates';
import { formatMinutes, isTime24h, normalizeTime, trimSeconds, worked } from '../lib/format';
import { invalidateTimeData, queryKeys } from '../lib/queries';
import { translate, translatePlural, useT } from '../i18n';
import { Button } from './Button';
import { NumberField, SelectField, TextAreaField, TextField } from './fields';
import { Icon } from './Icon';
import { Modal } from './Modal';
import { useToast } from './Toast';
import '../styles/dialog.css';

type Mode = 'WORKING' | AbsenceType;

const modeSwitch = (): { key: 'WORKING' | 'ABSENCE'; label: string }[] => [
  { key: 'WORKING', label: translate('entry.mode.working') },
  { key: 'ABSENCE', label: translate('entry.mode.absence') },
];

function modeName(mode: Mode): string {
  return mode === 'WORKING' ? translate('entry.mode.working') : absenceLabel(mode);
}

const DEFAULT_BREAK_MINUTES = 30;

class ValidationError extends Error {}

function errorMessage(error: unknown): string {
  if (error instanceof ValidationError) return error.message;
  return error instanceof ApiError && error.message ? error.message : translate('entry.error.generic');
}

function bookingHeadline(entry: TimeEntry): string {
  return `${trimSeconds(entry.startTime) || '--:--'} - ${entry.endTime ? trimSeconds(entry.endTime) : translate('entry.open')}`;
}

function bookingDetails(entry: TimeEntry): string {
  const parts: string[] = [];
  if (entry.breakMinutes) parts.push(translate('entry.break', { minutes: entry.breakMinutes }));
  const notes = entry.notes?.trim();
  if (notes) parts.push(notes.length > 40 ? `${notes.slice(0, 40)}…` : notes);
  return parts.length > 0 ? parts.join(' · ') : translate('entry.noDetails');
}

function summarize(entries: TimeEntry[]): string {
  if (entries.length === 0) return translate('entry.noBookings');
  const minutes = entries.reduce((sum, e) => sum + worked(e.startTime, e.endTime, e.breakMinutes), 0);
  const open = entries.filter((e) => !e.endTime).length;
  const parts = [
    translatePlural('entry.booking', entries.length),
    translate('entry.worked', { value: formatMinutes(minutes) }),
  ];
  if (open > 0) parts.push(translatePlural('entry.openBooking', open));
  return parts.join(' · ');
}

interface EditorState {
  id: string | null;
  date: string;
  startTime: string;
  endTime: string;
  breakMinutes: number | null;
  notes: string;
}

function newEditor(date: string): EditorState {
  return { id: null, date, startTime: '', endTime: '', breakMinutes: DEFAULT_BREAK_MINUTES, notes: '' };
}

function editorFor(entry: TimeEntry): EditorState {
  return {
    id: entry.id,
    date: entry.date,
    startTime: trimSeconds(entry.startTime),
    endTime: trimSeconds(entry.endTime),
    breakMinutes: entry.breakMinutes,
    notes: entry.notes ?? '',
  };
}

interface DialogProps {
  date: string;
  onClose: () => void;
}

function TimeEntryDialogContent({ date, onClose }: DialogProps) {
  const queryClient = useQueryClient();
  const { t } = useT();
  const toast = useToast();
  const formId = useId();

  const [mode, setMode] = useState<Mode | null>(null);
  const [lastAbsenceType, setLastAbsenceType] = useState<AbsenceType>('VACATION');
  const [listDate, setListDate] = useState(date);
  const [editor, setEditor] = useState<EditorState>(() => newEditor(date));
  const [absenceStart, setAbsenceStart] = useState(date);
  const [absenceEnd, setAbsenceEnd] = useState(date);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [dateEditable, setDateEditable] = useState(false);
  const [confirmRemove, setConfirmRemove] = useState(false);
  const today = todayIso();

  const entriesQuery = useQuery({
    queryKey: queryKeys.timeEntries(listDate, listDate),
    queryFn: () => api.getTimeEntries(listDate, listDate),
  });
  const absencesQuery = useQuery({
    queryKey: queryKeys.absences(date, date),
    queryFn: () => api.getAbsences(date, date),
  });
  const absenceRangeEnd = absenceEnd || absenceStart;
  const absenceRangeValid = !!absenceStart && absenceRangeEnd >= absenceStart;
  const absenceRangeQuery = useQuery({
    queryKey: queryKeys.absences(absenceStart, absenceRangeEnd),
    queryFn: () => api.getAbsences(absenceStart, absenceRangeEnd),
    enabled: absenceRangeValid,
  });
  const removableCount =
    mode === null || mode === 'WORKING'
      ? 0
      : (absenceRangeQuery.data ?? []).filter((a) => a.type === mode).length;

  useEffect(() => {
    setConfirmRemove(false);
  }, [mode, absenceStart, absenceEnd]);

  const entries = useMemo(
    () =>
      [...(entriesQuery.data ?? [])].sort((a, b) => trimSeconds(a.startTime).localeCompare(trimSeconds(b.startTime))),
    [entriesQuery.data],
  );

  const showList = entriesQuery.isError || entries.length > 0;
  const initiallyLoaded = !entriesQuery.isPending && !absencesQuery.isPending;
  useEffect(() => {
    if (mode !== null || !initiallyLoaded) return;
    const absence = (absencesQuery.data ?? []).find((a) => a.date === date);
    const hasEntries = (entriesQuery.data ?? []).length > 0;
    const initial: Mode = !hasEntries && absence ? absence.type : date > todayIso() ? 'VACATION' : 'WORKING';
    setMode(initial);
    if (initial !== 'WORKING') setLastAbsenceType(initial);
  }, [mode, initiallyLoaded, absencesQuery.data, entriesQuery.data, date]);

  const patchEditor = (patch: Partial<EditorState>) => {
    setError(null);
    setEditor((current) => ({ ...current, ...patch }));
  };

  const startNewBooking = (forDate: string = listDate) => {
    setError(null);
    setEditor(newEditor(forDate));
  };

  const selectEntry = (entry: TimeEntry) => {
    setError(null);
    setEditor(editorFor(entry));
  };

  const changeEditorDate = (value: string) => {
    if (editor.id === null) {
      // New booking: the date field selects which day's bookings are shown.
      setError(null);
      setEditor((current) => ({ ...current, date: value }));
      if (value) setListDate(value);
    } else {
      patchEditor({ date: value });
    }
  };

  const changeMode = (next: Mode) => {
    setError(null);
    setMode(next);
    if (next !== 'WORKING') setLastAbsenceType(next);
  };

  const showAbsenceTab = (key: 'WORKING' | 'ABSENCE') =>
    key === 'WORKING' ? mode === 'WORKING' : mode !== 'WORKING';

  const changeAbsenceStart = (value: string) => {
    setError(null);
    if (absenceEnd === '' || absenceEnd === absenceStart) setAbsenceEnd(value);
    setAbsenceStart(value);
  };

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setError(null);
    try {
      await action();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const saveWorking = () =>
    run(async () => {
      if (!editor.date) throw new ValidationError(t('entry.validation.dateNull'));
      const startTime = normalizeTime(editor.startTime);
      const endTime = normalizeTime(editor.endTime);
      if (!startTime) throw new ValidationError(t('entry.validation.startMandatory'));
      if (!isTime24h(startTime) || (endTime && !isTime24h(endTime))) {
        throw new ValidationError(t('entry.validation.timeFormat'));
      }
      const payload = {
        date: editor.date,
        startTime,
        endTime: endTime || null,
        breakMinutes: editor.breakMinutes,
        notes: editor.notes.trim() || null,
      };
      if (editor.id) await api.updateTimeEntry(editor.id, payload);
      else await api.createTimeEntry(payload);
      toast.success(t('entry.saved'));
      setListDate(editor.date);
      setEditor(newEditor(editor.date));
      await invalidateTimeData(queryClient);
    });

  const deleteBooking = (id: string) =>
    run(async () => {
      await api.deleteTimeEntry(id);
      toast.success(t('entry.deleted'));
      if (editor.id === id) setEditor(newEditor(listDate));
      await invalidateTimeData(queryClient);
    });

  const saveAbsence = (type: AbsenceType) =>
    run(async () => {
      if (!absenceStart) throw new ValidationError(t('entry.validation.startDateMandatory'));
      const end = absenceEnd || absenceStart;
      if (end < absenceStart) throw new ValidationError(t('entry.validation.endBeforeStart'));
      await api.saveAbsence(absenceStart, end, type);
      const { weekdays, weekendDays } = countDays(absenceStart, end);
      const label = absenceLabel(type);
      const days = translatePlural('entry.day', weekdays);
      toast.success(
        weekendDays > 0
          ? t('entry.absenceBooked.skipped', {
              label,
              days,
              weekend: translatePlural('entry.weekendDay', weekendDays),
            })
          : t('entry.absenceBooked', { label, days }),
      );
      await invalidateTimeData(queryClient);
      onClose();
    });

  const removeAbsence = (type: AbsenceType) =>
    run(async () => {
      if (!absenceStart) throw new ValidationError(t('entry.validation.startDateMandatory'));
      const end = absenceEnd || absenceStart;
      if (end < absenceStart) throw new ValidationError(t('entry.validation.endBeforeStart'));
      await api.deleteAbsences(absenceStart, end, type);
      toast.success(t('entry.absenceRemoved', { label: absenceLabel(type), days: translatePlural('entry.day', removableCount) }));
      await invalidateTimeData(queryClient);
      onClose();
    });

  const futureBooking = mode === 'WORKING' && editor.date > today;

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    if (busy || mode === null || futureBooking) return;
    void (mode === 'WORKING' ? saveWorking() : saveAbsence(mode));
  };

  const absenceSummary =
    absenceEnd && absenceEnd !== absenceStart
      ? `${formatShortDate(absenceStart)} – ${formatShortDate(absenceEnd)}`
      : formatShortDate(absenceStart);

  const headerDate = mode === 'WORKING' || mode === null ? listDate : absenceStart || date;
  const summary = mode === null ? '' : mode === 'WORKING' ? summarize(entries) : absenceSummary;

  const title = (
    <div className="dialog-head">
      <div className="dialog-head__row">
        <h2 className="dialog-head__date">{formatShortDate(headerDate)}</h2>
        {mode && <span className="dialog-head__pill">{modeName(mode)}</span>}
      </div>
      <p className="dialog-head__summary">{summary || '\u00a0'}</p>
    </div>
  );

  const footer =
    mode === null ? undefined : (
      <>
        <Button variant="tertiary" onClick={onClose}>
          {t('entry.close')}
        </Button>
        {mode === 'WORKING' && editor.id && (
          <Button variant="error" disabled={busy} onClick={() => void deleteBooking(editor.id as string)}>
            {t('entry.deleteBooking')}
          </Button>
        )}
        {mode !== 'WORKING' && removableCount > 0 && (
          <Button
            variant="error"
            disabled={busy}
            onClick={() => {
              if (confirmRemove) void removeAbsence(mode);
              else setConfirmRemove(true);
            }}
          >
            {confirmRemove
              ? t('entry.confirmRemove', { days: translatePlural('entry.day', removableCount) })
              : t('entry.remove', { label: absenceLabel(mode).toLowerCase() })}
          </Button>
        )}
        <Button
          type="submit"
          form={formId}
          variant="primary"
          className="modal__primary"
          loading={busy}
          disabled={futureBooking}
        >
          {t('entry.save')}
        </Button>
      </>
    );

  return (
    <Modal open onClose={onClose} title={title} footer={footer}>
      {mode === null ? (
        <div className="dialog-loading" role="status">
          <span className="spinner" aria-hidden="true" />
          {t('entry.loading')}
        </div>
      ) : (
        <form id={formId} className="dialog-body" noValidate onSubmit={onSubmit}>
          <div className="dialog-modes" role="group" aria-label={t('entry.modeGroup')}>
            {modeSwitch().map((item) => (
              <Button
                key={item.key}
                variant={showAbsenceTab(item.key) ? 'primary' : 'tertiary'}
                aria-pressed={showAbsenceTab(item.key)}
                onClick={() => {
                  if (!showAbsenceTab(item.key)) changeMode(item.key === 'WORKING' ? 'WORKING' : lastAbsenceType);
                }}
              >
                {item.label}
              </Button>
            ))}
          </div>

          {futureBooking && (
            <div className="notice notice--error" role="alert">
              {t('entry.futureBlocked')}
            </div>
          )}

          {error && (
            <div className="notice notice--error" role="alert">
              {error}
            </div>
          )}

          {mode === 'WORKING' ? (
            <div className={`dialog-panels`}>
              {showList && (
                <section className="dialog-panel"                 aria-label={t('entry.bookings')}>
                                  <h3 className="dialog-panel__title">{t('entry.bookings')}</h3>
                  <p className="dialog-panel__hint">{summarize(entries)}</p>
                  {entriesQuery.isPending ? (
                    <div className="dialog-loading" role="status">
                      <span className="spinner" aria-hidden="true" />
                      {t('entry.loading')}
                    </div>
                  ) : entriesQuery.isError ? (
                    <div className="notice notice--error" role="alert">
                      {t('entry.bookingsLoadError')}
                    </div>
                  ) : (
                    <ul className="bookings">
                      {entries.map((entry) => {
                        const selected = editor.id === entry.id;
                        return (
                          <li key={entry.id} className={`booking${selected ? ' booking--selected' : ''}`}>
                            <button
                              type="button"
                              className="booking__main"
                              aria-pressed={selected}
                              onClick={() => selectEntry(entry)}
                            >
                              <span className="booking__time">{bookingHeadline(entry)}</span>
                              <span className="booking__details">{bookingDetails(entry)}</span>
                            </button>
                            <div className="booking__actions">
                              <Button
                                icon
                                variant="tertiary"
                                aria-label={t('entry.editBooking', { booking: bookingHeadline(entry) })}
                                onClick={() => selectEntry(entry)}
                              >
                                <Icon name="edit" />
                              </Button>
                              <Button
                                icon
                                variant="tertiary"
                                className="btn--danger-text"
                                aria-label={t('entry.deleteBookingItem', { booking: bookingHeadline(entry) })}
                                disabled={busy}
                                onClick={(e) => {
                                  e.stopPropagation();
                                  void deleteBooking(entry.id);
                                }}
                              >
                                <Icon name="trash" />
                              </Button>
                            </div>
                          </li>
                        );
                      })}
                    </ul>
                  )}
                  <div>
                    <Button variant="tertiary" onClick={() => startNewBooking()}>
                      <Icon name="plus" />
                      {t('entry.addBooking')}
                    </Button>
                  </div>
                </section>
              )}

              <section className="dialog-panel dialog-panel--editor"               aria-label={t('entry.editor')}>
                              <h3 className="dialog-panel__title">{editor.id ? t('entry.editing') : t('entry.newBooking')}</h3>
                <p className="dialog-panel__hint">
                  {editor.id
                                  ? t('entry.editingHint')
                    : showList
                                    ? t('entry.newHint')
                                    : t('entry.firstHint')}
                </p>
                <div className="dialog-grid">
                  {dateEditable ? (
                    <TextField
                      className="dialog-grid__full"
                      label={t('entry.date')}
                      type="date"
                      required
                      value={editor.date}
                      onChange={(e) => changeEditorDate(e.target.value)}
                    />
                  ) : (
                    <div className="dialog-grid__full dialog-date">
                      <span>{t('entry.forDate', { date: formatShortDate(editor.date) })}</span>
                      <Button variant="tertiary" small onClick={() => setDateEditable(true)}>
                        {t('entry.changeDate')}
                      </Button>
                    </div>
                  )}
                  <TextField
                    label={t('entry.startTime')}
                    type="text"
                    inputMode="numeric"
                    placeholder={t('entry.timePlaceholder')}
                    maxLength={5}
                    autoComplete="off"
                    required
                    value={editor.startTime}
                    onChange={(e) => patchEditor({ startTime: e.target.value })}
                    onBlur={() => patchEditor({ startTime: normalizeTime(editor.startTime) })}
                  />
                  <TextField
                    label={t('entry.endTime')}
                    type="text"
                    inputMode="numeric"
                    placeholder={t('entry.timePlaceholder')}
                    maxLength={5}
                    autoComplete="off"
                    value={editor.endTime}
                    onChange={(e) => patchEditor({ endTime: e.target.value })}
                    onBlur={() => patchEditor({ endTime: normalizeTime(editor.endTime) })}
                  />
                  <NumberField
                    className="dialog-grid__full"
                    label={t('entry.break.label')}
                    helperText={t('entry.break.helper')}
                    min={0}
                    max={480}
                    step={5}
                    value={editor.breakMinutes}
                    onChange={(value) => patchEditor({ breakMinutes: value })}
                  />
                  <TextAreaField
                    className="dialog-grid__full"
                    label={t('entry.notes')}
                    helperText={t('entry.notes.helper')}
                    maxLength={500}
                    value={editor.notes}
                    onChange={(e) => patchEditor({ notes: e.target.value })}
                  />
                </div>
              </section>
            </div>
          ) : (
            <section className="dialog-panel"             aria-label={t('entry.absence')}>
                          <h3 className="dialog-panel__title">{t('entry.absence')}</h3>
                          <p className="dialog-panel__hint">
                            {getAbsenceTypes().find((a) => a.type === mode)?.hint ?? t('entry.absence.hint')}
                          </p>
              <div className="dialog-grid dialog-grid--absence">
                <SelectField
                  className="dialog-grid__full"
                  label={t('entry.type')}
                  value={mode}
                  options={getAbsenceTypes().map((a) => ({ value: a.type, label: a.label }))}
                  onChange={(e) => changeMode(e.target.value as AbsenceType)}
                />
                <TextField
                  label={t('entry.startDate')}
                  type="date"
                  required
                  value={absenceStart}
                  onChange={(e) => changeAbsenceStart(e.target.value)}
                />
                <TextField
                  label={t('entry.endDate')}
                  type="date"
                  value={absenceEnd}
                  onChange={(e) => {
                    setError(null);
                    setAbsenceEnd(e.target.value);
                  }}
                />
              </div>
            </section>
          )}
        </form>
      )}
    </Modal>
  );
}

/** Dialog to book working time, sick leave or vacation for a day. Open while `date` (ISO) is set. */
export function TimeEntryDialog({ date, onClose }: { date: string | null; onClose: () => void }) {
  if (!date) return null;
  return <TimeEntryDialogContent key={date} date={date} onClose={onClose} />;
}
