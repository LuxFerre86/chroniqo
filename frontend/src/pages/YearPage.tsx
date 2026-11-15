import { useEffect, useMemo, useState, type CSSProperties } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router';
import { api } from '../api/endpoints';
import type { DaySummary } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { Button } from '../components/Button';
import { Icon } from '../components/Icon';
import { absenceCssKey, absenceLabel, getAbsenceTypes } from '../lib/absence';
import { daysInMonth, firstWeekdayIndex, formatShortDate, isoOf, todayIso, weekdayOrder } from '../lib/dates';
import { formatBalance, formatMinutes } from '../lib/format';
import { queryKeys } from '../lib/queries';
import { computeYearStats } from '../lib/yearStats';
import { getLocale, translate, useT } from '../i18n';
import '../styles/year.css';

const MIN_YEAR = 2000;
const MAX_YEAR = 2050;
const HEAT_FALLBACK_MINUTES = 480;
const weekdayInitial = (index: number) => translate(`month.weekday.${index}` as 'month.weekday.0').slice(0, 2);
const monthName = (year: number, month: number) =>
  new Date(year, month - 1, 1).toLocaleDateString(getLocale(), { month: 'long' });
const shortMonth = (month: number) => translate(`month.short.${month}` as 'month.short.1');
const number = (value: number) => value.toLocaleString(getLocale(), { maximumFractionDigits: 1 });
const tone = (n: number) => (n >= 0 ? 'positive' : 'negative');

function MiniMonth({
  year,
  month,
  byDate,
  today,
  firstDay,
}: {
  year: number;
  month: number;
  byDate: Map<string, DaySummary>;
  today: string;
  firstDay: Parameters<typeof weekdayOrder>[0];
}) {
  const { t } = useT();
  const ym = { year, month };
  const leading = firstWeekdayIndex(ym, firstDay);
  const target = `/month?month=${year}-${String(month).padStart(2, '0')}`;

  return (
    <section className="year-month" aria-label={`${monthName(year, month)} ${year}`}>
      <h3 className="year-month__title">
        <Link to={target} aria-label={t('year.openMonth', { month: `${monthName(year, month)} ${year}` })}>
          {monthName(year, month)}
        </Link>
      </h3>
      <div className="year-grid">
        {weekdayOrder(firstDay).map((weekday) => (
          <span key={weekday} className="year-grid__head" aria-hidden="true">
            {weekdayInitial(weekday)}
          </span>
        ))}
        {Array.from({ length: leading }, (_, i) => (
          <span key={`pad-${i}`} aria-hidden="true" />
        ))}
        {Array.from({ length: daysInMonth(ym) }, (_, i) => {
          const iso = isoOf(ym, i + 1);
          const day = byDate.get(iso);
          const worked = day?.workedMinutes ?? 0;
          const isFuture = iso > today;
          const heat =
            !isFuture && worked > 0
              ? Math.min(1, worked / (day && day.targetMinutes > 0 ? day.targetMinutes : HEAT_FALLBACK_MINUTES))
              : 0;
          const classes = [
            'year-day',
            day?.absenceType ? `year-day--absence badge--${absenceCssKey(day.absenceType)}` : '',
            day && !day.workday && !day.absenceType ? 'year-day--off' : '',
            iso === today ? 'year-day--today' : '',
            heat > 0 ? 'year-day--worked' : '',
          ]
            .filter(Boolean)
            .join(' ');
          const label = [
            formatShortDate(iso),
            day?.absenceLabel,
            worked > 0 ? formatMinutes(worked) : null,
          ]
            .filter(Boolean)
            .join(', ');
          return (
            <Link
              key={iso}
              to={`/month?month=${year}-${String(month).padStart(2, '0')}`}
              className={classes}
              aria-label={label}
              aria-current={iso === today ? 'date' : undefined}
              data-date={iso}
              style={heat > 0 ? ({ '--heat': heat.toFixed(2) } as CSSProperties) : undefined}
            >
              {i + 1}
            </Link>
          );
        })}
      </div>
    </section>
  );
}

function StatCard({
  label,
  value,
  sub,
  tone: valueTone,
}: {
  label: string;
  value: string;
  sub?: string;
  tone?: 'positive' | 'negative';
}) {
  return (
    <div className="year-stat">
      <div className="year-stat__label">{label}</div>
      <div className={`year-stat__value${valueTone ? ` is-${valueTone}` : ''}`}>{value}</div>
      {sub && <div className="year-stat__sub">{sub}</div>}
    </div>
  );
}

