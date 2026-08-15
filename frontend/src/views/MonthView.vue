<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import AbsenceDialog from '../components/AbsenceDialog.vue'
import MonthCalendar from '../components/MonthCalendar.vue'
import TimeEntryDialog from '../components/TimeEntryDialog.vue'
import type { Absence } from '../api/absences'
import type { DaySummary } from '../api/summary'
import type { TimeEntry } from '../api/timeEntries'
import { useAbsenceStore } from '../stores/absence'
import { useSummaryStore } from '../stores/summary'
import { useTimeEntryStore } from '../stores/timeEntry'

const route = useRoute()
const router = useRouter()
const timeEntryStore = useTimeEntryStore()
const absenceStore = useAbsenceStore()
const summaryStore = useSummaryStore()

const dialogMode = ref<'time' | 'absence'>('time')
const selectedDay = ref<string>('')
const selectedEntry = ref<TimeEntry | null>(null)
const timeDialogOpen = ref(false)
const absenceDialogOpen = ref(false)

const currentYear = computed(() => Number(route.params.year))
const currentMonth = computed(() => Number(route.params.month))
const pageError = computed(() => timeEntryStore.error ?? absenceStore.error ?? summaryStore.error)

const monthTitle = computed(() => {
  if (!currentYear.value || !currentMonth.value) {
    return ''
  }
  return new Intl.DateTimeFormat(undefined, {
    month: 'long',
    year: 'numeric'
  })
    .format(new Date(currentYear.value, currentMonth.value - 1, 1))
    .toUpperCase()
})

const selectedDayEntries = computed(() =>
  timeEntryStore.entries.filter((entry) => entry.date === selectedDay.value)
)
const selectedAbsence = computed<Absence | null>(() =>
  absenceStore.absences.find((absence) => absence.date === selectedDay.value) ?? null
)
const monthWorkedMinutes = computed(() => summaryStore.monthSummaries.reduce((sum, day) => sum + day.workedMinutes, 0))
const monthTargetMinutes = computed(() => summaryStore.monthSummaries.reduce((sum, day) => sum + day.targetMinutes, 0))
const monthBalanceMinutes = computed(() => summaryStore.monthSummaries.reduce((sum, day) => sum + day.balanceMinutes, 0))

function formatMinutes(minutes: number): string {
  const absolute = Math.abs(minutes)
  const hours = Math.floor(absolute / 60)
  const remainder = absolute % 60
  return `${hours}h ${remainder}m`
}

function formatBalance(minutes: number): string {
  return `${minutes >= 0 ? '+' : '-'}${formatMinutes(minutes)}`
}

async function loadMonth() {
  if (!Number.isInteger(currentYear.value) || !Number.isInteger(currentMonth.value) || currentMonth.value < 1 || currentMonth.value > 12) {
    const now = new Date()
    await router.replace(`/month/${now.getFullYear()}/${now.getMonth() + 1}`)
    return
  }

  await Promise.all([
    summaryStore.fetchMonth(currentYear.value, currentMonth.value),
    summaryStore.fetchYearlyBalance(currentYear.value, currentMonth.value),
    timeEntryStore.fetchEntries(currentYear.value, currentMonth.value),
    absenceStore.fetchAbsences(currentYear.value, currentMonth.value)
  ])
}

function navigateMonth(offset: number) {
  const date = new Date(currentYear.value, currentMonth.value - 1 + offset, 1)
  void router.push(`/month/${date.getFullYear()}/${date.getMonth() + 1}`)
}

function handleDaySelection(summary: DaySummary) {
  selectedDay.value = summary.date
  selectedEntry.value = selectedDayEntries.value[0] ?? null

  if (summary.absenceType || dialogMode.value === 'absence') {
    absenceDialogOpen.value = true
    return
  }

  timeDialogOpen.value = true
}

