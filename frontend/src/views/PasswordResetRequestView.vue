<script setup lang="ts">
import { reactive, ref } from 'vue'
import { requestPasswordReset } from '../api/auth'
import { getErrorMessage } from '../api/client'

const form = reactive({ email: '' })
const loading = ref(false)
const error = ref('')
const success = ref('')

async function handleSubmit() {
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await requestPasswordReset(form.email)
    success.value = 'If an account exists for this email, a reset link has been sent.'
  } catch (submitError) {
    error.value = getErrorMessage(submitError, 'Could not request a password reset.')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <form class="card auth-card" @submit.prevent="handleSubmit">
      <div class="auth-card__header">
        <span class="brand brand--large">chroniqo</span>
        <h1>Reset password</h1>
        <p class="muted">Enter your email address to receive a reset link.</p>
      </div>

      <label class="field">
        <span>Email</span>
        <input v-model="form.email" class="input" type="email" required />
      </label>

      <p v-if="error" class="error-message">{{ error }}</p>
      <p v-if="success" class="success-message">{{ success }}</p>

      <button class="btn btn-primary btn-block" type="submit" :disabled="loading">
        {{ loading ? 'Sending…' : 'Send reset link' }}
      </button>

      <div class="auth-links auth-links--single">
        <RouterLink to="/login">Back to login</RouterLink>
      </div>
    </form>
  </div>
</template>
