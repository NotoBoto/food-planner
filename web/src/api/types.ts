export type Unit = 'G' | 'ML'

export type ProductSummary = {
  id: number
  productKey: string
  name: string
  brand: string
  category: string
  packageGrams: number
  unit: Unit
  priceCents: number
  pricePerKgCents: number
  protein100g: number
  fat100g: number
  carbs100g: number
  kcal100g: number
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