async function refreshCurrentMonth() {
  await Promise.all([
    summaryStore.fetchMonth(currentYear.value, currentMonth.value),
    summaryStore.fetchYearlyBalance(currentYear.value, currentMonth.value),
    timeEntryStore.fetchEntries(currentYear.value, currentMonth.value),
    absenceStore.fetchAbsences(currentYear.value, currentMonth.value)
  ])
}

async function handleEntrySaved(entry: TimeEntry) {
  await timeEntryStore.saveEntry(entry)
  await refreshCurrentMonth()
  timeDialogOpen.value = false
}

async function handleEntryDeleted(id: string) {
  await timeEntryStore.deleteEntry(id)
  await refreshCurrentMonth()
  timeDialogOpen.value = false
}

async function handleAbsenceSaved(payload: { startDate: string; endDate: string; absenceType: 'VACATION' | 'SICK' }) {
  await absenceStore.saveAbsence(payload)
  await refreshCurrentMonth()
  absenceDialogOpen.value = false
}

async function handleAbsenceDeleted(date: string) {
  await absenceStore.deleteAbsence(date)
  await refreshCurrentMonth()
  absenceDialogOpen.value = false
}

watch([currentYear, currentMonth], () => {
  void loadMonth()
}, { immediate: true })
</script>

<template>
  <AppLayout>
    <div class="stack-lg">
      <div class="section-header month-toolbar">
        <button class="btn btn-secondary" type="button" @click="navigateMonth(-1)">←</button>
        <h1>{{ monthTitle }}</h1>
        <button class="btn btn-secondary" type="button" @click="navigateMonth(1)">→</button>
      </div>

      <section class="card">
        <div class="section-header section-header--compact">
          <div>
            <p class="eyebrow">Month overview</p>
            <h2>Statistics</h2>
          </div>
          <div class="segmented-control">
            <button
              class="btn"
              :class="dialogMode === 'time' ? 'btn-primary' : 'btn-secondary'"
              type="button"
              @click="dialogMode = 'time'"
            >
              Time mode
            </button>
            <button
              class="btn"
              :class="dialogMode === 'absence' ? 'btn-primary' : 'btn-secondary'"
              type="button"
              @click="dialogMode = 'absence'"
            >
              Absence mode
            </button>
          </div>
        </div>

        <div class="stats-grid stats-grid--four">
          <div>
            <span class="muted">Worked</span>
            <strong>{{ formatMinutes(monthWorkedMinutes) }}</strong>
          </div>
          <div>
            <span class="muted">Target</span>
            <strong>{{ formatMinutes(monthTargetMinutes) }}</strong>
          </div>
          <div>
            <span class="muted">Month balance</span>
            <strong :class="monthBalanceMinutes >= 0 ? 'text-success' : 'text-danger'">{{ formatBalance(monthBalanceMinutes) }}</strong>
          </div>
          <div>
            <span class="muted">Yearly balance</span>
            <strong :class="summaryStore.yearlyBalanceMinutes >= 0 ? 'text-success' : 'text-danger'">
              {{ formatBalance(summaryStore.yearlyBalanceMinutes) }}
            </strong>
          </div>
        </div>
      </section>

      <p v-if="pageError" class="error-message">{{ pageError }}</p>

      <MonthCalendar
        :year="currentYear"
        :month="currentMonth"
        :summaries="summaryStore.monthSummaries"
        @select-day="handleDaySelection"
      />
    </div>

    <TimeEntryDialog
      :open="timeDialogOpen"
      :date="selectedDay"
      :entry="selectedEntry"
      :entries="selectedDayEntries"
      @saved="handleEntrySaved"
      @deleted="handleEntryDeleted"
      @closed="timeDialogOpen = false"
    />

    <AbsenceDialog
      :open="absenceDialogOpen"
      :date="selectedDay"
      :absence="selectedAbsence"
      @saved="handleAbsenceSaved"
      @deleted="handleAbsenceDeleted"
      @closed="absenceDialogOpen = false"
    />
  </AppLayout>
</template>
