import client from './client'

export interface CountrySubdivision {
  code: string
  name: string
}

export interface CountryOption {
  code: string
  name: string
  subdivisions: CountrySubdivision[]
}

export interface UpdateProfilePayload {
  firstName: string
  lastName: string
  weeklyTargetHours: number
  workingDays: string[]
  countryCode?: string | null
  subdivisionCode?: string | null
}

export async function fetchCountries(): Promise<CountryOption[]> {
  const { data } = await client.get<CountryOption[]>('/public/countries')
  return data
}

export async function updateProfile(payload: UpdateProfilePayload): Promise<void> {
  await client.put('/user/profile', payload)
}

export async function changePassword(oldPassword: string, newPassword: string): Promise<void> {
  await client.put('/user/password', { oldPassword, newPassword })
}

export async function deleteAccount(password: string): Promise<void> {
  await client.delete('/user/account', { data: { password } })
}
