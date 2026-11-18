import { ApiError } from '../api/errorHandling'

type Props = {
  error: unknown
  /** Deutsche Namen für Felder aus `errors`, z. B. { minPriceCents: 'Preis von' } */
  fieldLabels?: Record<string, string>
}

// Zeigt API-Fehler einheitlich an: title und detail aus dem ProblemDetail, bei 400 auch je Feld,
// was nicht stimmt.
export function ErrorMessage({ error, fieldLabels = {} }: Props) {
  let title = 'Die API ist nicht erreichbar.'
  let detail: string | undefined
  let fieldErrors: [string, string][] = []

  if (error instanceof ApiError) {
    title = error.problem?.title ?? `Unerwartete Antwort der API (HTTP ${error.status})`
    detail = error.problem?.detail
    fieldErrors = Object.entries(error.problem?.errors ?? {})
  }

  return (
    <div role="alert" className="border-l-4 border-danger bg-danger-soft px-4 py-3 text-sm">
      <p className="font-medium text-danger">{title}</p>
      {detail && <p className="mt-1 text-ink-muted">{detail}</p>}
      {fieldErrors.length > 0 && (
        <ul className="mt-1 list-disc pl-5 text-ink-muted">
          {fieldErrors.map(([field, message]) => (
            <li key={field}>
              {fieldLabels[field] ?? field}: {message}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
