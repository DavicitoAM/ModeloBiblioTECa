import { apiClient } from '../../api/client'
import type {
  Student,
  VisitReason,
  VisitResponse,
} from './types'

export async function getStudentByControlNumber(
  controlNumber: string
): Promise<Student> {

  const response = await apiClient.get<Student>(
    `/students/control-number/${controlNumber}`
  )

  return response.data
}

export async function getVisitReasons(): Promise<VisitReason[]> {

  const response =
    await apiClient.get<VisitReason[]>('/visit-reasons')

  return response.data
}

export async function createVisit(
  controlNumber: string,
  visitReasonId: number
): Promise<VisitResponse> {

  const response = await apiClient.post<VisitResponse>(
    '/visits',
    {
      controlNumber,
      visitReasonId,
    }
  )

  return response.data
}