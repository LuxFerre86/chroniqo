<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const monthLink = computed(() => {
  if (route.name === 'month') {
    return route.fullPath
  }
  const now = new Date()
  return `/month/${now.getFullYear()}/${now.getMonth() + 1}`
})

async function handleLogout() {
  await authStore.logout()
  await router.push('/login')
}
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="topbar__inner">
        <div class="brand">chroniqo</div>
        <nav class="nav-links">
          <RouterLink class="nav-link" to="/">Dashboard</RouterLink>
          <RouterLink class="nav-link" :to="monthLink">Month</RouterLink>
          <RouterLink class="nav-link" to="/settings">Settings</RouterLink>
        </nav>
        <div class="topbar__actions">
          <span class="topbar__user">{{ authStore.fullName }}</span>
          <button class="btn btn-secondary" type="button" @click="handleLogout">Logout</button>
        </div>
      </div>
    </header>
    <main class="page-shell">
      <slot />
    </main>
  </div>
</template>
