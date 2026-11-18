import { QueryClient, QueryClientProvider, useQuery } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { ErrorMessage } from '../components/ErrorMessage'
import { server } from '../test/server'
import { apiGet } from './client'
import type { CatalogInfo } from './types'

// Kleine Testkomponente: lädt catalog/info und zeigt die Zahl oder den Fehler.
function CatalogCount() {
  const query = useQuery({
    queryKey: ['catalog-info'],
    queryFn: () => apiGet<CatalogInfo>('/catalog/info'),
  })
  if (query.error) return <ErrorMessage error={query.error} />
  if (!query.data) return <p>Lädt …</p>
  return <p>{query.data.productCount} Produkte</p>
}

function renderWithQuery() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <CatalogCount />
    </QueryClientProvider>,
  )
}

describe('apiGet', () => {
  it('liefert die Daten der API', async () => {
    server.use(
      http.get('*/api/v1/catalog/info', () =>
        HttpResponse.json({ productCount: 312, lastScrapedAt: '2026-11-17T10:00:00Z' }),
      ),
    )
    renderWithQuery()
    expect(await screen.findByText('312 Produkte')).toBeInTheDocument()
  })

  it('zeigt title und detail aus einem ProblemDetail der API', async () => {
    server.use(
      http.get('*/api/v1/catalog/info', () =>
        HttpResponse.json(
          { title: 'Interner Fehler', status: 500, detail: 'Die Datenbank ist nicht erreichbar.' },
          { status: 500, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )
    renderWithQuery()
    expect(await screen.findByRole('alert')).toHaveTextContent('Interner Fehler')
    expect(screen.getByRole('alert')).toHaveTextContent('Die Datenbank ist nicht erreichbar.')
  })

  it('nennt den Statuscode, wenn die Antwort kein ProblemDetail ist', async () => {
    server.use(
      http.get(
        '*/api/v1/catalog/info',
        () => new HttpResponse('<html>Bad Gateway</html>', { status: 502 }),
      ),
    )
    renderWithQuery()
    expect(await screen.findByRole('alert')).toHaveTextContent('HTTP 502')
  })
})
