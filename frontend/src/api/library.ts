import { apiClient } from './client'
import type {
  AccessRecord,
  Career,
  CheckInPayload,
  CreateVisitorPayload,
  Person,
  PersonType,
  VisitReason,
} from './types'

export async function getPersonTypes() {
  const { data } = await apiClient.get<PersonType[]>('/person-types')
  return data
}

export async function getCareers() {
  const { data } = await apiClient.get<Career[]>('/careers')
  return data
}

export async function getVisitReasons() {
  const { data } = await apiClient.get<VisitReason[]>('/visit-reasons')
  return data
}

export async function getPersonByIdentifier(identifier: string) {
  const { data } = await apiClient.get<Person>(
    `/persons/identifier/${encodeURIComponent(identifier)}`,
  )
  return data
}

export async function createVisitor(payload: CreateVisitorPayload) {
  const { data } = await apiClient.post<Person>('/persons/visitors', payload)
  return data
}

export async function checkIn(payload: CheckInPayload) {
  const { data } = await apiClient.post<AccessRecord>(
    '/access-records/check-in',
    payload,
  )
  return data
}

export async function checkOut(accessRecordId: number) {
  const { data } = await apiClient.post<AccessRecord>(
    `/access-records/${accessRecordId}/check-out`,
  )
  return data
}

export async function getOpenAccessRecords() {
  const { data } = await apiClient.get<AccessRecord[]>(
    '/access-records/open',
  )
  return data
}

export async function getRecentAccessRecords() {
  const { data } = await apiClient.get<AccessRecord[]>(
    '/access-records/recent',
  )
  return data
}
