<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const form = reactive({
  email: '',
  password: '',
  rememberMe: true
})
const error = ref('')

async function handleSubmit() {
  error.value = ''
  try {
    await authStore.login(form)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.push(redirect)
  } catch {
    error.value = authStore.error ?? 'Login failed. Please check your credentials.'
  }
}
</script>

<template>
  <div class="auth-page">
    <form class="card auth-card" @submit.prevent="handleSubmit">
      <div class="auth-card__header">
        <span class="brand brand--large">chroniqo</span>
        <h1>Welcome back</h1>
        <p class="muted">Sign in to continue tracking your time.</p>
      </div>

      <label class="field">
        <span>Email</span>
        <input v-model="form.email" class="input" type="email" autocomplete="email" required />
      </label>

      <label class="field">
        <span>Password</span>
        <input v-model="form.password" class="input" type="password" autocomplete="current-password" required />
      </label>

      <label class="checkbox-field">
        <input v-model="form.rememberMe" type="checkbox" />
        <span>Remember me</span>
      </label>

      <p v-if="error" class="error-message">{{ error }}</p>

      <button class="btn btn-primary btn-block" type="submit" :disabled="authStore.loading">
        {{ authStore.loading ? 'Signing in…' : 'Sign in' }}
      </button>

      <div class="auth-links">
        <RouterLink to="/register">Create account</RouterLink>
        <RouterLink to="/request-password-reset">Forgot password?</RouterLink>
      </div>
    </form>
  </div>
</template>
