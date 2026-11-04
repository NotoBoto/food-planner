import { ApiError, parseProblem } from './errorHandling'

// Holt Daten von der API. Bei einem Fehler wird eine ApiError geworfen.
export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(`/api/v1${path}`)

  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new ApiError(response.status, parseProblem(body))
  }

  return (await response.json()) as T
}