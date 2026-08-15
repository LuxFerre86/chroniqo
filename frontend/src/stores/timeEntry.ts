import { defineStore } from 'pinia'
import { createTimeEntry, fetchTimeEntries, removeTimeEntry, updateTimeEntry, type TimeEntry } from '../api/timeEntries'
import { getErrorMessage } from '../api/client'

function toMinutes(time: string | null): number | null {
  if (!time) {
    return null
  }
  const [hours, minutes] = time.split(':').map(Number)
  return hours * 60 + minutes
}

function sortEntries(entries: TimeEntry[]): TimeEntry[] {
  return [...entries].sort((left, right) => {
    if (left.date !== right.date) {
      return left.date.localeCompare(right.date)
    }
    return (toMinutes(left.startTime) ?? 0) - (toMinutes(right.startTime) ?? 0)
  })
}

export const useTimeEntryStore = defineStore('time-entries', {
  state: () => ({
    entries: [] as TimeEntry[],
    loading: false,
    error: null as string | null
  }),
  actions: {
    async fetchEntries(year: number, month: number) {
      this.loading = true
      this.error = null
      try {
        this.entries = sortEntries(await fetchTimeEntries(year, month))
      } catch (error) {
        this.error = getErrorMessage(error, 'Could not load time entries.')
        throw error
      } finally {
        this.loading = false
      }
    },
    async saveEntry(entry: TimeEntry) {
      this.error = null
      try {
        const payload = {
          date: entry.date,
          startTime: entry.startTime,
          endTime: entry.endTime,
          breakMinutes: entry.breakMinutes,
          notes: entry.notes
        }
        const saved = entry.id
          ? await updateTimeEntry(entry.id, payload)
          : await createTimeEntry(payload)
        const otherEntries = this.entries.filter((item) => item.id !== entry.id)
        this.entries = sortEntries([...otherEntries, saved])
        return saved
      } catch (error) {
        this.error = getErrorMessage(error, 'Could not save the time entry.')
        throw error
      }
    },
    async deleteEntry(id: string) {
      this.error = null
      try {
        await removeTimeEntry(id)
        this.entries = this.entries.filter((entry) => entry.id !== id)
      } catch (error) {
        this.error = getErrorMessage(error, 'Could not delete the time entry.')
        throw error
      }
    }
  }
})
