import { defineStore } from 'pinia'
import axios from 'axios'
import router from '../router'
import { fetchCurrentUser, login as loginApi, logout as logoutApi, type AuthUser, type LoginPayload } from '../api/auth'
import { getErrorMessage } from '../api/client'

let pendingFetch: Promise<AuthUser | null> | null = null

export const useAuthStore = defineStore('auth', {
  state: () => ({
    currentUser: null as AuthUser | null,
    initialized: false,
    loading: false,
    error: null as string | null
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.currentUser),
    fullName: (state) =>
      state.currentUser ? `${state.currentUser.firstName} ${state.currentUser.lastName}`.trim() : ''
  },
  actions: {
    clearSession() {
      this.currentUser = null
      this.initialized = true
      this.error = null
    },
    async login(payload: LoginPayload) {
      this.loading = true
      this.error = null
      try {
        const user = await loginApi(payload)
        this.currentUser = user
        this.initialized = true
        return user
      } catch (error) {
        this.error = getErrorMessage(error, 'Login failed. Please check your credentials.')
        throw error
      } finally {
        this.loading = false
      }
    },
    async logout() {
      try {
        await logoutApi()
      } catch {
      } finally {
        this.clearSession()
        if (router.currentRoute.value.path !== '/login') {
          await router.push('/login')
        }
      }
    },
    async fetchMe(force = false): Promise<AuthUser | null> {
      if (!force && this.initialized) {
        return this.currentUser
      }

      if (pendingFetch && !force) {
        return pendingFetch
      }

      this.loading = true
      this.error = null
      pendingFetch = (async () => {
        try {
          const user = await fetchCurrentUser()
          this.currentUser = user
          this.initialized = true
          return user
        } catch (error) {
          if (axios.isAxiosError(error) && error.response?.status === 401) {
            this.clearSession()
            return null
          }
          this.error = getErrorMessage(error, 'Could not load your profile.')
          throw error
        } finally {
          this.loading = false
          pendingFetch = null
        }
      })()

      return pendingFetch
    }
  }
})
