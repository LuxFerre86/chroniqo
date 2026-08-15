<script setup lang="ts">
import { computed } from 'vue'
import type { DaySummary } from '../api/summary'

const props = defineProps<{
  monthSummaries: DaySummary[]
}>()

function formatMinutes(minutes: number): string {
  const absolute = Math.abs(minutes)
  const hours = Math.floor(absolute / 60)
  const remainder = absolute % 60
  return `${hours}h ${remainder}m`
}

function formatBalance(minutes: number): string {
  const prefix = minutes >= 0 ? '+' : '-'
  return `${prefix}${formatMinutes(minutes)}`
}

const workedMinutes = computed(() => props.monthSummaries.reduce((sum, day) => sum + day.workedMinutes, 0))
const targetMinutes = computed(() => props.monthSummaries.reduce((sum, day) => sum + day.targetMinutes, 0))
const balanceMinutes = computed(() => props.monthSummaries.reduce((sum, day) => sum + day.balanceMinutes, 0))
const trackedDays = computed(() => props.monthSummaries.filter((day) => day.workedMinutes > 0 || day.absenceType).length)
</script>

<template>
  <section class="card widget-card">
    <div class="section-header section-header--compact">
      <div>
        <p class="eyebrow">Month snapshot</p>
        <h3>Quick stats</h3>
      </div>
    </div>

    <div class="stats-grid">
      <div>
        <span class="muted">Worked</span>
        <strong>{{ formatMinutes(workedMinutes) }}</strong>
      </div>
      <div>
        <span class="muted">Target</span>
        <strong>{{ formatMinutes(targetMinutes) }}</strong>
      </div>
      <div>
        <span class="muted">Tracked days</span>
        <strong>{{ trackedDays }}</strong>
      </div>
      <div>
        <span class="muted">Balance</span>
        <strong :class="balanceMinutes >= 0 ? 'text-success' : 'text-danger'">{{ formatBalance(balanceMinutes) }}</strong>
      </div>
    </div>
  </section>
</template>
