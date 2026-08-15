<script setup lang="ts">
import { computed } from 'vue'
import type { DaySummary } from '../api/summary'

const props = defineProps<{
  summary: DaySummary
  isToday: boolean
}>()

const emit = defineEmits<{
  (event: 'select', summary: DaySummary): void
}>()

function formatWorkedMinutes(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const remainder = minutes % 60
  return `${hours}h ${remainder}m`
}

function formatBalance(minutes: number): string {
  const absolute = Math.abs(minutes)
  const hours = Math.floor(absolute / 60)
  const remainder = absolute % 60
  return `${minutes >= 0 ? '+' : '-'}${hours}h ${remainder}m`
}

const dayNumber = computed(() => Number(props.summary.date.slice(-2)))
const isWeekend = computed(() => !props.summary.isWorkday)
const absenceClass = computed(() => {
  const type = props.summary.absenceType ?? ''
  if (type.includes('SICK')) {
    return 'day-badge badge-sick'
  }
  if (type.includes('VACATION')) {
    return 'day-badge badge-vacation'
  }
  return 'day-badge badge-holiday'
})
</script>

<template>
  <button
    class="day-card"
    :class="{ 'day-card--today': isToday, 'day-card--weekend': isWeekend }"
    type="button"
    @click="emit('select', summary)"
  >
    <div class="day-header">
      <div class="day-number-section">
        <span class="day-number">{{ dayNumber }}</span>
      </div>
    </div>
    <div class="day-content">
      <span v-if="summary.absenceLabel" :class="absenceClass">{{ summary.absenceLabel }}</span>
      <span v-else-if="summary.workedMinutes > 0" class="worked-hours">{{ formatWorkedMinutes(summary.workedMinutes) }}</span>
      <span v-else class="muted">No entries</span>
      <span v-if="summary.targetMinutes > 0 || summary.balanceMinutes !== 0" class="balance" :class="summary.balanceMinutes >= 0 ? 'balance-positive' : 'balance-negative'">
        {{ formatBalance(summary.balanceMinutes) }}
      </span>
    </div>
  </button>
</template>
