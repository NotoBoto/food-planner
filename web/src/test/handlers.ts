import { http, HttpResponse } from 'msw'
import type { CatalogInfo, PagedProductSummaries, ProductSummary } from '../api/types'

// Standardantworten der nachgebauten API. Einzelne Tests überschreiben sie mit server.use(...).

export function product(overrides: Partial<ProductSummary> = {}): ProductSummary {
  return {
    id: 1,
    productKey: 'aldi:1001',
    name: 'Haferflocken zart',
    brand: 'Golden Bridge',
    category: 'Frühstück',
    packageGrams: 500,
    unit: 'G',
    priceCents: 89,
    pricePerKgCents: 178,
    protein100g: 13.5,
    fat100g: 7,
    carbs100g: 58.7,
    kcal100g: 372,
    ...overrides,
  }
}

export function page(
  items: ProductSummary[],
  overrides: Partial<PagedProductSummaries> = {},
): PagedProductSummaries {
  return {
    items,
    page: 1,
    pageSize: 20,
    totalCount: items.length,
    totalPages: items.length === 0 ? 0 : 1,
    ...overrides,
  }
}

export const catalogInfo: CatalogInfo = { productCount: 312, lastScrapedAt: '2026-11-17T10:00:00Z' }

export const defaultHandlers = [
  http.get('*/api/v1/catalog/info', () => HttpResponse.json(catalogInfo)),
  http.get('*/api/v1/categories', () => HttpResponse.json(['Frühstück', 'Milchprodukte'])),
  http.get('*/api/v1/products/search', () => HttpResponse.json(page([product()]))),
]
