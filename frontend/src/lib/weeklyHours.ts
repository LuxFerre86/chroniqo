export const MAX_WEEKLY_HOURS = 80;

/** Valid when within 0–80 and given with at most one decimal place (e.g. 37.5). */
export function isValidWeeklyHours(hours: number | null): hours is number {
  if (hours === null || !Number.isFinite(hours)) return false;
  if (hours < 0 || hours > MAX_WEEKLY_HOURS) return false;
  return Math.abs(Math.round(hours * 10) - hours * 10) < 1e-6;
}
