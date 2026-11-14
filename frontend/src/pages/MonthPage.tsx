import { useEffect, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import { useAuth } from '../auth/AuthContext';
import { Button } from '../components/Button';
import { SelectField } from '../components/fields';
import { Icon } from '../components/Icon';
import { Modal } from '../components/Modal';
import { TimeEntryDialog } from '../components/TimeEntryDialog';
import {
  addMonths,
  currentYearMonth,
  daysInMonth,
  firstWeekdayIndex,
  formatMonthLabel,
  formatShortDate,
  isoOf,
  parseIsoDate,
  sameMonth,
  todayIso,
  weekdayIndex,
  weekdayOrder,
  type YearMonth,
} from '../lib/dates';
import { formatBalance, formatMinutes } from '../lib/format';
import { queryKeys } from '../lib/queries';
import type { DaySummary } from '../api/types';
import '../styles/month.css';

const WEEKDAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
const WEEKEND_INDICES = [5, 6];
const SHORT_MONTHS = Array.from({ length: 12 }, (_, i) =>
  new Date(2000, i, 1).toLocaleDateString('en-US', { month: 'short' }),
);
const MIN_YEAR = 2000;
const MAX_YEAR = 2050;

interface Totals {
  target: number;
  worked: number;
  balance: number;
}

/** Sums all days up to and including today; `month` limits the sum to a single month (1-12). */
function totalsFor(days: DaySummary[], today: string, month?: number): Totals {
  let target = 0;
  let worked = 0;
  for (const day of days) {
    if (day.date > today) continue;
    if (month !== undefined && Number(day.date.slice(5, 7)) !== month) continue;
    target += day.targetMinutes;
    worked += day.workedMinutes;
  }
  return { target, worked, balance: worked - target };
}

function badgeClass(type: DaySummary['absenceType']): string {
  return `badge badge--${(type ?? 'holiday').toLowerCase()}`;
}

function StatSection({
  kind,
  label,
  value,
  tone,
}: {
  kind: 'target' | 'worked' | 'balance';
  label: string;
  value: string;
  tone?: 'positive' | 'negative';
}) {
  return (
    <div className={`month-stat month-stat--${kind}`}>
      <div className="month-stat__label">{label}</div>
      <div className="month-stat__value">
        <span className={`month-stat__month${tone ? ` is-${tone}` : ''}`}>{value}</span>
      </div>
    </div>
  );
}

function DayCard({
  iso,
  day,
  today,
  onOpen,
}: {
  iso: string;
  day: DaySummary | undefined;
  today: string;
  onOpen: (iso: string) => void;
}) {
  const dayNumber = Number(iso.slice(8, 10));
  const isToday = iso === today;
  const workday = day?.workday ?? false;
  const isFuture = iso > today;
  const worked = day?.workedMinutes ?? 0;
  const balance = day?.balanceMinutes ?? 0;
  const target = day?.targetMinutes ?? 0;
  const showTarget = isFuture && workday && target > 0 && worked === 0;
  const showWorked = !!day && !showTarget && (workday || worked > 0);
  const showBalance = !!day && !showTarget && (workday || balance > 0);
  const hasBadge = !!day?.absenceType && !!day.absenceLabel;

  const label = [
    formatShortDate(iso),
    hasBadge ? day?.absenceLabel : null,
    showTarget ? `target ${formatMinutes(target)}` : null,
    showWorked ? `${formatMinutes(worked)} worked` : null,
    showBalance ? `balance ${formatBalance(balance)}` : null,
  ]
    .filter(Boolean)
    .join(', ');

  const classes = ['day', isToday ? 'day--today' : '', workday ? '' : 'day--off'].filter(Boolean).join(' ');

  return (
    <div
      className={classes}
      role="button"
      tabIndex={0}
      aria-label={label}
      aria-current={isToday ? 'date' : undefined}
      data-date={iso}
      onClick={() => onOpen(iso)}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onOpen(iso);
        }
      }}
    >
      <div className="day__date">
        <span className="day__weekday">{WEEKDAYS[weekdayIndex(parseIsoDate(iso))]}</span>
        <span className="day__num">{dayNumber}</span>
      </div>
      {hasBadge && <span className={`day__badge ${badgeClass(day?.absenceType ?? null)}`}>{day?.absenceLabel}</span>}
      {showTarget && (
        <div className="day__figures">
          <span className="day__target">Target {formatMinutes(target)}</span>
        </div>
      )}
      {(showWorked || showBalance) && (
        <div className="day__figures">
          {showWorked && <span className="day__worked">{formatMinutes(worked)}</span>}
          {showBalance && (
            <span className={`day__balance ${balance >= 0 ? 'balance-positive' : 'balance-negative'}`}>
              {formatBalance(balance)}
            </span>
          )}
        </div>
      )}
    </div>
  );
}

