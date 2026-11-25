import { ApiError, parseProblem } from './errorHandling'
import type { paths } from './schema'

// Pfade, Parameter und Antworten kommen aus den generierten Typen (openapi.json). Ein Tippfehler im Pfad,
// ein unbekannter Parameter oder ein falscher Wert wie sort: 'PRICE_ASC' fallen damit schon beim Bauen auf.

/** Alle Pfade, die ein GET mit 200-Antwort haben, z. B. '/api/v1/catalog/info' */
export type GetPath = {
  [P in keyof paths]: paths[P] extends { get: { responses: { 200: unknown } } } ? P : never
}[keyof paths]

/** Was der Pfad bei 200 zurückgibt */
export type GetResponse<P extends GetPath> =
  paths[P]['get']['responses'][200]['content']['application/json']

/** Welche Query-Parameter der Pfad annimmt */
export type GetQuery<P extends GetPath> = paths[P]['get']['parameters']['query']

// Holt Daten von der API. Bei einem Fehler wird eine ApiError geworfen.
export async function apiGet<P extends GetPath>(
  path: P,
  query?: GetQuery<P>,
): Promise<GetResponse<P>> {
  // Volle Adresse, damit derselbe Code im Browser und in den Tests (Node) funktioniert.
  const url = new URL(path, window.location.origin)
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, String(value))
    }
  }

  const response = await fetch(url)

  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new ApiError(response.status, parseProblem(body))
  }

  return (await response.json()) as GetResponse<P>
}
