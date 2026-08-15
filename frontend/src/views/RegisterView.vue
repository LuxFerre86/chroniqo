<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { register } from '../api/auth'
import { getErrorMessage } from '../api/client'
import { fetchCountries, type CountryOption } from '../api/user'

const form = reactive({
  email: '',
  password: '',
  confirmPassword: '',
  firstName: '',
  lastName: '',
  weeklyTargetHours: 40,
  countryCode: '',
  subdivisionCode: ''
})
const countries = ref<CountryOption[]>([])
const loading = ref(false)
const loadError = ref('')
const submitError = ref('')
const successMessage = ref('')

const selectedCountry = computed(() => countries.value.find((country) => country.code === form.countryCode) ?? null)
const subdivisions = computed(() => selectedCountry.value?.subdivisions ?? [])

watch(selectedCountry, (country) => {
  if (!country?.subdivisions.some((item) => item.code === form.subdivisionCode)) {
    form.subdivisionCode = ''
  }
})

async function loadCountries() {
  try {
    countries.value = await fetchCountries()
  } catch (error) {
    loadError.value = getErrorMessage(error, 'Could not load countries.')
  }
}

async function handleSubmit() {
  submitError.value = ''
  successMessage.value = ''

  if (form.password !== form.confirmPassword) {
    submitError.value = 'Passwords do not match.'
    return
  }
  if (form.weeklyTargetHours < 0 || form.weeklyTargetHours > 80) {
    submitError.value = 'Weekly target hours must be between 0 and 80.'
    return
  }

  loading.value = true
  try {
    await register({
      email: form.email,
      password: form.password,
      firstName: form.firstName,
      lastName: form.lastName,
      weeklyTargetHours: Number(form.weeklyTargetHours),
      countryCode: form.countryCode || null,
      subdivisionCode: form.subdivisionCode || null
    })
    successMessage.value = 'Registration successful. Please check your email to verify your account.'
    form.password = ''
    form.confirmPassword = ''
  } catch (error) {
    submitError.value = getErrorMessage(error, 'Registration failed. Please review your details and try again.')
  } finally {
    loading.value = false
  }
}

onMounted(loadCountries)
</script>

<template>
  <div class="auth-page auth-page--wide">
    <form class="card auth-card auth-card--wide" @submit.prevent="handleSubmit">
      <div class="auth-card__header">
        <span class="brand brand--large">chroniqo</span>
        <h1>Create your account</h1>
        <p class="muted">Set up your profile and holiday region in one step.</p>
      </div>

      <div class="form-grid">
        <label class="field field--full">
          <span>Email</span>
          <input v-model="form.email" class="input" type="email" required />
        </label>
        <label class="field">
          <span>Password</span>
          <input v-model="form.password" class="input" type="password" required />
        </label>
        <label class="field">
          <span>Confirm password</span>
          <input v-model="form.confirmPassword" class="input" type="password" required />
        </label>
        <label class="field">
          <span>First name</span>
          <input v-model="form.firstName" class="input" type="text" required />
        </label>
        <label class="field">
          <span>Last name</span>
          <input v-model="form.lastName" class="input" type="text" required />
        </label>
        <label class="field">
          <span>Weekly target hours</span>
          <input v-model.number="form.weeklyTargetHours" class="input" type="number" min="0" max="80" required />
        </label>
        <label class="field">
          <span>Country</span>
          <select v-model="form.countryCode" class="input">
            <option value="">No holiday calendar</option>
            <option v-for="country in countries" :key="country.code" :value="country.code">{{ country.name }}</option>
          </select>
        </label>
        <label class="field">
          <span>Subdivision</span>
          <select v-model="form.subdivisionCode" class="input" :disabled="!subdivisions.length">
            <option value="">Nationwide only</option>
            <option v-for="subdivision in subdivisions" :key="subdivision.code" :value="subdivision.code">
              {{ subdivision.name }}
            </option>
          </select>
        </label>
      </div>

      <p v-if="loadError" class="error-message">{{ loadError }}</p>
      <p v-if="submitError" class="error-message">{{ submitError }}</p>
      <p v-if="successMessage" class="success-message">{{ successMessage }}</p>

      <button class="btn btn-primary btn-block" type="submit" :disabled="loading">
        {{ loading ? 'Creating account…' : 'Register' }}
      </button>

      <div class="auth-links auth-links--single">
        <RouterLink to="/login">Back to login</RouterLink>
      </div>
    </form>
  </div>
</template>