function MonthPicker({
  open,
  selected,
  onSelect,
  onClose,
}: {
  open: boolean;
  selected: YearMonth;
  onSelect: (ym: YearMonth) => void;
  onClose: () => void;
}) {
  const [year, setYear] = useState(selected.year);
  const current = currentYearMonth();

  useEffect(() => {
    if (open) setYear(selected.year);
  }, [open, selected.year]);

  const years = useMemo(() => {
    const from = Math.min(MIN_YEAR, selected.year, current.year);
    const to = Math.max(MAX_YEAR, selected.year, current.year);
    return Array.from({ length: to - from + 1 }, (_, i) => {
      const value = String(from + i);
      return { value, label: value };
    });
  }, [selected.year, current.year]);

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Select Month"
      narrow
      closeOnBackdrop
      footer={
        <Button variant="tertiary" onClick={onClose}>
          Close
        </Button>
      }
    >
      <div className="month-picker">
        <SelectField label="Year" value={String(year)} options={years} onChange={(e) => setYear(Number(e.target.value))} />
        <div className="month-picker__grid">
          {SHORT_MONTHS.map((name, i) => {
            const ym = { year, month: i + 1 };
            const isSelected = sameMonth(ym, selected);
            const isCurrent = sameMonth(ym, current);
            return (
              <Button
                key={name}
                variant={isSelected ? 'primary' : isCurrent ? 'outlined' : 'tertiary'}
                className={isCurrent && !isSelected ? 'btn--current' : undefined}
                aria-pressed={isSelected}
                onClick={() => onSelect(ym)}
              >
                {name}
              </Button>
            );
          })}
        </div>
      </div>
    </Modal>
  );
}

export function MonthPage() {
  const { user } = useAuth();
  const [ym, setYm] = useState<YearMonth>(currentYearMonth);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [dialogDate, setDialogDate] = useState<string | null>(null);

  useEffect(() => {
    document.title = 'Monthly View | chroniqo';
  }, []);

  const from = `${ym.year}-01-01`;
  const to = `${ym.year}-12-31`;
  const { data, isPending, isError, refetch } = useQuery({
    queryKey: queryKeys.summary(from, to),
    queryFn: () => api.getSummary(from, to),
  });

  const today = todayIso();
  const current = currentYearMonth();

  const byDate = useMemo(() => new Map((data ?? []).map((d) => [d.date, d])), [data]);
  const monthTotals = useMemo(() => totalsFor(data ?? [], today, ym.month), [data, today, ym.month]);
  const yearTotals = useMemo(() => totalsFor(data ?? [], today), [data, today]);

  const total = daysInMonth(ym);
  const firstDay = user?.firstDayOfWeek ?? 'MONDAY';
  const leading = firstWeekdayIndex(ym, firstDay);
  const headerOrder = weekdayOrder(firstDay);
  const tone = (n: number) => (n >= 0 ? 'positive' : 'negative');

  const selectMonth = (next: YearMonth) => {
    setYm(next);
    setPickerOpen(false);
  };

  return (
    <div className="page month">
      <div className="month-nav">
        <span aria-hidden="true" />
        <div className="month-nav__group">
          <Button icon aria-label="Previous month" onClick={() => setYm((v) => addMonths(v, -1))}>
            <Icon name="chevron-left" />
          </Button>
          <Button className="month-nav__label" aria-haspopup="dialog" onClick={() => setPickerOpen(true)}>
            {formatMonthLabel(ym)}
          </Button>
          <Button icon aria-label="Next month" onClick={() => setYm((v) => addMonths(v, 1))}>
            <Icon name="chevron-right" />
          </Button>
        </div>
        <div className="month-nav__today-slot">
          {!sameMonth(ym, current) && (
            <Button variant="outlined" small className="month-nav__today" onClick={() => setYm(current)}>
              Today
            </Button>
          )}
        </div>
      </div>

      {isPending ? (
        <div className="centered-state" role="status">
          <span className="spinner" aria-hidden="true" />
          Loading…
        </div>
      ) : isError ? (
        <div>
          <div className="notice notice--error" role="alert">
            The monthly overview could not be loaded.
          </div>
          <p>
            <Button onClick={() => void refetch()}>Retry</Button>
          </p>
        </div>
      ) : (
        <>
          <section className="card month-stats" aria-label="Statistics">
            <StatSection kind="target" label="Target hours" value={formatMinutes(monthTotals.target)} />
            <StatSection kind="worked" label="Worked hours" value={formatMinutes(monthTotals.worked)} />
            <StatSection
              kind="balance"
              label={`Balance (${ym.year})`}
              value={formatBalance(yearTotals.balance)}
              tone={tone(yearTotals.balance)}
            />
          </section>

          <div className="cal" aria-label={formatMonthLabel(ym)}>
            {headerOrder.map((weekday) => (
              <div
                key={weekday}
                className={`cal__head${WEEKEND_INDICES.includes(weekday) ? ' cal__head--weekend' : ''}`}
                aria-hidden="true"
              >
                {WEEKDAYS[weekday]}
              </div>
            ))}
            {Array.from({ length: leading }, (_, i) => (
              <div key={`pad-${i}`} className="cal__pad" aria-hidden="true" />
            ))}
            {Array.from({ length: total }, (_, i) => {
              const iso = isoOf(ym, i + 1);
              return (
                <DayCard
                  key={iso}
                  iso={iso}
                  day={byDate.get(iso)}
                  today={today}
                  onOpen={setDialogDate}
                />
              );
            })}
          </div>
        </>
      )}

      <MonthPicker open={pickerOpen} selected={ym} onSelect={selectMonth} onClose={() => setPickerOpen(false)} />
      <TimeEntryDialog date={dialogDate} onClose={() => setDialogDate(null)} />
    </div>
  );
}
