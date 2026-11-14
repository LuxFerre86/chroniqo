import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { api } from '../api/endpoints';
import { Button } from '../components/Button';
import { Icon } from '../components/Icon';
import { TimeEntryDialog } from '../components/TimeEntryDialog';
import { formatClock, formatHours, formatLongBalance, formatSignedClock } from '../lib/format';
import { formatLongDate, parseIsoDate, todayIso, weekdayShort } from '../lib/dates';
import { queryKeys } from '../lib/queries';
import type { Dashboard, DaySummary } from '../api/types';
import '../styles/dashboard.css';

const MIN_CHART_SCALE_MINUTES = 480;

function progressTone(percentage: number): 'green' | 'amber' | 'red' {
  if (percentage >= 100) return 'green';
  if (percentage >= 80) return 'amber';
  return 'red';
}

const RING_RADIUS = 84;
const RING_CIRCUMFERENCE = 2 * Math.PI * RING_RADIUS;

function TodayCard({ today }: { today: DaySummary | null }) {
  const workedMinutes = today?.workedMinutes ?? 0;
  const targetMinutes = today?.targetMinutes ?? 0;
  const balance = today?.balanceMinutes ?? 0;
  const hasTarget = targetMinutes > 0;
  const ratio = hasTarget ? Math.min(1, workedMinutes / targetMinutes) : 0;
  return (
    <section className="card dash-today" aria-label="Today">
      <div className="dash-today__label">Today</div>
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
            {hasTarget ? `of ${formatClock(targetMinutes)} h target` : 'hours worked'}
          </div>
        </div>
      </div>
      <div
        className={`dash-today__balance dash-today__balance--${balance >= 0 ? 'positive' : 'negative'}`}
        data-testid="today-balance"
      >
        {formatSignedClock(balance)}
      </div>
    </section>
  );
}
function BalanceCard({ minutes, year }: { minutes: number; year: string }) {
  return (
    <section className="card dash-stat" aria-label="Current balance">
      <div className="dash__label">Current balance ({year})</div>
      <div
        className={`dash-stat__value dash-stat__value--${minutes >= 0 ? 'positive' : 'negative'}`}
        data-testid="current-balance"
      >
        {formatLongBalance(minutes)}
      </div>
    </section>
  );
}

function WeeklyProgressCard({ progress }: { progress: Dashboard['weeklyProgress'] }) {
  const tone = progress.hasTarget ? progressTone(progress.percentage) : null;
  const colorClass = tone === 'green' ? 'positive' : tone === 'red' ? 'negative' : 'amber';
  return (
    <section className="card dash-stat" aria-label="Weekly progress">
      <div className="dash__label">Weekly progress</div>
      <div className={`dash-stat__value dash-stat__value--${colorClass}`} data-testid="weekly-percentage">
        {progress.hasTarget ? `${Math.round(progress.percentage)}%` : '–'}
      </div>
      {progress.hasTarget ? (
        <>
          <div
            className="progress"
            role="progressbar"
            aria-label="Weekly progress"
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
        <div className="dash-stat__detail">No target this week</div>
      )}
    </section>
  );
}

function WeekChart({ week, todayDate }: { week: DaySummary[]; todayDate: string }) {
  const maxMinutes = Math.max(MIN_CHART_SCALE_MINUTES, ...week.map((d) => Math.max(d.workedMinutes, d.targetMinutes)));
  const nothingLogged = week.every((d) => d.workedMinutes === 0);
  return (
    <section className="card" aria-label="Current week">
      <h3 className="week__title">Current Week</h3>
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
              title={`${label}: ${formatHours(day.workedMinutes)}${hasTarget ? ` of ${formatHours(day.targetMinutes)}` : ''}`}
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
        {nothingLogged ? 'Nothing logged this week yet – use Log Time to add your first booking.' : (
          <>
            <span className="week__legend-line" aria-hidden="true" /> Daily target
          </>
        )}
      </p>
    </section>
  );
}
function DashboardSkeleton() {
  return (
    <div className="dash" role="status" aria-label="Loading dashboard">
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
  const navigate = useNavigate();
  const [dialogDate, setDialogDate] = useState<string | null>(null);
  const { data, isPending, isError, isFetching, refetch } = useQuery({
    queryKey: queryKeys.dashboard,
    queryFn: () => api.getDashboard(),
  });

  useEffect(() => {
    document.title = 'Dashboard | chroniqo';
  }, []);

  const today = todayIso();

  return (
    <div className="page dash-page">
      <div className="page__header">
        <div>
          <h2 className="page__title">Dashboard</h2>
          <p className="page__subtitle">{formatLongDate(new Date())}</p>
        </div>
        <Button
          variant="tertiary"
          className="dash__refresh"
          onClick={() => void refetch()}
          disabled={isFetching}
        >
          <Icon name="refresh" className={isFetching ? 'is-spinning' : undefined} />
          Refresh
        </Button>
      </div>

      {isPending ? (
        <DashboardSkeleton />
      ) : isError || !data ? (
        <div className="dash">
          <div className="notice notice--error" role="alert">
            The dashboard could not be loaded.
          </div>
          <div>
            <Button onClick={() => void refetch()}>Retry</Button>
          </div>
        </div>
      ) : (
        <div className="dash">
          <div className="dash-hero">
            <div className="dash-hero__main">
              <TodayCard today={data.today} />

              <div className="dash-actions">
                <Button variant="primary" onClick={() => setDialogDate(today)}>
                  <Icon name="clock" />
                  Log Time
                </Button>
                <Button variant="tertiary" onClick={() => navigate('/month')}>
                  <Icon name="calendar" />
                  View Month
                </Button>
              </div>
            </div>

            <div className="dash-hero__side">
              <BalanceCard minutes={data.currentBalanceMinutes} year={today.slice(0, 4)} />
              <WeeklyProgressCard progress={data.weeklyProgress} />
            </div>
          </div>
          <WeekChart week={data.week} todayDate={today} />
        </div>
      )}

      <TimeEntryDialog date={dialogDate} onClose={() => setDialogDate(null)} />
    </div>
  );
}
