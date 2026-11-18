import type { components, operations } from './schema'

// Kurznamen für die Typen, die aus openapi.json generiert werden (npm run api:types).
// Ändert sich ein DTO im Backend, ändern sich diese Typen mit, und der Build zeigt jede betroffene Stelle.
type Schemas = components['schemas']

export type ProductSummary = Schemas['ProductSummary']
export type PagedProductSummaries = Schemas['PagedResultProductSummary']
export type CatalogInfo = Schemas['CatalogInfo']

/** Query-Parameter von GET /products/search */
export type SearchQuery = NonNullable<operations['search']['parameters']['query']>
export type SearchSort = NonNullable<SearchQuery['sort']>
