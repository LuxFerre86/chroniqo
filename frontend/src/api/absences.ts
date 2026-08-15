import client from './client'

export type AbsenceType = 'VACATION' | 'SICK' | 'HOLIDAY' | 'PUBLIC_HOLIDAY' | string

export interface Absence {
  id?: string
  date: string
  type: AbsenceType
}

export interface AbsencePayload {
  startDate: string
  endDate: string
  absenceType: 'VACATION' | 'SICK'
}

export async function fetchAbsences(year: number, month: number): Promise<Absence[]> {
  const { data } = await client.get<Absence[]>('/absences', { params: { year, month } })
  return data
}

export async function saveAbsence(payload: AbsencePayload): Promise<void> {
  await client.post('/absences', payload)
}

export async function deleteAbsence(date: string): Promise<void> {
  await client.delete('/absences', { params: { date } })
}
