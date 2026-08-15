<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { TimeEntry } from '../api/timeEntries'

const props = defineProps<{
  open: boolean
  date: string
  entry?: TimeEntry | null
  entries?: TimeEntry[]
}>()

const emit = defineEmits<{
  (event: 'saved', entry: TimeEntry): void
  (event: 'deleted', id: string): void
  (event: 'closed'): void
}>()

const form = reactive<TimeEntry>({
  id: undefined,
  date: '',
  startTime: '',
  endTime: '',
  breakMinutes: 0,
  notes: ''
})
const error = ref('')
const selectedEntryId = ref<string | null>(null)

const availableEntries = computed(() => props.entries ?? [])

function applyEntry(entry?: TimeEntry | null) {
  const source = entry ?? {
    date: props.date,
    startTime: '',
    endTime: '',
    breakMinutes: 0,
    notes: ''
  }
  form.id = source.id
  form.date = source.date
  form.startTime = source.startTime
  form.endTime = source.endTime
  form.breakMinutes = source.breakMinutes ?? 0
  form.notes = source.notes ?? ''
  selectedEntryId.value = source.id ?? null
  error.value = ''
}

watch(
  () => [props.open, props.entry, props.date, props.entries] as const,
  () => {
    if (!props.open) {
      return
    }
    if (props.entry) {
      applyEntry(props.entry)
      return
    }
    if (availableEntries.value.length === 1) {
      applyEntry(availableEntries.value[0])
      return
    }
    applyEntry(null)
  },
  { immediate: true }
)

function formatDate(date: string): string {
  return new Intl.DateTimeFormat(undefined, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  }).format(new Date(`${date}T00:00:00`))
}

function handleSave() {
  error.value = ''
  if (!form.startTime || !form.endTime) {
    error.value = 'Start time and end time are required.'
    return
  }

  const [startHour, startMinute] = form.startTime.split(':').map(Number)
  const [endHour, endMinute] = form.endTime.split(':').map(Number)
  const start = startHour * 60 + startMinute
  const end = endHour * 60 + endMinute

  if (end <= start) {
    error.value = 'End time must be after start time.'
    return
  }

  if (form.breakMinutes < 0) {
    error.value = 'Break minutes cannot be negative.'
    return
  }

  emit('saved', {
    id: form.id,
    date: form.date,
    startTime: form.startTime,
    endTime: form.endTime || null,
    breakMinutes: Number(form.breakMinutes),
    notes: form.notes.trim()
  })
}

function selectExistingEntry(entry: TimeEntry) {
  applyEntry(entry)
}

function createNewEntry() {
  applyEntry(null)
}
</script>

<template>
  <div v-if="open" class="modal-backdrop" @click.self="emit('closed')">
    <div class="modal card">
      <div class="section-header">
        <div>
          <p class="eyebrow">Time entry</p>
          <h3>{{ formatDate(date) }}</h3>
        </div>
        <button class="icon-button" type="button" @click="emit('closed')">×</button>
      </div>

      <div v-if="availableEntries.length" class="dialog-list">
        <div class="dialog-list__header">
          <strong>Entries on this day</strong>
          <button class="btn btn-secondary" type="button" @click="createNewEntry">+ New entry</button>
        </div>
        <div class="dialog-list__items">
          <button
            v-for="item in availableEntries"
            :key="item.id ?? `${item.date}-${item.startTime}`"
            class="entry-chip"
            :class="{ 'entry-chip--active': selectedEntryId === item.id }"
            type="button"
            @click="selectExistingEntry(item)"
          >
            {{ item.startTime }} – {{ item.endTime ?? 'Running' }}
          </button>
        </div>
      </div>

      <div class="form-grid">
        <label class="field">
          <span>Date</span>
          <input class="input" :value="date" readonly />
        </label>
        <label class="field">
          <span>Start time</span>
          <input v-model="form.startTime" class="input" type="time" />
        </label>
        <label class="field">
          <span>End time</span>
          <input v-model="form.endTime" class="input" type="time" />
        </label>
        <label class="field">
          <span>Break minutes</span>
          <input v-model.number="form.breakMinutes" class="input" type="number" min="0" />
        </label>
        <label class="field field--full">
          <span>Notes</span>
          <textarea v-model="form.notes" class="input textarea" rows="4" placeholder="Optional note" />
        </label>
      </div>

      <p v-if="error" class="error-message">{{ error }}</p>

      <div class="dialog-actions">
        <button v-if="form.id" class="btn btn-danger" type="button" @click="emit('deleted', form.id)">Delete</button>
        <div class="dialog-actions__end">
          <button class="btn btn-secondary" type="button" @click="emit('closed')">Cancel</button>
          <button class="btn btn-primary" type="button" @click="handleSave">Save</button>
        </div>
      </div>
    </div>
  </div>
</template>
