import axios from 'axios'
import type { ApiProblem } from './types'

export function apiErrorMessage(
  error: unknown,
  fallback = 'No fue posible completar la operación.',
) {
  if (!axios.isAxiosError<ApiProblem>(error)) {
    return fallback
  }

  const data = error.response?.data

  if (Array.isArray(data?.errors) && data.errors.length > 0) {
    return data.errors.join(' · ')
  }

  return data?.detail || data?.title || fallback
}
