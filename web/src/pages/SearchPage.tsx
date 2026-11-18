import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { apiGet } from '../api/client'
import {
  hasFilters,
  RELEVANCE_LIMIT,
  readSearchQuery,
  SORT_LABELS,
  toQueryString,
} from '../api/search'
import type { CatalogInfo, PagedProductSummaries, SearchSort } from '../api/types'
import { ErrorMessage } from '../components/ErrorMessage'
import { Pagination } from '../components/Pagination'
import { ProductCard } from '../components/ProductCard'
import {
  centsToEuroInput,
  formatCount,
  formatDate,
  parseDecimal,
  parseEuroToCents,
} from '../format'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import { addToList } from '../list/addToList'

/** Wartezeit nach dem letzten Tastendruck, bevor gesucht wird */
export const DEBOUNCE_MS = 300

type ParamChanges = Record<string, string | undefined>

/** Deutsche Namen der Felder, die die API bei einem 400 nennt */
const FIELD_LABELS: Record<string, string> = {
  q: 'Suchbegriff',
  category: 'Kategorie',
  minPriceCents: 'Preis von',
  maxPriceCents: 'Preis bis',
  minProtein100g: 'Eiweiß ab',
  sort: 'Sortierung',
  page: 'Seite',
}

export function SearchPage() {
  // Die Adresszeile ist die Quelle der Wahrheit: /?q=hafer&sort=price_asc&page=2
  const [params, setParams] = useSearchParams()
  const query = readSearchQuery(params)

  // Jede Änderung an Suche oder Filter springt auf Seite 1 zurück.
  function update(changes: ParamChanges) {
    setParams(
      (previous) => {
        const next = new URLSearchParams(previous)
        for (const [key, value] of Object.entries(changes)) {
          if (value === undefined || value === '') next.delete(key)
          else next.set(key, value)
        }
        next.delete('page')
        return next
      },
      { replace: true },
    )
  }

  function goToPage(page: number) {
    setParams((previous) => {
      const next = new URLSearchParams(previous)
      next.set('page', String(page))
      return next
    })
    window.scrollTo({ top: 0 })
  }

  const [text, setText] = useUrlField(
    params,
    'q',
    (t) => t.trim(),
    (u) => u,
    update,
  )
  const [minPrice, setMinPrice] = useUrlField(
    params,
    'minPriceCents',
    euroToUrl,
    centsToText,
    update,
  )
  const [maxPrice, setMaxPrice] = useUrlField(
    params,
    'maxPriceCents',
    euroToUrl,
    centsToText,
    update,
  )
  const [minProtein, setMinProtein] = useUrlField(
    params,
    'minProtein100g',
    decimalToUrl,
    (u) => u.replace('.', ','),
    update,
  )

  const catalog = useQuery({
    queryKey: ['catalog-info'],
    queryFn: () => apiGet<CatalogInfo>('/catalog/info'),
  })
  const categories = useQuery({
    queryKey: ['categories'],
    queryFn: () => apiGet<string[]>('/categories'),
  })
  const search = useQuery({
    queryKey: ['search', query],
    queryFn: () => apiGet<PagedProductSummaries>(`/products/search?${toQueryString(query)}`),
    // Während eine neue Suche lädt, bleiben die alten Treffer stehen (web/DESIGN.md, Zustände).
    placeholderData: keepPreviousData,
  })

  const catalogEmpty = catalog.data?.lastScrapedAt === null

  function resetFilters() {
    setMinPrice('')
    setMaxPrice('')
    setMinProtein('')
    update({
      category: undefined,
      minPriceCents: undefined,
      maxPriceCents: undefined,
      minProtein100g: undefined,
    })
  }

  return (
    <>
      <h1 className="text-2xl font-semibold">Produkte suchen</h1>

      <div className="mt-6 flex flex-col gap-4 sm:flex-row sm:items-end">
        <label className="flex flex-1 flex-col gap-1 text-sm">
          Produkt, Marke oder EAN
          <input
            type="search"
            value={text}
            onChange={(event) => setText(event.target.value)}
            disabled={catalogEmpty}
            className={fieldClass}
          />
        </label>
        <label className="flex flex-col gap-1 text-sm">
          Sortierung
          <select
            value={query.sort}
            onChange={(event) =>
              update({ sort: event.target.value === 'relevance' ? undefined : event.target.value })
            }
            disabled={catalogEmpty}
            className={fieldClass}
          >
            {(Object.keys(SORT_LABELS) as SearchSort[]).map((sort) => (
              <option key={sort} value={sort}>
                {SORT_LABELS[sort]}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-4 sm:flex sm:items-end">
        <label className="col-span-2 flex flex-col gap-1 text-sm sm:col-span-1">
          Kategorie
          <select
            value={query.category ?? ''}
            onChange={(event) => update({ category: event.target.value })}
            disabled={catalogEmpty}
            className={fieldClass}
          >
            <option value="">Alle Kategorien</option>
            {categories.data?.map((category) => (
              <option key={category} value={category}>
                {category}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-1 text-sm">
          Preis von (€)
          <input
            inputMode="decimal"
            value={minPrice}
            onChange={(event) => setMinPrice(event.target.value)}
            disabled={catalogEmpty}
            className={`${fieldClass} sm:w-28`}
          />
        </label>
        <label className="flex flex-col gap-1 text-sm">
          Preis bis (€)
          <input
            inputMode="decimal"
            value={maxPrice}
            onChange={(event) => setMaxPrice(event.target.value)}
            disabled={catalogEmpty}
            className={`${fieldClass} sm:w-28`}
          />
        </label>
        <label className="col-span-2 flex flex-col gap-1 text-sm sm:col-span-1">
          Eiweiß ab (g pro 100 g)
          <input
            inputMode="decimal"
            value={minProtein}
            onChange={(event) => setMinProtein(event.target.value)}
            disabled={catalogEmpty}
            className={`${fieldClass} sm:w-28`}
          />
        </label>
      </div>

      <CatalogLine info={catalog.data} />

      <section className="mt-8" aria-live="polite">
        {search.error && <ErrorMessage error={search.error} fieldLabels={FIELD_LABELS} />}

        {!search.data && search.isPending && <p className="text-ink-muted">Lädt …</p>}

        {search.data && (
          <Results
            data={search.data}
            searchText={query.q}
            isRelevance={query.sort === 'relevance' && Boolean(query.q)}
            filtersSet={hasFilters(query)}
            onResetFilters={resetFilters}
            onPage={goToPage}
          />
        )}
      </section>
    </>
  )
}

type ResultsProps = {
  data: PagedProductSummaries
  searchText: string | undefined
  isRelevance: boolean
  filtersSet: boolean
  onResetFilters: () => void
  onPage: (page: number) => void
}

function Results({
  data,
  searchText,
  isRelevance,
  filtersSet,
  onResetFilters,
  onPage,
}: ResultsProps) {
  if (data.totalCount === 0) {
    return (
      <div className="text-ink-muted">
        <p>{searchText ? `Keine Treffer für „${searchText}“.` : 'Keine Treffer.'}</p>
        {filtersSet && (
          <button type="button" onClick={onResetFilters} className="mt-2 text-accent underline">
            Filter zurücksetzen
          </button>
        )}
      </div>
    )
  }

  if (data.page > data.totalPages) {
    return (
      <div className="text-ink-muted">
        <p>Diese Seite gibt es nicht.</p>
        <button type="button" onClick={() => onPage(1)} className="mt-2 text-accent underline">
          Zu Seite 1
        </button>
      </div>
    )
  }

  return (
    <>
      <p className="text-sm text-ink-muted tabular-nums">{formatCount(data.totalCount)} Treffer</p>
      {isRelevance && data.totalCount >= RELEVANCE_LIMIT && (
        <p className="mt-1 text-sm text-ink-muted">
          Mindestens {RELEVANCE_LIMIT} Treffer, grenze die Suche ein.
        </p>
      )}
      <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
        {data.items.map((product) => (
          <ProductCard key={product.id} product={product} onAdd={addToList} />
        ))}
      </div>
      <Pagination page={data.page} totalPages={data.totalPages} onChange={onPage} />
    </>
  )
}

function CatalogLine({ info }: { info: CatalogInfo | undefined }) {
  if (!info) {
    return null
  }
  if (info.lastScrapedAt === null) {
    return <p className="mt-4 text-sm text-ink-muted">Der Katalog ist noch leer.</p>
  }
  return (
    <p className="mt-4 text-sm text-ink-muted">
      {formatCount(info.productCount)} Produkte im Katalog · Preise vom{' '}
      {formatDate(info.lastScrapedAt)}
    </p>
  )
}

/**
 * Ein Eingabefeld, dessen Wert verzögert in die Adresszeile geschrieben wird.
 * toUrl macht aus dem Text den Wert für die URL, toText aus der URL wieder einen Text.
 */
function useUrlField(
  params: URLSearchParams,
  key: string,
  toUrl: (text: string) => string,
  toText: (urlValue: string) => string,
  update: (changes: ParamChanges) => void,
) {
  const urlValue = params.get(key) ?? ''
  const [text, setText] = useState(() => toText(urlValue))
  const debounced = useDebouncedValue(text, DEBOUNCE_MS)

  // Getippt: nach der Wartezeit in die URL schreiben.
  useEffect(() => {
    if (toUrl(debounced) !== urlValue) update({ [key]: toUrl(debounced) })
    // eslint-disable-next-line react-hooks/exhaustive-deps -- nur auf neue Eingaben reagieren
  }, [debounced])

  // Von außen geändert (z. B. „Filter zurücksetzen“ oder Zurück im Browser): Feld anpassen.
  useEffect(() => {
    if (toUrl(debounced) !== urlValue) setText(toText(urlValue))
    // eslint-disable-next-line react-hooks/exhaustive-deps -- nur auf Änderungen der URL reagieren
  }, [urlValue])

  return [text, setText] as const
}

function euroToUrl(text: string): string {
  const cents = parseEuroToCents(text)
  return cents === undefined ? '' : String(cents)
}

function centsToText(urlValue: string): string {
  return urlValue === '' ? '' : centsToEuroInput(Number(urlValue))
}

function decimalToUrl(text: string): string {
  const value = parseDecimal(text)
  return value === undefined ? '' : String(value)
}

const fieldClass =
  'h-10 rounded-md border border-line-strong bg-white px-3 text-base focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent disabled:cursor-not-allowed disabled:opacity-50'
