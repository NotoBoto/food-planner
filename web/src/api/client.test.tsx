import { QueryClient, QueryClientProvider, useQuery } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { ErrorMessage } from '../components/ErrorMessage'
import { server } from '../test/server'
import { apiGet } from './client'

// Kleine Testkomponente: lädt catalog/info und zeigt die Zahl oder den Fehler.
function CatalogCount() {
  const query = useQuery({
    queryKey: ['catalog-info'],
    queryFn: () => apiGet('/api/v1/catalog/info'),
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

  it('hängt die Query-Parameter an und lässt leere weg', async () => {
    let requested: URLSearchParams | undefined
    server.use(
      http.get('*/api/v1/products/search', ({ request }) => {
        requested = new URL(request.url).searchParams
        return HttpResponse.json({ items: [], page: 1, pageSize: 20, totalCount: 0, totalPages: 0 })
      }),
    )
    await apiGet('/api/v1/products/search', { q: 'milch', sort: 'price_asc', category: undefined })
    expect(requested?.get('q')).toBe('milch')
    expect(requested?.get('sort')).toBe('price_asc')
    expect(requested?.has('category')).toBe(false)
  })

  // Diese Aufrufe laufen nie. tsc prüft beim Bauen, dass sie Typfehler sind.
  it('lässt nur Pfade und Parameter aus der Spezifikation zu', () => {
    const typeChecks = () => {
      // @ts-expect-error unbekannter Pfad
      void apiGet('/api/v1/gibt-es-nicht')
      // @ts-expect-error sort nur in snake_case
      void apiGet('/api/v1/products/search', { sort: 'PRICE_ASC' })
    }
    expect(typeChecks).toBeTypeOf('function')
  })
})
