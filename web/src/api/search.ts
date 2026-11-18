import type { SearchQuery, SearchSort } from './types'

export const PAGE_SIZE = 20

/** Höchstzahl der Treffer bei Sortierung nach Relevanz (Spezifikation 5.5) */
export const RELEVANCE_LIMIT = 200

export const SORT_LABELS: Record<SearchSort, string> = {
  relevance: 'Relevanz',
  price_asc: 'Preis aufsteigend',
  price_desc: 'Preis absteigend',
  protein_desc: 'Eiweiß absteigend',
  kcal_asc: 'kcal aufsteigend',
}

const SORTS = Object.keys(SORT_LABELS) as SearchSort[]

/** Liest die Suche aus der Adresszeile, z. B. /?q=hafer&sort=price_asc&page=2 */
export function readSearchQuery(params: URLSearchParams): SearchQuery {
  const sort = params.get('sort')
  return {
    q: params.get('q')?.trim() || undefined,
    category: params.get('category') || undefined,
    minPriceCents: readNumber(params.get('minPriceCents')),
    maxPriceCents: readNumber(params.get('maxPriceCents')),
    minProtein100g: readNumber(params.get('minProtein100g')),
    sort: SORTS.includes(sort as SearchSort) ? (sort as SearchSort) : 'relevance',
    page: readPage(params.get('page')),
    pageSize: PAGE_SIZE,
  }
}

/** Baut den Query-String für die API, leere Werte fallen weg. */
export function toQueryString(query: SearchQuery): string {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== '') {
      params.set(key, String(value))
    }
  }
  return params.toString()
}

/** Ob außer dem Suchbegriff ein Filter gesetzt ist */
export function hasFilters(query: SearchQuery): boolean {
  return (
    query.category !== undefined ||
    query.minPriceCents !== undefined ||
    query.maxPriceCents !== undefined ||
    query.minProtein100g !== undefined
  )
}

function readNumber(text: string | null): number | undefined {
  if (text === null || text === '') {
    return undefined
  }
  const value = Number(text)
  return Number.isFinite(value) ? value : undefined
}

function readPage(text: string | null): number {
  const page = Number(text)
  return Number.isInteger(page) && page >= 1 ? page : 1
}
