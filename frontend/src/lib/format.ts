/** Duration formatting helpers. All inputs are minutes. */

function split(minutes: number): { sign: string; hours: number; mins: number } {
  const abs = Math.abs(Math.trunc(minutes));
  return { sign: minutes < 0 ? '-' : '', hours: Math.floor(abs / 60), mins: abs % 60 };
}

/** "7:48 h", "-0:58 h" – used in the month view and statistics. */
export function formatMinutes(minutes: number): string {
  const { sign, hours, mins } = split(minutes);
  return `${sign}${hours}:${String(mins).padStart(2, '0')} h`;
}

/** "+0:22 h" / "-0:58 h" – signed variant of {@link formatMinutes}. */
export function formatBalance(minutes: number): string {
  return `${minutes >= 0 ? '+' : ''}${formatMinutes(minutes)}`;
}

/** "7:48" without unit – used for large dashboard numbers. */
export function formatClock(minutes: number): string {
  const { hours, mins } = split(minutes);
  return `${hours}:${String(mins).padStart(2, '0')}`;
}

/** "+0:22" / "-0:58"; "0:00" is rendered with a plus sign like on the dashboard today card. */
export function formatSignedClock(minutes: number): string {
  const { sign, hours, mins } = split(minutes);
  return `${minutes >= 0 ? '+' : sign}${hours}:${String(mins).padStart(2, '0')}`;
}

/** "−1,723 h 48 min" – long-form signed duration with a real minus sign; zero is unsigned. */
export function formatLongBalance(minutes: number): string {
  const { hours, mins } = split(minutes);
  const sign = minutes < 0 ? '\u2212' : minutes > 0 ? '+' : '';
  return `${sign}${hours.toLocaleString('en-US')} h ${mins} min`;
}

/** "8h" or "8:10h" – used for chart labels and weekly progress. */
export function formatHours(minutes: number): string {
  const { hours, mins } = split(minutes);
  return mins === 0 ? `${hours}h` : `${hours}:${String(mins).padStart(2, '0')}h`;
}

/** Normalizes "HH:mm:ss" to "HH:mm". */
export function trimSeconds(time: string | null | undefined): string {
  return time ? time.slice(0, 5) : '';
}

export function worked(startTime: string | null, endTime: string | null, breakMinutes: number | null): number {
  if (!startTime || !endTime) return 0;
  const toMin = (t: string) => Number(t.slice(0, 2)) * 60 + Number(t.slice(3, 5));
  return Math.max(0, toMin(endTime) - toMin(startTime) - (breakMinutes ?? 0));
}

const TIME_24H = /^([01]\d|2[0-3]):[0-5]\d$/;

/** True for a valid 24-hour "HH:MM" value. */
export function isTime24h(value: string): boolean {
  return TIME_24H.test(value);
}

/** Lenient 24-hour input: "9" -> "09:00", "930" -> "09:30", "9:5" -> "09:05". Unparseable input is returned as typed. */
export function normalizeTime(raw: string): string {
  const value = raw.trim();
  let hours: string;
  let mins: string;
  const colon = /^(\d{1,2})[:.](\d{1,2})$/.exec(value);
  if (colon) {
    [, hours, mins] = colon;
  } else if (/^\d{1,2}$/.test(value)) {
    hours = value;
    mins = '0';
  } else if (/^\d{3,4}$/.test(value)) {
    hours = value.slice(0, -2);
    mins = value.slice(-2);
  } else {
    return raw;
  }
  const candidate = `${hours.padStart(2, '0')}:${mins.padStart(2, '0')}`;
  return isTime24h(candidate) ? candidate : raw;
}
