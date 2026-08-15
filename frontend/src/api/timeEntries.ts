import client from './client'

export interface TimeEntry {
  id?: string
  date: string
  startTime: string
  endTime: string | null
  breakMinutes: number
  notes: string
}

export type TimeEntryPayload = Omit<TimeEntry, 'id'>

export async function fetchTimeEntries(year: number, month: number): Promise<TimeEntry[]> {
  const { data } = await client.get<TimeEntry[]>('/time-entries', { params: { year, month } })
  return data
}

export async function createTimeEntry(payload: TimeEntryPayload): Promise<TimeEntry> {
  const { data } = await client.post<TimeEntry>('/time-entries', payload)
  return data
}

export async function updateTimeEntry(id: string, payload: TimeEntryPayload): Promise<TimeEntry> {
  const { data } = await client.put<TimeEntry>(`/time-entries/${id}`, payload)
  return data
}

export async function removeTimeEntry(id: string): Promise<void> {
  await client.delete(`/time-entries/${id}`)
}
