import { defineStore } from 'pinia'
import { fetchMonthSummary, fetchWeeklyProgress, type DaySummary, type WeeklyProgress } from '../api/summary'
import { getErrorMessage } from '../api/client'

const defaultWeeklyProgress: WeeklyProgress = {
  workedMinutes: 0,
  targetMinutes: 0,
  percentage: 0,
  hasTarget: false
}

export const useSummaryStore = defineStore('summary', {
  state: () => ({
    monthSummaries: [] as DaySummary[],
    weeklyProgress: defaultWeeklyProgress as WeeklyProgress,
    yearlyBalanceMinutes: 0,
    loadingMonth: false,
    loadingWeekly: false,
    error: null as string | null
  }),
  actions: {
    async fetchMonth(year: number, month: number) {
      this.loadingMonth = true
      this.error = null
      try {
        this.monthSummaries = await fetchMonthSummary(year, month)
      } catch (error) {
        this.error = getErrorMessage(error, 'Could not load the monthly summary.')
        throw error
      } finally {
        this.loadingMonth = false
      }
    },
    async fetchWeekly() {
      this.loadingWeekly = true
      this.error = null
      try {
        this.weeklyProgress = await fetchWeeklyProgress()
      } catch (error) {
        this.error = getErrorMessage(error, 'Could not load weekly progress.')
        throw error
      } finally {
        this.loadingWeekly = false
      }
    },
    async fetchYearlyBalance(year: number, upToMonth = 12) {
      this.error = null
      try {
        const months = Array.from({ length: upToMonth }, (_, index) => index + 1)
        const summaries = await Promise.all(months.map((month) => fetchMonthSummary(year, month)))
        this.yearlyBalanceMinutes = summaries
          .flat()
          .reduce((total, day) => total + day.balanceMinutes, 0)
      } catch (error) {
        this.error = getErrorMessage(error, 'Could not load the yearly balance.')
        throw error
      }
    }
  }
})
