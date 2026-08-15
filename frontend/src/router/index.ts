import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import EmailVerificationView from '../views/EmailVerificationView.vue'
import LoginView from '../views/LoginView.vue'
import MonthView from '../views/MonthView.vue'
import PasswordResetConfirmView from '../views/PasswordResetConfirmView.vue'
import PasswordResetRequestView from '../views/PasswordResetRequestView.vue'
import RegisterView from '../views/RegisterView.vue'
import SettingsView from '../views/SettingsView.vue'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    { path: '/register', name: 'register', component: RegisterView },
    { path: '/verify-email', name: 'verify-email', component: EmailVerificationView },
    { path: '/request-password-reset', name: 'request-password-reset', component: PasswordResetRequestView },
    { path: '/reset-password', name: 'reset-password', component: PasswordResetConfirmView },
    { path: '/', name: 'dashboard', component: DashboardView, meta: { requiresAuth: true } },
    { path: '/month/:year/:month', name: 'month', component: MonthView, meta: { requiresAuth: true } },
    { path: '/settings', name: 'settings', component: SettingsView, meta: { requiresAuth: true } }
  ]
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (!to.meta.requiresAuth) {
    if (to.path === '/login' && authStore.currentUser) {
      return '/'
    }
    return true
  }

  const user = await authStore.fetchMe()
  if (!user) {
    return {
      path: '/login',
      query: to.fullPath && to.fullPath !== '/' ? { redirect: to.fullPath } : undefined
    }
  }

  return true
})

export default router
