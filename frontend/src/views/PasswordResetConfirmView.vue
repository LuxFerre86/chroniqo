<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { resetPassword } from '../api/auth'
import { getErrorMessage } from '../api/client'

const route = useRoute()
const router = useRouter()
const token = computed(() => (typeof route.query.token === 'string' ? route.query.token : ''))
const form = reactive({ newPassword: '', confirmPassword: '' })
const loading = ref(false)
const error = ref('')
const success = ref('')

async function handleSubmit() {
  error.value = ''
  success.value = ''

  if (!token.value) {
    error.value = 'The reset token is missing.'
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    error.value = 'Passwords do not match.'
    return
  }

  loading.value = true
  try {
    await resetPassword({ token: token.value, newPassword: form.newPassword })
    success.value = 'Password updated successfully. Redirecting to login…'
    window.setTimeout(() => {
      void router.push('/login')
    }, 1200)
  } catch (submitError) {
    error.value = getErrorMessage(submitError, 'Could not reset your password.')
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
        <h1>Choose a new password</h1>
      </div>

      <label class="field">
        <span>New password</span>
        <input v-model="form.newPassword" class="input" type="password" required />
      </label>
      <label class="field">
        <span>Confirm new password</span>
        <input v-model="form.confirmPassword" class="input" type="password" required />
      </label>

      <p v-if="error" class="error-message">{{ error }}</p>
      <p v-if="success" class="success-message">{{ success }}</p>

      <button class="btn btn-primary btn-block" type="submit" :disabled="loading">
        {{ loading ? 'Updating…' : 'Update password' }}
      </button>

      <div class="auth-links auth-links--single">
        <RouterLink to="/login">Back to login</RouterLink>
      </div>
    </form>
  </div>
</template>
