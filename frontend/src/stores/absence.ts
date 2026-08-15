import { defineStore } from 'pinia'
import { deleteAbsence, fetchAbsences, saveAbsence, type Absence, type AbsencePayload } from '../api/absences'
import { getErrorMessage } from '../api/client'

export const useAbsenceStore = defineStore('absences', {
  state: () => ({
    absences: [] as Absence[],
    loading: false,
    error: null as string | null
  }),
  actions: {
    async fetchAbsences(year: number, month: number) {
      this.loading = true
      this.error = null
      try {
        this.absences = await fetchAbsences(year, month)
      } catch (error) {
        this.error = getErrorMessage(error, 'Could not load absences.')
        throw error
      } finally {
        this.loading = false
      }
    },
    async saveAbsence(payload: AbsencePayload) {
      this.error = null
      try {
        await saveAbsence(payload)
      } catch (error) {
        this.error = getErrorMessage(error, 'Could not save the absence.')
        throw error
      }
    },
    async deleteAbsence(date: string) {
      this.error = null
      try {
        await deleteAbsence(date)
        this.absences = this.absences.filter((absence) => absence.date !== date)
      } catch (error) {
        this.error = getErrorMessage(error, 'Could not delete the absence.')
        throw error
      }
    }
  }
})
