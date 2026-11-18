import { ApiError } from './errorHandling'

// Bei 4xx ändert eine Wiederholung nichts. Nur Netzwerkfehler und 5xx erneut versuchen,
// insgesamt höchstens dreimal.
export function shouldRetry(failureCount: number, error: unknown): boolean {
  if (error instanceof ApiError && error.status < 500) {
    return false
  }
  return failureCount < 3
}
