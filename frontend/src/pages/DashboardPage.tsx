import { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { api } from '../api/endpoints';
import { ApiError } from '../api/http';
import { Button } from '../components/Button';
import { Icon } from '../components/Icon';
import { TimeEntryDialog } from '../components/TimeEntryDialog';
import { useToast } from '../components/Toast';
import { formatClock, formatHours, formatLongBalance, formatSignedClock, trimSeconds } from '../lib/format';
import { formatLongDate, parseIsoDate, todayIso, weekdayShort } from '../lib/dates';
import { invalidateTimeData, queryKeys } from '../lib/queries';
import { useT } from '../i18n';
import type { Dashboard, DaySummary, TimeEntry } from '../api/types';
import '../styles/dashboard.css';

const MIN_CHART_SCALE_MINUTES = 480;

function progressTone(percentage: number): 'green' | 'amber' | 'red' {
  if (percentage >= 100) return 'green';
  if (percentage >= 80) return 'amber';
  return 'red';
}

const RING_RADIUS = 84;
const RING_CIRCUMFERENCE = 2 * Math.PI * RING_RADIUS;

function TodayCard({
  today,
  openEntry,
  onStop,
  stopping,
}: {
  today: DaySummary | null;
  openEntry: TimeEntry | null;
  onStop: () => void;
  stopping: boolean;
}) {
  const { t } = useT();
  const workedMinutes = today?.workedMinutes ?? 0;
  const targetMinutes = today?.targetMinutes ?? 0;
  const balance = today?.balanceMinutes ?? 0;
  const hasTarget = targetMinutes > 0;
  // Without a target (e.g. weekend) any worked time fills the ring instead of leaving it empty.
  const ratio = hasTarget ? Math.min(1, workedMinutes / targetMinutes) : workedMinutes > 0 ? 1 : 0;
  return (
    <section className="card dash-today"     aria-label={t('dashboard.today')}>
          <div className="dash-today__label">{t('dashboard.today')}</div>
      <div className="dash-ring">
        <svg className="dash-ring__svg" viewBox="0 0 200 200" aria-hidden="true">
          <circle className="dash-ring__track" cx="100" cy="100" r={RING_RADIUS} />
          {ratio > 0 && (
            <circle
              className={`dash-ring__arc${ratio >= 1 ? ' dash-ring__arc--done' : ''}`}
              cx="100"
              cy="100"
              r={RING_RADIUS}
              strokeDasharray={RING_CIRCUMFERENCE}
              strokeDashoffset={RING_CIRCUMFERENCE * (1 - ratio)}
            />
          )}
        </svg>
        <div className="dash-ring__center">
          <div className="dash-today__value" data-testid="today-worked">
            {formatClock(workedMinutes)}
          </div>
          <div className="dash-today__caption">
            {hasTarget ? t('dashboard.today.target', { target: formatClock(targetMinutes) }) : t('dashboard.today.noTarget')}
          </div>
        </div>
      </div>
      <div
        className={`dash-today__balance dash-today__balance--${balance >= 0 ? 'positive' : 'negative'}`}
        data-testid="today-balance"
      >
        {formatSignedClock(balance)}
      </div>
      {openEntry && (
        <div className="dash-today__running" data-testid="running-entry">
          <span className="dash-today__running-label">
            <span className="dash-today__running-dot" aria-hidden="true" />
            {t('dashboard.running', { time: trimSeconds(openEntry.startTime) })}
          </span>
          <Button variant="error" small onClick={onStop} loading={stopping}>
            {t('dashboard.stop')}
          </Button>
        </div>
      )}
    </section>
  );
}
function BalanceCard({ minutes, year }: { minutes: number; year: string }) {
  const { t } = useT();
  return (
    <section className="card dash-stat" aria-label={t('dashboard.balance.label')}>
      <div className="dash__label">{t('dashboard.balance.title', { year })}</div>
      <div
        className={`dash-stat__value dash-stat__value--${minutes >= 0 ? 'positive' : 'negative'}`}
        data-testid="current-balance"
      >
        {formatLongBalance(minutes)}
      </div>
    </section>
  );
}

function VacationCard({ year }: { year: number }) {
  const { t, tp } = useT();
  const { data } = useQuery({ queryKey: queryKeys.vacation(year), queryFn: () => api.getVacationAccount(year) });
  if (!data) return null;
  return (
    <section className="card dash-stat"     aria-label={t('dashboard.vacation.label')}>
          <div className="dash__label">{t('dashboard.vacation.title', { year })}</div>
      <div
        className={`dash-stat__value dash-stat__value--${data.remaining >= 0 ? 'positive' : 'negative'}`}
        data-testid="vacation-remaining"
      >
        {tp('dashboard.vacation.left', Math.abs(data.remaining) === 1 ? 1 : 2, { count: data.remaining })}
      </div>
      <p className="dash-stat__detail">
        {t('dashboard.vacation.detail', {
          taken: data.taken,
          planned: data.planned,
          entitlement: data.entitlement,
        })}
      </p>
    </section>
  );
}

function WeeklyProgressCard({ progress }: { progress: Dashboard['weeklyProgress'] }) {
  const { t } = useT();
  const tone = progress.hasTarget ? progressTone(progress.percentage) : null;
  const colorClass = tone === 'green' ? 'positive' : tone === 'red' ? 'negative' : 'amber';
  return (
    <section className="card dash-stat"     aria-label={t('dashboard.weekly.title')}>
          <div className="dash__label">{t('dashboard.weekly.title')}</div>
      <div className={`dash-stat__value dash-stat__value--${colorClass}`} data-testid="weekly-percentage">
        {progress.hasTarget ? `${Math.round(progress.percentage)}%` : '–'}
      </div>
      {progress.hasTarget ? (
        <>
          <div
            className="progress"
            role="progressbar"
            aria-label={t('dashboard.weekly.title')}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={Math.min(100, Math.round(progress.percentage))}
          >
            <div
              className={`progress__bar progress__bar--${tone}`}
              style={{ width: `${Math.max(0, Math.min(100, progress.percentage))}%` }}
            />
          </div>
          <div className="dash-stat__detail">
            {formatHours(progress.workedMinutes)} / {formatHours(progress.targetMinutes)}
          </div>
        </>
      ) : (
        <div className="dash-stat__detail">{t('dashboard.weekly.noTarget')}</div>
      )}
    </section>
  );
}

function WeekChart({ week, todayDate }: { week: DaySummary[]; todayDate: string }) {
  const { t } = useT();
  const maxMinutes = Math.max(MIN_CHART_SCALE_MINUTES, ...week.map((d) => Math.max(d.workedMinutes, d.targetMinutes)));
  const nothingLogged = week.every((d) => d.workedMinutes === 0);
  return (
    <section className="card"     aria-label={t('dashboard.week.label')}>
          <h3 className="week__title">{t('dashboard.week.title')}</h3>
      <div className="week__chart">
        {week.map((day) => {
          const label = weekdayShort(parseIsoDate(day.date));
          const isToday = day.date === todayDate;
          const hasTarget = day.targetMinutes > 0;
          return (
            <div
              key={day.date}
              className={`week__col${isToday ? ' week__col--today' : ''}`}
              role="img"
              aria-label={`${label} ${formatHours(day.workedMinutes)}`}
              title={
                hasTarget
                  ? t('dashboard.week.dayTitleTarget', {
                      day: label,
                      worked: formatHours(day.workedMinutes),
                      target: formatHours(day.targetMinutes),
                    })
                  : t('dashboard.week.dayTitle', { day: label, worked: formatHours(day.workedMinutes) })
              }
            >
              <div className="week__bar-area">
                {hasTarget && (
                  <div className="week__target" style={{ bottom: `${(day.targetMinutes / maxMinutes) * 100}%` }} />
                )}
                {day.workedMinutes > 0 ? (
                  <div className="week__bar" style={{ height: `${(day.workedMinutes / maxMinutes) * 100}%` }} />
                ) : (
                  <div className="week__bar week__bar--empty" />
                )}
              </div>
              <span className={`week__day${isToday ? ' week__day--today' : ''}`}>{label}</span>
              <span className="week__hours">{formatHours(day.workedMinutes)}</span>
            </div>
          );
        })}
      </div>
      <p className="week__legend">
        {nothingLogged ? t('dashboard.week.empty') : (
          <>
            <span className="week__legend-line" aria-hidden="true" /> {t('dashboard.week.dailyTarget')}
          </>
        )}
      </p>
    </section>
  );
}
function DashboardSkeleton() {
  const { t } = useT();
  return (
    <div className="dash" role="status" aria-label={t('dashboard.loading')}>
      <div className="dash-hero">
        <div className="dash-hero__main">
          <div className="card skeleton dash-skeleton__today" />
          <div className="skeleton dash-skeleton__action" />
        </div>
        <div className="dash-hero__side">
          <div className="card skeleton dash-skeleton__stat" />
          <div className="card skeleton dash-skeleton__stat" />
        </div>
      </div>
      <div className="card skeleton dash-skeleton__week" />
    </div>
  );
}

export function DashboardPage() {
  const { t } = useT();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [dialogDate, setDialogDate] = useState<string | null>(null);
  const [stopping, setStopping] = useState(false);
  const today = todayIso();
  const { data, isPending, isError, isFetching, refetch } = useQuery({
    queryKey: queryKeys.dashboard,
    queryFn: () => api.getDashboard(),
  });
  const todayEntries = useQuery({
    queryKey: queryKeys.timeEntries(today, today),
    queryFn: () => api.getTimeEntries(today, today),
  });
  const openEntry: TimeEntry | null = (todayEntries.data ?? []).find((e) => !e.endTime && e.startTime) ?? null;

  // The server counts an open booking up to "now"; keep the figures live while one is running.
  useEffect(() => {
    if (!openEntry) return;
    const timer = window.setInterval(() => void refetch(), 60_000);
    return () => window.clearInterval(timer);
  }, [openEntry?.id, refetch]);

  useEffect(() => {
    document.title = t('dashboard.pageTitle');
  }, [t]);

  const stopRunning = async () => {
    if (!openEntry || stopping) return;
    const now = new Date();
    const endTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
    setStopping(true);
    try {
      await api.updateTimeEntry(openEntry.id, {
        date: openEntry.date,
        startTime: trimSeconds(openEntry.startTime),
        endTime,
        breakMinutes: openEntry.breakMinutes,
        notes: openEntry.notes,
      });
      toast.success(t('dashboard.stopped', { time: endTime }));
      await invalidateTimeData(queryClient);
    } catch (e) {
      toast.error(e instanceof ApiError && e.message ? e.message : t('dashboard.stopFailed'));
    } finally {
      setStopping(false);
    }
  };

  return (
    <div className="page dash-page">
      <div className="page__header">
        <div>
          <h2 className="page__title">{t('dashboard.title')}</h2>
          <p className="page__subtitle">{formatLongDate(new Date())}</p>
        </div>
        <Button
          variant="tertiary"
          className="dash__refresh"
          onClick={() => void refetch()}
          disabled={isFetching}
        >
          <Icon name="refresh" className={isFetching ? 'is-spinning' : undefined} />
          {t('dashboard.refresh')}
        </Button>
      </div>

      {isPending ? (
        <DashboardSkeleton />
      ) : isError || !data ? (
        <div className="dash">
          <div className="notice notice--error" role="alert">
            {t('dashboard.loadError')}
          </div>
          <div>
            <Button onClick={() => void refetch()}>{t('dashboard.retry')}</Button>
          </div>
        </div>
      ) : (
        <div className="dash">
          <div className="dash-hero">
            <div className="dash-hero__main">
              <TodayCard today={data.today} openEntry={openEntry} onStop={() => void stopRunning()} stopping={stopping} />

              <div className="dash-actions">
                <Button variant="primary" onClick={() => setDialogDate(today)}>
                  <Icon name="clock" />
                  {t('dashboard.logTime')}
                </Button>
                <Button variant="tertiary" onClick={() => navigate('/month')}>
                  <Icon name="calendar" />
                  {t('dashboard.viewMonth')}
                </Button>
              </div>
            </div>

            <div className="dash-hero__side">
              <BalanceCard minutes={data.currentBalanceMinutes} year={today.slice(0, 4)} />
              <WeeklyProgressCard progress={data.weeklyProgress} />
              <VacationCard year={Number(today.slice(0, 4))} />
            </div>
          </div>
          <WeekChart week={data.week} todayDate={today} />
        </div>
      )}

      <TimeEntryDialog date={dialogDate} onClose={() => setDialogDate(null)} />
    </div>
  );
}
