<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import QuickStatsWidget from '../components/QuickStatsWidget.vue'
import TimeEntryDialog from '../components/TimeEntryDialog.vue'
import TodaySummaryCard from '../components/TodaySummaryCard.vue'
import WeekChartWidget from '../components/WeekChartWidget.vue'
import type { TimeEntry } from '../api/timeEntries'
import { useSummaryStore } from '../stores/summary'
import { useTimeEntryStore } from '../stores/timeEntry'

const timeEntryStore = useTimeEntryStore()
const summaryStore = useSummaryStore()
const dialogOpen = ref(false)
const selectedEntry = ref<TimeEntry | null>(null)

const today = new Date()
const todayIso = today.toISOString().slice(0, 10)
const year = today.getFullYear()
const month = today.getMonth() + 1

const todayEntries = computed(() => timeEntryStore.entries.filter((entry) => entry.date === todayIso))
const pageError = computed(() => timeEntryStore.error ?? summaryStore.error)

async function loadDashboard() {
  await Promise.all([
    timeEntryStore.fetchEntries(year, month),
    summaryStore.fetchMonth(year, month),
    summaryStore.fetchWeekly()
  ])
}

function openNewEntry() {
  selectedEntry.value = null
  dialogOpen.value = true
}

function openExistingEntry(entry: TimeEntry) {
  selectedEntry.value = entry
  dialogOpen.value = true
}

async function handleSaved(entry: TimeEntry) {
  await timeEntryStore.saveEntry(entry)
  await Promise.all([summaryStore.fetchMonth(year, month), summaryStore.fetchWeekly()])
  dialogOpen.value = false
}

async function handleDeleted(id: string) {
  await timeEntryStore.deleteEntry(id)
  await Promise.all([summaryStore.fetchMonth(year, month), summaryStore.fetchWeekly()])
  dialogOpen.value = false
}

onMounted(() => {
  void loadDashboard()
})
</script>

<template>
  <AppLayout>
    <div class="stack-lg">
      <p v-if="pageError" class="error-message">{{ pageError }}</p>

      <TodaySummaryCard :date="todayIso" :entries="todayEntries" @new-entry="openNewEntry" @edit-entry="openExistingEntry" />

      <div class="dashboard-grid">
        <WeekChartWidget :weekly-progress="summaryStore.weeklyProgress" />
        <QuickStatsWidget :month-summaries="summaryStore.monthSummaries" />
      </div>
    </div>

    <TimeEntryDialog
      :open="dialogOpen"
      :date="todayIso"
      :entry="selectedEntry"
      :entries="todayEntries"
      @saved="handleSaved"
      @deleted="handleDeleted"
      @closed="dialogOpen = false"
    />
  </AppLayout>
</template>
