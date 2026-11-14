import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { api } from '../api/endpoints';
import { Button } from '../components/Button';
import { Icon } from '../components/Icon';
import { TimeEntryDialog } from '../components/TimeEntryDialog';
import { formatClock, formatHours, formatRunningBalance, formatSignedClock } from '../lib/format';
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

function TodayCard({ today }: { today: DaySummary | null }) {
  const workedMinutes = today?.workedMinutes ?? 0;
  const balance = today?.balanceMinutes ?? 0;
  return (
    <section className="card dash-today" aria-label="Today">
      <div className="dash-today__label">Today</div>
      <div className="dash-today__value" data-testid="today-worked">
        {formatClock(workedMinutes)}
      </div>
      <div className="dash-today__caption">hours worked</div>
      <div
        className={`dash-today__balance dash-today__balance--${balance >= 0 ? 'positive' : 'negative'}`}
        data-testid="today-balance"
      >
        {formatSignedClock(balance)}
      </div>
    </section>
  );
}

function BalanceCard({ minutes }: { minutes: number }) {
  return (
    <section className="card dash-stat" aria-label="Current balance">
      <div className="dash__label">Current balance</div>
      <div
        className={`dash-stat__value dash-stat__value--${minutes >= 0 ? 'positive' : 'negative'}`}
        data-testid="current-balance"
      >
        {formatRunningBalance(minutes)}
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
  const maxMinutes = Math.max(MIN_CHART_SCALE_MINUTES, ...week.map((d) => d.workedMinutes));
  return (
    <section className="card" aria-label="Current week">
      <h3 className="week__title">Current Week</h3>
      <div className="week__chart">
        {week.map((day) => {
          const label = weekdayShort(parseIsoDate(day.date));
          return (
            <div
              key={day.date}
              className="week__col"
              role="img"
              aria-label={`${label} ${formatHours(day.workedMinutes)}`}
            >
              <div className="week__bar-area">
                {day.workedMinutes > 0 ? (
                  <div className="week__bar" style={{ height: `${(day.workedMinutes / maxMinutes) * 100}%` }} />
                ) : (
                  <div className="week__bar week__bar--empty" />
                )}
              </div>
              <span className={`week__day${day.date === todayDate ? ' week__day--today' : ''}`}>{label}</span>
              <span className="week__hours">{formatHours(day.workedMinutes)}</span>
            </div>
          );
        })}
      </div>
    </section>
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
    <div className="page">
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
        <div className="centered-state" role="status">
          <span className="spinner" aria-hidden="true" />
          Loading…
        </div>
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
          <TodayCard today={data.today} />

          <div className="dash-actions">
            <Button variant="primary" onClick={() => setDialogDate(today)}>
              <Icon name="clock" />
              Log Time
            </Button>
            <Button onClick={() => navigate('/month')}>
              <Icon name="calendar" />
              View Month
            </Button>
          </div>

          <div className="dash-stats">
            <BalanceCard minutes={data.currentBalanceMinutes} />
            <WeeklyProgressCard progress={data.weeklyProgress} />
          </div>

          <WeekChart week={data.week} todayDate={today} />
        </div>
      )}

      <TimeEntryDialog date={dialogDate} onClose={() => setDialogDate(null)} />
    </div>
  );
}
