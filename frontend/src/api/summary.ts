import client from './client'

export interface DaySummary {
  date: string
  isWorkday: boolean
  workedMinutes: number
  targetMinutes: number
  balanceMinutes: number
  absenceType: string | null
  absenceLabel: string | null
}

export interface WeeklyProgress {
  workedMinutes: number
  targetMinutes: number
  percentage: number
  hasTarget: boolean
}

export async function fetchMonthSummary(year: number, month: number): Promise<DaySummary[]> {
  const { data } = await client.get<DaySummary[]>('/summary/month', { params: { year, month } })
  return data
}

export async function fetchWeeklyProgress(): Promise<WeeklyProgress> {
  const { data } = await client.get<WeeklyProgress>('/summary/weekly-progress')
  return data
}
