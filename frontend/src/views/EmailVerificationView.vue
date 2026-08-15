<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { verifyEmail } from '../api/auth'
import { getErrorMessage } from '../api/client'

const route = useRoute()
const loading = ref(true)
const success = ref(false)
const message = ref('')

onMounted(async () => {
  const token = typeof route.query.token === 'string' ? route.query.token : ''
  if (!token) {
    loading.value = false
    message.value = 'The verification link is missing a token.'
    return
  }

  try {
    await verifyEmail(token)
    success.value = true
    message.value = 'Your email has been verified. You can sign in now.'
  } catch (error) {
    message.value = getErrorMessage(error, 'Email verification failed. The link may have expired.')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="auth-page">
    <div class="card auth-card">
      <div class="auth-card__header">
        <span class="brand brand--large">chroniqo</span>
        <h1>Email verification</h1>
      </div>
      <p v-if="loading" class="muted">Verifying your email…</p>
      <p v-else :class="success ? 'success-message' : 'error-message'">{{ message }}</p>
      <div class="auth-links auth-links--single">
        <RouterLink to="/login">Go to login</RouterLink>
      </div>
    </div>
  </div>
</template>
