<script setup lang="ts">
import { computed } from 'vue'
import type { TimeEntry } from '../api/timeEntries'

const props = defineProps<{
  date: string
  entries: TimeEntry[]
}>()

const emit = defineEmits<{
  (event: 'new-entry'): void
  (event: 'edit-entry', entry: TimeEntry): void
}>()

function toMinutes(value: string): number {
  const [hours, minutes] = value.split(':').map(Number)
  return hours * 60 + minutes
}

function getEntryMinutes(entry: TimeEntry): number {
  const endTime = entry.endTime ?? new Date().toTimeString().slice(0, 5)
  const duration = toMinutes(endTime) - toMinutes(entry.startTime) - entry.breakMinutes
  return Math.max(duration, 0)
}

function formatMinutes(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const remainder = minutes % 60
  return `${hours}h ${remainder}m`
}

function formatDate(date: string): string {
  return new Intl.DateTimeFormat(undefined, {
    weekday: 'long',
    day: 'numeric',
    month: 'long'
  }).format(new Date(`${date}T00:00:00`))
}

const totalMinutes = computed(() => props.entries.reduce((sum, entry) => sum + getEntryMinutes(entry), 0))
</script>

<template>
  <section class="card summary-card">
    <div class="section-header">
      <div>
        <p class="eyebrow">Today</p>
        <h2>{{ formatDate(date) }}</h2>
      </div>
      <button class="btn btn-primary" type="button" @click="emit('new-entry')">+ New Entry</button>
    </div>

    <p v-if="entries.length" class="muted">Total worked: {{ formatMinutes(totalMinutes) }}</p>
    <p v-else class="muted">No time entries yet. Add the first one for today.</p>

    <div class="entry-list">
      <button
        v-for="entry in entries"
        :key="entry.id ?? `${entry.date}-${entry.startTime}`"
        class="entry-row"
        type="button"
        @click="emit('edit-entry', entry)"
      >
        <div>
          <div class="entry-row__time">{{ entry.startTime }} – {{ entry.endTime ?? 'Running' }}</div>
          <div class="entry-row__meta">Break {{ entry.breakMinutes }} min</div>
        </div>
        <div class="entry-row__side">
          <span class="entry-row__duration">{{ formatMinutes(getEntryMinutes(entry)) }}</span>
          <span v-if="entry.notes" class="entry-row__notes">{{ entry.notes }}</span>
        </div>
      </button>
    </div>
  </section>
</template>
