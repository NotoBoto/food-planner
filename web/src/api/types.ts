export type Unit = 'G' | 'ML'

// Wie der Record ProductSummary im Backend: Marke, Kategorie und Nährwerte können fehlen.
// Nährwerte gibt es laut Spezifikation nur alle vier oder keiner.
export type ProductSummary = {
  id: number
  productKey: string
  name: string
  brand: string | null
  category: string | null
  packageGrams: number
  unit: Unit
  priceCents: number
  pricePerKgCents: number
  protein100g: number | null
  fat100g: number | null
  carbs100g: number | null
  kcal100g: number | null
}

export type PagedResult<T> = {
  items: T[]
  page: number
  pageSize: number
  totalCount: number
  totalPages: number
}

export type CatalogInfo = {
  productCount: number
  lastScrapedAt: string | null
}
