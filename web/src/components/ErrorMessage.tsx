import { ApiError } from '../api/errorHandling'

type Props = {
  error: unknown
}

// Zeigt API-Fehler einheitlich an: title und detail aus dem ProblemDetail.
export function ErrorMessage({ error }: Props) {
  let title = 'Die API ist nicht erreichbar.'
  let detail: string | undefined

  if (error instanceof ApiError) {
    title = error.problem?.title ?? `Unerwartete Antwort der API (HTTP ${error.status})`
    detail = error.problem?.detail
  }

  return (
    <div role="alert" className="border-l-4 border-danger bg-danger-soft px-4 py-3 text-sm">
      <p className="font-medium text-danger">{title}</p>
      {detail && <p className="mt-1 text-ink-muted">{detail}</p>}
    </div>
  )
}