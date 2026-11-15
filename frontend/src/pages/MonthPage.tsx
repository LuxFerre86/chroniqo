import { Fragment, useEffect, useMemo, useState, type CSSProperties } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router';
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
import { absenceCssKey, getAbsenceTypes } from '../lib/absence';
import { translate, useT } from '../i18n';
import { formatBalance, formatMinutes } from '../lib/format';
import { queryKeys } from '../lib/queries';
import type { DaySummary } from '../api/types';
import '../styles/month.css';

const WEEKEND_INDICES = [5, 6];
const HEAT_FALLBACK_MINUTES = 480;
const weekdayName = (index: number) => translate(`month.weekday.${index}` as 'month.weekday.0');
const shortMonths = () => Array.from({ length: 12 }, (_, i) => translate(`month.short.${i + 1}` as 'month.short.1'));
const absenceLegend = (): { type: DaySummary['absenceType']; label: string }[] => [
  { type: 'HOLIDAY', label: translate('month.holiday') },
  ...getAbsenceTypes().map(({ type, label }) => ({ type, label })),
];
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

/** Reads YYYY-MM (e.g. from the yearly overview); invalid or out-of-range values are ignored. */
function parseMonthParam(value: string | null): YearMonth | null {
  const match = /^(\d{4})-(\d{2})$/.exec(value ?? '');
  if (!match) return null;
  const year = Number(match[1]);
  const month = Number(match[2]);
  return year >= MIN_YEAR && year <= MAX_YEAR && month >= 1 && month <= 12 ? { year, month } : null;
}

function badgeClass(type: DaySummary['absenceType']): string {
  return `badge badge--${absenceCssKey(type)}`;
}

