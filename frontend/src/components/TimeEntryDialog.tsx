import { useEffect, useId, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import { ApiError } from '../api/http';
import type { AbsenceType, TimeEntry } from '../api/types';
import { formatShortDate } from '../lib/dates';
import { formatMinutes, trimSeconds, worked } from '../lib/format';
import { invalidateTimeData, queryKeys } from '../lib/queries';
import { Button } from './Button';
import { NumberField, TextAreaField, TextField } from './fields';
import { Icon } from './Icon';
import { Modal } from './Modal';
import { useToast } from './Toast';
import '../styles/dialog.css';

type Mode = 'WORKING' | AbsenceType;

const MODE_NAMES: Record<Mode, string> = {
  WORKING: 'Working time',
  SICK: 'Sick leave',
  VACATION: 'Vacation',
};

const MODE_SWITCH: { mode: Mode; label: string }[] = [
  { mode: 'WORKING', label: 'Working time' },
  { mode: 'SICK', label: 'Sick' },
  { mode: 'VACATION', label: 'Vacation' },
];

const DEFAULT_BREAK_MINUTES = 30;
const GENERIC_ERROR = 'An unexpected error occurred. Please try again.';

class ValidationError extends Error {}

function errorMessage(error: unknown): string {
  if (error instanceof ValidationError) return error.message;
  return error instanceof ApiError && error.message ? error.message : GENERIC_ERROR;
}

function plural(count: number, noun: string): string {
  return `${count} ${noun}${count === 1 ? '' : 's'}`;
}

function bookingHeadline(entry: TimeEntry): string {
  return `${trimSeconds(entry.startTime) || '--:--'} - ${entry.endTime ? trimSeconds(entry.endTime) : 'OPEN'}`;
}

function bookingDetails(entry: TimeEntry): string {
  const parts: string[] = [];
  if (entry.breakMinutes) parts.push(`Break ${entry.breakMinutes}m`);
  const notes = entry.notes?.trim();
  if (notes) parts.push(notes.length > 40 ? `${notes.slice(0, 40)}…` : notes);
  return parts.length > 0 ? parts.join(' · ') : 'No extra details';
}

function summarize(entries: TimeEntry[]): string {
  if (entries.length === 0) return 'No bookings yet for this day';
  const minutes = entries.reduce((sum, e) => sum + worked(e.startTime, e.endTime, e.breakMinutes), 0);
  const open = entries.filter((e) => !e.endTime).length;
  const parts = [plural(entries.length, 'booking'), `${formatMinutes(minutes)} worked`];
  if (open > 0) parts.push(plural(open, 'open booking'));
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
  const toast = useToast();
  const formId = useId();

  const [mode, setMode] = useState<Mode | null>(null);
  const [listDate, setListDate] = useState(date);
  const [editor, setEditor] = useState<EditorState>(() => newEditor(date));
  const [absenceStart, setAbsenceStart] = useState(date);
  const [absenceEnd, setAbsenceEnd] = useState(date);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [dateEditable, setDateEditable] = useState(false);

  const entriesQuery = useQuery({
    queryKey: queryKeys.timeEntries(listDate, listDate),
    queryFn: () => api.getTimeEntries(listDate, listDate),
  });
  const absencesQuery = useQuery({
    queryKey: queryKeys.absences(date, date),
    queryFn: () => api.getAbsences(date, date),
  });

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
    setMode(!hasEntries && absence ? absence.type : 'WORKING');
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
  };

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
      if (!editor.date) throw new ValidationError('Date cannot be null');
      if (!editor.startTime) throw new ValidationError('Start time is mandatory');
      const payload = {
        date: editor.date,
        startTime: editor.startTime,
        endTime: editor.endTime || null,
        breakMinutes: editor.breakMinutes,
        notes: editor.notes.trim() || null,
      };
      if (editor.id) await api.updateTimeEntry(editor.id, payload);
      else await api.createTimeEntry(payload);
      toast.success('Time entry saved successfully');
      setListDate(editor.date);
      setEditor(newEditor(editor.date));
      await invalidateTimeData(queryClient);
    });

  const deleteBooking = (id: string) =>
    run(async () => {
      await api.deleteTimeEntry(id);
      toast.success('Time entry deleted');
      if (editor.id === id) setEditor(newEditor(listDate));
      await invalidateTimeData(queryClient);
    });

  const saveAbsence = (type: AbsenceType) =>
    run(async () => {
      if (!absenceStart) throw new ValidationError('Start date is mandatory');
      const end = absenceEnd || absenceStart;
      if (end < absenceStart) throw new ValidationError('End date must be on or after start date');
      await api.saveAbsence(absenceStart, end, type);
      toast.success(type === 'SICK' ? 'Sick leave recorded' : 'Vacation booked');
      await invalidateTimeData(queryClient);
      onClose();
    });

  const removeAbsence = (type: AbsenceType) =>
    run(async () => {
      if (!absenceStart) throw new ValidationError('Start date is mandatory');
      const end = absenceEnd || absenceStart;
      if (end < absenceStart) throw new ValidationError('End date must be on or after start date');
      await api.deleteAbsences(absenceStart, end);
      toast.success(type === 'SICK' ? 'Sick leave removed' : 'Vacation cancelled');
      await invalidateTimeData(queryClient);
      onClose();
    });

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    if (busy || mode === null) return;
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
        {mode && <span className="dialog-head__pill">{MODE_NAMES[mode]}</span>}
      </div>
      <p className="dialog-head__summary">{summary || '\u00a0'}</p>
    </div>
  );

  const footer =
    mode === null ? undefined : (
      <>
        <Button type="submit" form={formId} variant="primary" loading={busy}>
          Save changes
        </Button>
        {mode === 'WORKING' && editor.id && (
          <Button variant="error" disabled={busy} onClick={() => void deleteBooking(editor.id as string)}>
            Delete booking
          </Button>
        )}
        {mode !== 'WORKING' && (
          <Button variant="error" disabled={busy} onClick={() => void removeAbsence(mode)}>
            {mode === 'SICK' ? 'Remove sick leave' : 'Remove vacation'}
          </Button>
        )}
        <Button variant="tertiary" onClick={onClose}>
          Close
        </Button>
      </>
    );

  return (
    <Modal open onClose={onClose} title={title} footer={footer}>
      {mode === null ? (
        <div className="dialog-loading" role="status">
          <span className="spinner" aria-hidden="true" />
          Loading…
        </div>
      ) : (
        <form id={formId} className="dialog-body" noValidate onSubmit={onSubmit}>
          <div className="dialog-modes" role="group" aria-label="Entry type">
            {MODE_SWITCH.map((item) => (
              <Button
                key={item.mode}
                variant={mode === item.mode ? 'primary' : 'tertiary'}
                aria-pressed={mode === item.mode}
                onClick={() => changeMode(item.mode)}
              >
                {item.label}
              </Button>
            ))}
          </div>

          {error && (
            <div className="notice notice--error" role="alert">
              {error}
            </div>
          )}

          {mode === 'WORKING' ? (
            <div className={`dialog-panels`}>
              {showList && (
                <section className="dialog-panel" aria-label="Bookings">
                  <h3 className="dialog-panel__title">Bookings</h3>
                  <p className="dialog-panel__hint">{summarize(entries)}</p>
                  {entriesQuery.isPending ? (
                    <div className="dialog-loading" role="status">
                      <span className="spinner" aria-hidden="true" />
                      Loading…
                    </div>
                  ) : entriesQuery.isError ? (
                    <div className="notice notice--error" role="alert">
                      The bookings could not be loaded.
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
                                aria-label={`Edit booking ${bookingHeadline(entry)}`}
                                onClick={() => selectEntry(entry)}
                              >
                                <Icon name="edit" />
                              </Button>
                              <Button
                                icon
                                variant="tertiary"
                                className="btn--danger-text"
                                aria-label={`Delete booking ${bookingHeadline(entry)}`}
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
                      Add booking
                    </Button>
                  </div>
                </section>
              )}

              <section className="dialog-panel dialog-panel--editor" aria-label="Booking editor">
                <h3 className="dialog-panel__title">{editor.id ? 'Editing booking' : 'New booking'}</h3>
                <p className="dialog-panel__hint">
                  {editor.id
                    ? 'Update the selected booking or delete it if it is no longer needed.'
                    : showList
                      ? 'Create a new booking for the selected day.'
                      : 'Add the first booking for this day.'}
                </p>
                <div className="dialog-grid">
                  {dateEditable ? (
                    <TextField
                      className="dialog-grid__full"
                      label="Date"
                      type="date"
                      required
                      value={editor.date}
                      onChange={(e) => changeEditorDate(e.target.value)}
                    />
                  ) : (
                    <div className="dialog-grid__full dialog-date">
                      <span>For {formatShortDate(editor.date)}</span>
                      <Button variant="tertiary" small onClick={() => setDateEditable(true)}>
                        Change date
                      </Button>
                    </div>
                  )}
                  <TextField
                    label="Start time"
                    type="time"
                    step={300}
                    required
                    value={editor.startTime}
                    onChange={(e) => patchEditor({ startTime: e.target.value })}
                  />
                  <TextField
                    label="End time"
                    type="time"
                    step={300}
                    value={editor.endTime}
                    onChange={(e) => patchEditor({ endTime: e.target.value })}
                  />
                  <NumberField
                    className="dialog-grid__full"
                    label="Break (min)"
                    helperText="Minutes"
                    min={0}
                    max={480}
                    step={5}
                    value={editor.breakMinutes}
                    onChange={(value) => patchEditor({ breakMinutes: value })}
                  />
                  <TextAreaField
                    className="dialog-grid__full"
                    label="Notes"
                    helperText="Max. 500 characters"
                    maxLength={500}
                    value={editor.notes}
                    onChange={(e) => patchEditor({ notes: e.target.value })}
                  />
                </div>
              </section>
            </div>
          ) : (
            <section className="dialog-panel" aria-label={MODE_NAMES[mode]}>
              <h3 className="dialog-panel__title">{MODE_NAMES[mode]}</h3>
              <p className="dialog-panel__hint">Use a simple date range. No time fields are needed for absences.</p>
              <div className="dialog-grid dialog-grid--absence">
                <TextField
                  label="Start date"
                  type="date"
                  required
                  value={absenceStart}
                  onChange={(e) => changeAbsenceStart(e.target.value)}
                />
                <TextField
                  label="End date"
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
