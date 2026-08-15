<script setup lang="ts">
import { computed } from 'vue'
import type { WeeklyProgress } from '../api/summary'

const props = defineProps<{
  weeklyProgress: WeeklyProgress
}>()

function formatMinutes(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const remainder = minutes % 60
  return `${hours}h ${remainder}m`
}

const workedRatio = computed(() => {
  if (!props.weeklyProgress.targetMinutes) {
    return 0
  }
  return Math.min((props.weeklyProgress.workedMinutes / props.weeklyProgress.targetMinutes) * 100, 100)
})

const remainingMinutes = computed(() =>
  Math.max(props.weeklyProgress.targetMinutes - props.weeklyProgress.workedMinutes, 0)
)
</script>

<template>
  <section class="card widget-card">
    <div class="section-header section-header--compact">
      <div>
        <p class="eyebrow">This week</p>
        <h3>Weekly progress</h3>
      </div>
      <span class="pill">{{ props.weeklyProgress.percentage }}%</span>
    </div>

    <div v-if="props.weeklyProgress.hasTarget" class="chart-stack">
      <div class="bar-group">
        <div class="bar-labels">
          <span>Worked</span>
          <strong>{{ formatMinutes(props.weeklyProgress.workedMinutes) }}</strong>
        </div>
        <div class="progress-track">
          <div class="progress-fill" :style="{ width: `${workedRatio}%` }" />
        </div>
      </div>

      <div class="bar-group">
        <div class="bar-labels">
          <span>Remaining</span>
          <strong>{{ formatMinutes(remainingMinutes) }}</strong>
        </div>
        <div class="progress-track progress-track--subtle">
          <div class="progress-fill progress-fill--secondary" :style="{ width: `${100 - workedRatio}%` }" />
        </div>
      </div>

      <div class="stats-grid stats-grid--two">
        <div>
          <span class="muted">Target</span>
          <strong>{{ formatMinutes(props.weeklyProgress.targetMinutes) }}</strong>
        </div>
        <div>
          <span class="muted">Status</span>
          <strong>{{ props.weeklyProgress.workedMinutes >= props.weeklyProgress.targetMinutes ? 'On target' : 'In progress' }}</strong>
        </div>
      </div>
    </div>

    <p v-else class="empty-state">Set weekly target hours in Settings to see progress.</p>
  </section>
</template>
