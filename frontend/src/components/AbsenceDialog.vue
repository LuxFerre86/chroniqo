<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { Absence } from '../api/absences'

const props = defineProps<{
  open: boolean
  date: string
  absence?: Absence | null
}>()

const emit = defineEmits<{
  (event: 'saved', payload: { startDate: string; endDate: string; absenceType: 'VACATION' | 'SICK' }): void
  (event: 'deleted', date: string): void
  (event: 'closed'): void
}>()

const form = reactive({
  startDate: '',
  endDate: '',
  absenceType: 'VACATION' as 'VACATION' | 'SICK'
})
const error = ref('')

watch(
  () => [props.open, props.date, props.absence] as const,
  () => {
    if (!props.open) {
      return
    }
    form.startDate = props.absence?.date ?? props.date
    form.endDate = props.absence?.date ?? props.date
    form.absenceType = props.absence?.type === 'SICK' ? 'SICK' : 'VACATION'
    error.value = ''
  },
  { immediate: true }
)

function handleSave() {
  error.value = ''
  if (!form.startDate || !form.endDate) {
    error.value = 'Start date and end date are required.'
    return
  }
  if (form.endDate < form.startDate) {
    error.value = 'End date must be the same day or later than the start date.'
    return
  }
  emit('saved', { ...form })
}
</script>

<template>
  <div v-if="open" class="modal-backdrop" @click.self="emit('closed')">
    <div class="modal card modal--narrow">
      <div class="section-header">
        <div>
          <p class="eyebrow">Absence</p>
          <h3>Manage absence</h3>
        </div>
        <button class="icon-button" type="button" @click="emit('closed')">×</button>
      </div>

      <div class="form-grid">
        <label class="field">
          <span>Start date</span>
          <input v-model="form.startDate" class="input" type="date" />
        </label>
        <label class="field">
          <span>End date</span>
          <input v-model="form.endDate" class="input" type="date" />
        </label>
        <label class="field field--full">
          <span>Absence type</span>
          <select v-model="form.absenceType" class="input">
            <option value="VACATION">Vacation</option>
            <option value="SICK">Sick</option>
          </select>
        </label>
      </div>

      <p v-if="error" class="error-message">{{ error }}</p>

      <div class="dialog-actions">
        <button v-if="absence" class="btn btn-danger" type="button" @click="emit('deleted', absence.date)">Delete</button>
        <div class="dialog-actions__end">
          <button class="btn btn-secondary" type="button" @click="emit('closed')">Cancel</button>
          <button class="btn btn-primary" type="button" @click="handleSave">Save</button>
        </div>
      </div>
    </div>
  </div>
</template>