export function YearPage() {
  const { t, tp } = useT();
  const { user } = useAuth();
  const [year, setYear] = useState(() => Number(todayIso().slice(0, 4)));
  const currentYear = Number(todayIso().slice(0, 4));

  useEffect(() => {
    document.title = t('year.pageTitle');
  }, [t]);

  const from = `${year}-01-01`;
  const to = `${year}-12-31`;
  const { data, isPending, isError, refetch } = useQuery({
    queryKey: queryKeys.summary(from, to),
    queryFn: () => api.getSummary(from, to),
  });
  const vacation = useQuery({
    queryKey: queryKeys.vacation(year),
    queryFn: () => api.getVacationAccount(year),
  });

  const today = todayIso();
  const byDate = useMemo(() => new Map((data ?? []).map((d) => [d.date, d])), [data]);
  const stats = useMemo(() => computeYearStats(data ?? [], today), [data, today]);
  const legend = useMemo(() => {
    const present = new Set((data ?? []).map((d) => d.absenceType));
    return [{ type: 'HOLIDAY', label: t('month.holiday') }, ...getAbsenceTypes()].filter((item) =>
      present.has(item.type as DaySummary['absenceType']),
    );
  }, [data, t]);

  const firstDay = user?.firstDayOfWeek ?? 'MONDAY';
  const chartMax = Math.max(1, ...stats.months.flatMap((m) => [m.worked, m.target]));
  const absenceEntries = Object.entries(stats.absenceDays).filter(([type]) => type !== 'VACATION');

  return (
    <div className="page year">
      <div className="year-nav">
        <Button icon aria-label={t('year.prev')} disabled={year <= MIN_YEAR} onClick={() => setYear((y) => y - 1)}>
          <Icon name="chevron-left" />
        </Button>
        <h1 className="year-nav__label">{year}</h1>
        <Button icon aria-label={t('year.next')} disabled={year >= MAX_YEAR} onClick={() => setYear((y) => y + 1)}>
          <Icon name="chevron-right" />
        </Button>
        {year !== currentYear && (
          <Button variant="outlined" small onClick={() => setYear(currentYear)}>
            {t('year.current')}
          </Button>
        )}
      </div>

      {isPending ? (
        <div className="centered-state" role="status">
          <span className="spinner" aria-hidden="true" />
          {t('year.loading')}
        </div>
      ) : isError ? (
        <div>
          <div className="notice notice--error" role="alert">
            {t('year.loadError')}
          </div>
          <p>
            <Button onClick={() => void refetch()}>{t('year.retry')}</Button>
          </p>
        </div>
      ) : (
        <>
          <section className="card year-stats" aria-label={t('year.stats')}>
            <StatCard label={t('year.worked')} value={formatMinutes(stats.total.worked)} />
            <StatCard label={t('year.target')} value={formatMinutes(stats.total.target)} />
            <StatCard
              label={t('year.balance')}
              value={formatBalance(stats.total.balance)}
              tone={tone(stats.total.balance)}
            />
            <StatCard
              label={t('year.average')}
              value={formatMinutes(stats.total.average)}
              sub={tp('year.workedDays', stats.total.workedDays)}
            />
            {vacation.data && (
              <StatCard
                label={t('year.vacation')}
                value={t('year.vacation.value', {
                  taken: number(vacation.data.taken),
                  entitlement: number(vacation.data.entitlement),
                })}
                sub={t('year.vacation.remaining', {
                  remaining: number(vacation.data.remaining),
                  planned: number(vacation.data.planned),
                })}
              />
            )}
            {absenceEntries.length > 0 && (
              <div className="year-stat">
                <div className="year-stat__label">{t('year.absences')}</div>
                <ul className="year-absences">
                  {absenceEntries.map(([type, count]) => (
                    <li key={type}>
                      {absenceLabel(type as Parameters<typeof absenceLabel>[0])}: {tp('year.days', count)}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </section>

          <div className="year-months">
            {Array.from({ length: 12 }, (_, i) => (
              <MiniMonth
                key={i + 1}
                year={year}
                month={i + 1}
                byDate={byDate}
                today={today}
                firstDay={firstDay}
              />
            ))}
          </div>

          {legend.length > 0 && (
            <ul className="cal-legend year-legend" aria-label={t('month.legend')}>
              {legend.map((item) => (
                <li key={item.type} className="cal-legend__item">
                  <span className={`cal-legend__dot badge--${absenceCssKey(item.type)}`} aria-hidden="true" />
                  {item.label}
                </li>
              ))}
            </ul>
          )}

          <section className="card year-compare">
            <h2 className="year-compare__title">{t('year.compare')}</h2>
            <div className="year-chart" role="img" aria-label={t('year.chart')}>
              {stats.months.map((m) => (
                <div key={m.month} className="year-chart__col">
                  <div className="year-chart__bars">
                    <span
                      className="year-chart__bar year-chart__bar--target"
                      style={{ height: `${(m.target / chartMax) * 100}%` }}
                    />
                    <span
                      className="year-chart__bar year-chart__bar--worked"
                      style={{ height: `${(m.worked / chartMax) * 100}%` }}
                    />
                  </div>
                  <span className="year-chart__label">{shortMonth(m.month)}</span>
                </div>
              ))}
            </div>
            <div className="year-chart__legend">
              <span>
                <i className="year-chart__swatch year-chart__bar--worked" />
                {t('year.worked')}
              </span>
              <span>
                <i className="year-chart__swatch year-chart__bar--target" />
                {t('year.target')}
              </span>
            </div>
            <div className="year-table-wrap">
              <table className="year-table">
                <thead>
                  <tr>
                    <th scope="col">{t('year.col.month')}</th>
                    <th scope="col">{t('year.worked')}</th>
                    <th scope="col">{t('year.target')}</th>
                    <th scope="col">{t('year.balance')}</th>
                    <th scope="col">{t('year.average')}</th>
                  </tr>
                </thead>
                <tbody>
                  {stats.months.map((m) => (
                    <tr key={m.month}>
                      <th scope="row">{monthName(year, m.month)}</th>
                      <td>{formatMinutes(m.worked)}</td>
                      <td>{formatMinutes(m.target)}</td>
                      <td className={`is-${tone(m.balance)}`}>{formatBalance(m.balance)}</td>
                      <td>{m.workedDays > 0 ? formatMinutes(m.average) : '–'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}
    </div>
  );
}
