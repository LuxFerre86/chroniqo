import client from './client'

export interface AuthUser {
  email: string
  firstName: string
  lastName: string
  weeklyTargetHours: number
  workingDays?: string[]
  countryCode?: string | null
  subdivisionCode?: string | null
}

export interface LoginPayload {
  email: string
  password: string
  rememberMe: boolean
}

export interface RegisterPayload {
  email: string
  password: string
  firstName: string
  lastName: string
  weeklyTargetHours: number
  countryCode?: string | null
  subdivisionCode?: string | null
}

export interface ResetPasswordPayload {
  token: string
  newPassword: string
}

export async function login(payload: LoginPayload): Promise<AuthUser> {
  const { data } = await client.post<AuthUser>('/auth/login', payload)
  return data
}

export async function logout(): Promise<void> {
  await client.post('/auth/logout')
}

export async function fetchCurrentUser(): Promise<AuthUser> {
  const { data } = await client.get<AuthUser>('/auth/me')
  return data
}

export async function register(payload: RegisterPayload): Promise<void> {
  await client.post('/auth/register', payload)
}

export async function verifyEmail(token: string): Promise<void> {
  await client.get('/auth/verify-email', { params: { token } })
}

export async function requestPasswordReset(email: string): Promise<void> {
  await client.post('/auth/request-password-reset', { email })
}

export async function resetPassword(payload: ResetPasswordPayload): Promise<void> {
  await client.post('/auth/reset-password', payload)
}
