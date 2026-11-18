import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { catalogInfo, page, product } from '../test/handlers'
import { renderRoute } from '../test/render'
import { server } from '../test/server'

// Merkt sich die Query-Parameter jeder Suchanfrage, damit die Tests sie prüfen können.
let searches: URLSearchParams[] = []

function answerSearch(response: () => Response = () => HttpResponse.json(page([product()]))) {
  server.use(
    http.get('*/api/v1/products/search', ({ request }) => {
      searches.push(new URL(request.url).searchParams)
      return response()
    }),
  )
}

const lastSearch = () => searches[searches.length - 1]

beforeEach(() => {
  searches = []
  answerSearch()
})

describe('SearchPage', () => {
  it('sucht erst 300 ms nach dem letzten Tastendruck', async () => {
    renderRoute('/')
    await screen.findByText('Haferflocken zart')

    await userEvent.type(screen.getByLabelText('Produkt, Marke oder EAN'), 'milch')

    // Direkt nach dem Tippen ist noch nichts mit dem Suchbegriff angefragt.
    expect(searches.some((s) => s.has('q'))).toBe(false)
    await waitFor(() => expect(lastSearch().get('q')).toBe('milch'))
    // Kein Zwischenstand wie „m“ oder „mil“ wurde angefragt.
    expect(searches.filter((s) => s.has('q')).map((s) => s.get('q'))).toEqual(['milch'])
  })

  it('schickt eine EAN unverändert als Suchbegriff', async () => {
    renderRoute('/')
    await userEvent.type(screen.getByLabelText('Produkt, Marke oder EAN'), '4008400401211')
    await waitFor(() => expect(lastSearch().get('q')).toBe('4008400401211'))
  })

  it('schickt Filter und Sortierung als Query-Parameter, Preise in Cent', async () => {
    renderRoute('/')
    await screen.findByRole('option', { name: 'Milchprodukte' })

    await userEvent.selectOptions(screen.getByLabelText('Kategorie'), 'Milchprodukte')
    await userEvent.selectOptions(screen.getByLabelText('Sortierung'), 'Preis aufsteigend')
    await userEvent.type(screen.getByLabelText('Preis von (€)'), '1,50')
    await userEvent.type(screen.getByLabelText('Preis bis (€)'), '4')
    await userEvent.type(screen.getByLabelText('Eiweiß ab (g pro 100 g)'), '10')

    await waitFor(() => {
      const s = lastSearch()
      expect(s.get('category')).toBe('Milchprodukte')
      expect(s.get('sort')).toBe('price_asc')
      expect(s.get('minPriceCents')).toBe('150')
      expect(s.get('maxPriceCents')).toBe('400')
      expect(s.get('minProtein100g')).toBe('10')
      expect(s.get('page')).toBe('1')
    })
  })

  it.each([
    ['relevance', 'Relevanz'],
    ['price_asc', 'Preis aufsteigend'],
    ['price_desc', 'Preis absteigend'],
    ['protein_desc', 'Eiweiß absteigend'],
    ['kcal_asc', 'kcal aufsteigend'],
  ])('schickt die Sortierung %s', async (value, label) => {
    renderRoute('/?sort=price_desc')
    await screen.findByText('Haferflocken zart')
    await userEvent.selectOptions(screen.getByLabelText('Sortierung'), label)
    await waitFor(() => expect(lastSearch().get('sort')).toBe(value))
  })

  it('blättert und springt bei einem neuen Filter auf Seite 1 zurück', async () => {
    answerSearch(() => HttpResponse.json(page([product()], { totalCount: 50, totalPages: 3 })))
    renderRoute('/')

    expect(await screen.findByText('Seite 1 von 3')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '‹ Zurück' })).toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: 'Weiter ›' }))
    await waitFor(() => expect(lastSearch().get('page')).toBe('2'))

    await userEvent.selectOptions(screen.getByLabelText('Sortierung'), 'Preis absteigend')
    await waitFor(() => {
      expect(lastSearch().get('sort')).toBe('price_desc')
      expect(lastSearch().get('page')).toBe('1')
    })
  })

  it('zeigt einen 400 der API mit title, detail und dem Grund je Feld', async () => {
    // So antwortet das echte Backend auf minPriceCents > maxPriceCents.
    answerSearch(() =>
      HttpResponse.json(
        {
          title: 'Ungültige Anfrage',
          status: 400,
          detail: 'Die Anfrage enthält ungültige Werte.',
          instance: '/api/v1/products/search',
          errors: { minPriceCents: 'darf nicht größer als maxPriceCents sein' },
        },
        { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
      ),
    )
    renderRoute('/?minPriceCents=500&maxPriceCents=100')

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Ungültige Anfrage')
    expect(alert).toHaveTextContent('Die Anfrage enthält ungültige Werte.')
    expect(alert).toHaveTextContent('Preis von: darf nicht größer als maxPriceCents sein')
  })

  it('zeigt bei leerem Ergebnis einen Hinweis und setzt Filter zurück', async () => {
    answerSearch(() => HttpResponse.json(page([])))
    renderRoute('/?q=milsch&category=Milchprodukte')

    expect(await screen.findByText('Keine Treffer für „milsch“.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Filter zurücksetzen' }))
    await waitFor(() => {
      expect(lastSearch().has('category')).toBe(false)
      expect(lastSearch().get('q')).toBe('milsch')
    })
  })

  it('zeigt Katalogumfang und Preisstand unter dem Suchfeld', async () => {
    renderRoute('/')
    expect(
      await screen.findByText('312 Produkte im Katalog · Preise vom 17.11.2026'),
    ).toBeInTheDocument()
  })

  it('sagt bei leerem Katalog Bescheid und sperrt die Suche', async () => {
    server.use(
      http.get('*/api/v1/catalog/info', () =>
        HttpResponse.json({ ...catalogInfo, productCount: 0, lastScrapedAt: null }),
      ),
    )
    renderRoute('/')
    expect(await screen.findByText('Der Katalog ist noch leer.')).toBeInTheDocument()
    expect(screen.getByLabelText('Produkt, Marke oder EAN')).toBeDisabled()
  })

  it('zeigt die Treffer als Karten und behält den Suchbegriff in der Adresse', async () => {
    const router = renderRoute('/?q=hafer')
    expect(await screen.findByText('1 Treffer')).toBeInTheDocument()
    expect(screen.getByLabelText('Produkt, Marke oder EAN')).toHaveValue('hafer')
    expect(router.state.location.search).toBe('?q=hafer')
  })
})
