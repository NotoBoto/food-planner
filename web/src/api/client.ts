import { ApiError, parseProblem } from './errorHandling'

// Holt Daten von der API. Bei einem Fehler wird eine ApiError geworfen.
export async function apiGet<T>(path: string): Promise<T> {
  // Volle Adresse, damit derselbe Code im Browser und in den Tests (Node) funktioniert.
  const response = await fetch(new URL(`/api/v1${path}`, window.location.origin))

  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new ApiError(response.status, parseProblem(body))
  }

  return (await response.json()) as T
}
