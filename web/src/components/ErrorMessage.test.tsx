import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ApiError } from '../api/errorHandling'
import { ErrorMessage } from './ErrorMessage'

describe('ErrorMessage', () => {
  it('zeigt title und detail aus dem ProblemDetail', () => {
    const error = new ApiError(404, {
      title: 'Nicht gefunden',
      detail: 'Kein Produkt mit der ID 42.',
    })
    render(<ErrorMessage error={error} />)
    expect(screen.getByRole('alert')).toHaveTextContent('Nicht gefunden')
    expect(screen.getByRole('alert')).toHaveTextContent('Kein Produkt mit der ID 42.')
  })

  it('nennt den Statuscode, wenn kein ProblemDetail kommt', () => {
    render(<ErrorMessage error={new ApiError(502, null)} />)
    expect(screen.getByRole('alert')).toHaveTextContent('HTTP 502')
  })

  it('meldet eine fehlende Verbindung', () => {
    render(<ErrorMessage error={new TypeError('Failed to fetch')} />)
    expect(screen.getByRole('alert')).toHaveTextContent('Die API ist nicht erreichbar.')
  })
})