function StatSection({
  kind,
  label,
  value,
  tone,
  sub,
  hint,
}: {
  kind: 'target' | 'worked' | 'balance';
  label: string;
  value: string;
  tone?: 'positive' | 'negative';
  sub?: { label: string; value: string; tone: 'positive' | 'negative' };
  hint?: string;
}) {
  return (
    <div className={`month-stat month-stat--${kind}`}>
      <div className="month-stat__label">{label}</div>
      <div className="month-stat__value">
        <span className={`month-stat__month${tone ? ` is-${tone}` : ''}`}>{value}</span>
      </div>
      {hint && <div className="month-stat__sub">{hint}</div>}
      {sub && (
        <div className="month-stat__sub">
          {sub.label} <span className={`is-${sub.tone}`}>{sub.value}</span>
        </div>
      )}
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
  const { t } = useT();
  const dayNumber = Number(iso.slice(8, 10));
  const isToday = iso === today;
  const workday = day?.workday ?? false;
  const isFuture = iso > today;
  const worked = day?.workedMinutes ?? 0;
  const balance = day?.balanceMinutes ?? 0;
  const target = day?.targetMinutes ?? 0;
  const showTarget = isFuture && workday && target > 0 && worked === 0;
  const showWorked = !showTarget && worked > 0;
  const showBalance = !!day && !showTarget && (workday || balance > 0);
  const hasBadge = !!day?.absenceType && !!day.absenceLabel;

  const label = [
    formatShortDate(iso),
    hasBadge ? day?.absenceLabel : null,
    showTarget ? t('month.day.target', { value: formatMinutes(target) }) : null,
    showWorked ? t('month.day.worked', { value: formatMinutes(worked) }) : null,
    showBalance ? t('month.day.balance', { value: formatBalance(balance) }) : null,
  ]
    .filter(Boolean)
    .join(', ');

  const isEmpty = worked === 0 && !hasBadge && (isFuture || !workday);
  const isWeekend = WEEKEND_INDICES.includes(weekdayIndex(parseIsoDate(iso)));
  const classes = [
    'day',
    isToday ? 'day--today' : '',
    workday ? '' : 'day--off',
    isWeekend ? 'day--weekend' : '',
    isEmpty ? 'day--empty' : '',
  ]
    .filter(Boolean)
    .join(' ');
  const heat = !isFuture && worked > 0 ? Math.min(1, worked / (target > 0 ? target : HEAT_FALLBACK_MINUTES)) : 0;

  return (
    <div
      className={classes}
      role="button"
      tabIndex={0}
      aria-label={label}
      aria-current={isToday ? 'date' : undefined}
      data-date={iso}
      style={heat > 0 ? ({ '--heat': heat.toFixed(2) } as CSSProperties) : undefined}
      onClick={() => onOpen(iso)}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onOpen(iso);
        }
      }}
    >
      <div className="day__date">
        <span className="day__weekday">{weekdayName(weekdayIndex(parseIsoDate(iso)))}</span>
        <span className="day__num">{dayNumber}</span>
      </div>
      {hasBadge && (
        <span className={`day__badge ${badgeClass(day?.absenceType ?? null)}`} title={day?.absenceLabel ?? undefined}>
          {day?.absenceLabel}
        </span>
      )}
      {showTarget && (
        <div className="day__figures">
          <span className="day__target">{t('month.day.targetShort', { value: formatMinutes(target) })}</span>
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

function WeekHeader({
  ym,
  startDay,
  endDay,
  byDate,
}: {
  ym: YearMonth;
  startDay: number;
  endDay: number;
  byDate: Map<string, DaySummary>;
}) {
  let worked = 0;
  let target = 0;
  for (let d = startDay; d <= endDay; d++) {
    const day = byDate.get(isoOf(ym, d));
    worked += day?.workedMinutes ?? 0;
    target += day?.targetMinutes ?? 0;
  }
  const range =
    startDay === endDay
      ? formatShortDate(isoOf(ym, startDay))
      : `${formatShortDate(isoOf(ym, startDay))} – ${formatShortDate(isoOf(ym, endDay))}`;

  return (
    <div className="week-head">
      <span className="week-head__range">{range}</span>
      <span className="week-head__sum">
        <strong>{formatMinutes(worked)}</strong> / {formatMinutes(target)}
      </span>
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
  const { t } = useT();
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
      title={t('month.picker.title')}
      narrow
      closeOnBackdrop
      footer={
        <Button variant="tertiary" onClick={onClose}>
          {t('month.picker.close')}
        </Button>
      }
    >
      <div className="month-picker">
        <SelectField
          label={t('month.picker.year')}
          value={String(year)}
          options={years}
          onChange={(e) => setYear(Number(e.target.value))}
        />
        <div className="month-picker__grid">
          {shortMonths().map((name, i) => {
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
  const { t } = useT();
  const { user } = useAuth();
  const [searchParams] = useSearchParams();
  const [ym, setYm] = useState<YearMonth>(() => parseMonthParam(searchParams.get('month')) ?? currentYearMonth());
  const [pickerOpen, setPickerOpen] = useState(false);
  const [dialogDate, setDialogDate] = useState<string | null>(null);

  useEffect(() => {
    document.title = t('month.pageTitle');
  }, [t]);

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

  const legend = useMemo(() => {
    const prefix = `0${ym.month}`.slice(-2);
    const present = new Set((data ?? []).filter((d) => d.date.slice(5, 7) === prefix).map((d) => d.absenceType));
    return absenceLegend().filter((item) => present.has(item.type));
  }, [data, ym.month, t]);

  const total = daysInMonth(ym);
  const firstDay = user?.firstDayOfWeek ?? 'MONDAY';
  const leading = firstWeekdayIndex(ym, firstDay);
  const headerOrder = weekdayOrder(firstDay);
  const isFutureMonth = ym.year * 12 + ym.month > current.year * 12 + current.month;
  const hint = sameMonth(ym, current)
    ? t('month.hint.current')
    : isFutureMonth
      ? t('month.hint.future')
      : t('month.hint.full');
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
          <Button icon aria-label={t('month.prev')} onClick={() => setYm((v) => addMonths(v, -1))}>
            <Icon name="chevron-left" />
          </Button>
          <Button className="month-nav__label" aria-haspopup="dialog" onClick={() => setPickerOpen(true)}>
            {formatMonthLabel(ym)}
          </Button>
          <Button icon aria-label={t('month.next')} onClick={() => setYm((v) => addMonths(v, 1))}>
            <Icon name="chevron-right" />
          </Button>
        </div>
        <div className="month-nav__today-slot">
          {!sameMonth(ym, current) && (
            <Button variant="outlined" small className="month-nav__today" onClick={() => setYm(current)}>
              {t('month.today')}
            </Button>
          )}
        </div>
      </div>

      {isPending ? (
        <div className="centered-state" role="status">
          <span className="spinner" aria-hidden="true" />
          {t('month.loading')}
        </div>
      ) : isError ? (
        <div>
          <div className="notice notice--error" role="alert">
            {t('month.loadError')}
          </div>
          <p>
            <Button onClick={() => void refetch()}>{t('month.retry')}</Button>
          </p>
        </div>
      ) : (
        <>
          <section className="card month-stats" aria-label={t('month.stats')}>
            <StatSection kind="target" label={t('month.target')} value={formatMinutes(monthTotals.target)} hint={hint} />
            <StatSection kind="worked" label={t('month.worked')} value={formatMinutes(monthTotals.worked)} hint={hint} />
            <StatSection
              kind="balance"
              label={t('month.balance')}
              value={formatBalance(monthTotals.balance)}
              tone={tone(monthTotals.balance)}
              sub={{
                label: t('month.year', { year: ym.year }),
                value: formatBalance(yearTotals.balance),
                tone: tone(yearTotals.balance),
              }}
            />
          </section>

          <div className="cal" aria-label={formatMonthLabel(ym)}>
            {headerOrder.map((weekday) => (
              <div
                key={weekday}
                className={`cal__head${WEEKEND_INDICES.includes(weekday) ? ' cal__head--weekend' : ''}`}
                aria-hidden="true"
              >
                {weekdayName(weekday)}
              </div>
            ))}
            {Array.from({ length: leading }, (_, i) => (
              <div key={`pad-${i}`} className="cal__pad" aria-hidden="true" />
            ))}
            {Array.from({ length: total }, (_, i) => {
              const iso = isoOf(ym, i + 1);
              const weekStart = i === 0 || (leading + i) % 7 === 0;
              return (
                <Fragment key={iso}>
                  {weekStart && (
                    <WeekHeader
                      ym={ym}
                      startDay={i + 1}
                      endDay={Math.min(total, i + 7 - ((leading + i) % 7))}
                      byDate={byDate}
                    />
                  )}
                  <DayCard iso={iso} day={byDate.get(iso)} today={today} onOpen={setDialogDate} />
                </Fragment>
              );
            })}
          </div>

          {legend.length > 0 && (
            <ul className="cal-legend" aria-label={t('month.legend')}>
              {legend.map((item) => (
                <li key={item.type} className="cal-legend__item">
                  <span
                    className={`cal-legend__dot badge--${absenceCssKey(item.type)}`}
                    aria-hidden="true"
                  />
                  {item.label}
                </li>
              ))}
            </ul>
          )}
        </>
      )}

      <MonthPicker open={pickerOpen} selected={ym} onSelect={selectMonth} onClose={() => setPickerOpen(false)} />
      <TimeEntryDialog date={dialogDate} onClose={() => setDialogDate(null)} />
    </div>
  );
}
