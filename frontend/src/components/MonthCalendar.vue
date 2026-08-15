<script setup lang="ts">
import { computed } from 'vue'
import type { DaySummary } from '../api/summary'
import DayCell from './DayCell.vue'

const props = defineProps<{
  year: number
  month: number
  summaries: DaySummary[]
}>()

const emit = defineEmits<{
  (event: 'select-day', summary: DaySummary): void
}>()

const weekdayLabels = ['Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa', 'So']
const todayIso = new Date().toISOString().slice(0, 10)

const leadingEmptyCells = computed(() => {
  const firstDay = new Date(props.year, props.month - 1, 1)
  return (firstDay.getDay() + 6) % 7
})
</script>

<template>
  <section class="card">
    <div class="calendar-grid">
      <div v-for="label in weekdayLabels" :key="label" class="calendar-header">{{ label }}</div>
      <div v-for="index in leadingEmptyCells" :key="`empty-${index}`" class="calendar-empty" />
      <DayCell
        v-for="summary in summaries"
        :key="summary.date"
        :summary="summary"
        :is-today="summary.date === todayIso"
        @select="emit('select-day', $event)"
      />
    </div>
  </section>
</template>
