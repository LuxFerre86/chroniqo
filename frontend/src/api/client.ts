import axios, { AxiosError } from 'axios'

const client = axios.create({
  baseURL: '/api',
  withCredentials: true
})

let redirecting = false

client.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<{ message?: string; error?: string }>) => {
    if (error.response?.status === 401) {
      const requestUrl = error.config?.url ?? ''
      const isPublicRequest =
        requestUrl.startsWith('/auth/login') ||
        requestUrl.startsWith('/auth/register') ||
        requestUrl.startsWith('/auth/request-password-reset') ||
        requestUrl.startsWith('/auth/reset-password') ||
        requestUrl.startsWith('/auth/verify-email') ||
        requestUrl.startsWith('/public/')

      if (!isPublicRequest) {
        const { default: router } = await import('../router')
        if (!redirecting && router.currentRoute.value.path !== '/login') {
          redirecting = true
          await router.push('/login')
          redirecting = false
        }
      }
    }

    return Promise.reject(error)
  }
)

export function getErrorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data
    if (typeof data === 'string' && data.trim()) {
      return data
    }

    if (data && typeof data === 'object') {
      const candidate = data.message ?? data.error
      if (typeof candidate === 'string' && candidate.trim()) {
        return candidate
      }
    }

    if (typeof error.message === 'string' && error.message.trim()) {
      return error.message
    }
  }

  if (error instanceof Error && error.message.trim()) {
    return error.message
  }

  return fallback
}

export default client
