import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useEffect, useId, useRef, useState } from 'react'
import { useSearchParams } from 'react-router'
import { apiGet } from '../api/client'
import { hasFilters, readSearchQuery, SORT_LABELS, TEXT_HIT_LIMIT } from '../api/search'
import type { CatalogInfo, PagedProductSummaries, SearchSort } from '../api/types'
import { ErrorMessage } from '../components/ErrorMessage'
import { Pagination } from '../components/Pagination'
import { ProductCard } from '../components/ProductCard'
import {
  centsToEuroInput,
  formatCount,
  formatDate,
  isValidDecimalInput,
  isValidEuroInput,
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

  const text = useUrlField(
    params,
    'q',
    (t) => t.trim(),
    (u) => u,
    update,
  )
  const minPrice = useUrlField(params, 'minPriceCents', euroToUrl, centsToText, update)
  const maxPrice = useUrlField(params, 'maxPriceCents', euroToUrl, centsToText, update)
  const minProtein = useUrlField(params, 'minProtein100g', decimalToUrl, decimalToText, update)

  const catalog = useQuery({
    queryKey: ['catalog-info'],
    queryFn: () => apiGet('/api/v1/catalog/info'),
  })
  const categories = useQuery({
    queryKey: ['categories'],
    queryFn: () => apiGet('/api/v1/categories'),
  })

  const catalogEmpty = catalog.data?.lastScrapedAt === null

  const search = useQuery({
    queryKey: ['search', query],
    queryFn: () => apiGet('/api/v1/products/search', query),
    // Erst suchen, wenn feststeht, ob der Katalog leer ist. Bei leerem Katalog gibt es nichts zu
    // suchen, sonst stünde dort zusätzlich „Keine Treffer“. Schlägt catalog/info fehl, wird trotzdem gesucht.
    enabled: catalog.isFetched && !catalogEmpty,
    // Während eine neue Suche lädt, bleiben die alten Treffer stehen (web/DESIGN.md, Zustände).
    placeholderData: keepPreviousData,
  })

  function resetFilters() {
    minPrice.reset()
    maxPrice.reset()
    minProtein.reset()
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
            value={text.value}
            onChange={(event) => text.edit(event.target.value)}
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

      <div className="mt-4 grid grid-cols-2 gap-4 sm:flex sm:items-start">
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
          {categories.error && (
            <span className="text-danger">Kategorien konnten nicht geladen werden.</span>
          )}
        </label>
        <NumberField
          label="Preis von (€)"
          field={minPrice}
          hint="Bitte eine Zahl wie 1,50 eingeben."
          disabled={catalogEmpty}
        />
        <NumberField
          label="Preis bis (€)"
          field={maxPrice}
          hint="Bitte eine Zahl wie 1,50 eingeben."
          disabled={catalogEmpty}
        />
        <NumberField
          label="Eiweiß ab (g pro 100 g)"
          field={minProtein}
          hint="Bitte eine Zahl wie 10 oder 2,5 eingeben."
          disabled={catalogEmpty}
          wide
        />
      </div>

      <CatalogLine info={catalog.data} failed={Boolean(catalog.error)} />

      <section className="mt-8" aria-live="polite">
        {search.error && <ErrorMessage error={search.error} fieldLabels={FIELD_LABELS} />}

        {!search.data && search.isLoading && <p className="text-ink-muted">Lädt …</p>}

        {search.data && (
          <Results
            data={search.data}
            searchText={query.q}
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
  filtersSet: boolean
  onResetFilters: () => void
  onPage: (page: number) => void
}

function Results({ data, searchText, filtersSet, onResetFilters, onPage }: ResultsProps) {
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
      {/* Eine Textsuche ist bei jeder Sortierung auf 200 Treffer begrenzt (Spezifikation 5.5). */}
      {searchText && data.totalCount >= TEXT_HIT_LIMIT && (
        <p className="mt-1 text-sm text-ink-muted">
          Mindestens {TEXT_HIT_LIMIT} Treffer, grenze die Suche ein.
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

function CatalogLine({ info, failed }: { info: CatalogInfo | undefined; failed: boolean }) {
  if (failed) {
    return <p className="mt-4 text-sm text-danger">Der Katalogstand konnte nicht geladen werden.</p>
  }
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

type UrlField = ReturnType<typeof useUrlField>

type NumberFieldProps = {
  label: string
  field: UrlField
  hint: string
  disabled: boolean
  wide?: boolean
}

// Zahlenfeld mit Hinweis, wenn die Eingabe ungültig ist. Ungültiges geht nicht an die API.
function NumberField({ label, field, hint, disabled, wide = false }: NumberFieldProps) {
  const hintId = useId()
  return (
    <div
      className={`flex flex-col gap-1 text-sm ${wide ? 'col-span-2 sm:col-span-1' : ''} sm:w-40`}
    >
      <label className="flex flex-col gap-1">
        {label}
        <input
          inputMode="decimal"
          value={field.value}
          onChange={(event) => field.edit(event.target.value)}
          disabled={disabled}
          aria-invalid={field.invalid}
          aria-describedby={field.invalid ? hintId : undefined}
          className={`${fieldClass} ${field.invalid ? 'border-danger' : ''}`}
        />
      </label>
      {/* Außerhalb des Labels, damit der Hinweis nicht Teil des Feldnamens wird */}
      {field.invalid && (
        <span id={hintId} className="text-danger">
          {hint}
        </span>
      )}
    </div>
  )
}

/**
 * Ein Eingabefeld, dessen Wert verzögert in die Adresszeile geschrieben wird.
 * toUrl macht aus dem Text den Wert für die URL (null = ungültig), toText aus der URL wieder einen Text.
 */
function useUrlField(
  params: URLSearchParams,
  key: string,
  toUrl: (text: string) => string | null,
  toText: (urlValue: string) => string,
  update: (changes: ParamChanges) => void,
) {
  const urlValue = params.get(key) ?? ''
  const [value, setValue] = useState(() => toText(urlValue))
  // Nur eine echte Eingabe schreibt in die URL. Das Laden einer Adresse (Zurück, Neuladen, geteilter
  // Link) darf sie nie selbst umschreiben.
  const edited = useRef(false)
  const debounced = useDebouncedValue(value, DEBOUNCE_MS)

  // Getippt: nach der Wartezeit in die URL schreiben, ungültige Eingaben nicht.
  useEffect(() => {
    const next = toUrl(debounced)
    if (edited.current && next !== null && next !== urlValue) update({ [key]: next })
    // eslint-disable-next-line react-hooks/exhaustive-deps -- nur auf neue Eingaben reagieren
  }, [debounced])

  // Von außen geändert (z. B. Zurück im Browser): Feld anpassen.
  useEffect(() => {
    if (toUrl(debounced) !== urlValue) {
      edited.current = false
      setValue(toText(urlValue))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- nur auf Änderungen der URL reagieren
  }, [urlValue])

  return {
    value,
    edit(next: string) {
      edited.current = true
      setValue(next)
    },
    reset() {
      edited.current = false
      setValue('')
    },
    // Erst nach der Wartezeit markieren, damit „1,“ beim Tippen von „1,5“ nicht aufblinkt.
    invalid: toUrl(debounced) === null,
  }
}

function euroToUrl(text: string): string | null {
  if (!isValidEuroInput(text)) return null
  const cents = parseEuroToCents(text)
  return cents === undefined ? '' : String(cents)
}

function centsToText(urlValue: string): string {
  const cents = Number(urlValue)
  return urlValue === '' || !Number.isFinite(cents) ? '' : centsToEuroInput(cents)
}

function decimalToUrl(text: string): string | null {
  if (!isValidDecimalInput(text)) return null
  const value = parseDecimal(text)
  return value === undefined ? '' : String(value)
}

function decimalToText(urlValue: string): string {
  return urlValue.replace('.', ',')
}

const fieldClass =
  'h-10 rounded-md border border-line-strong bg-white px-3 text-base focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent disabled:cursor-not-allowed disabled:opacity-50'
