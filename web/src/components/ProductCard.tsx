import { Link } from 'react-router'
import type { ProductSummary } from '../api/types'
import {
  formatGrams,
  formatKcal,
  formatPackage,
  formatPrice,
  formatPricePerUnit,
  per100,
} from '../format'

type Props = {
  product: ProductSummary
  onAdd: (product: ProductSummary) => void
}

export function ProductCard({ product, onAdd }: Props) {
  const hasNutrients = product.protein100g !== null && product.kcal100g !== null
  const details = [product.brand, formatPackage(product.packageGrams, product.unit)]
    .filter(Boolean)
    .join(' · ')

  return (
    // relative: Der Link im Namen legt sich über die ganze Karte, der Button liegt darüber.
    <article className="relative flex flex-col rounded-md border border-line bg-panel p-4">
      <h2 className="line-clamp-2 font-medium">
        <Link
          to={`/product/${product.id}`}
          className="after:absolute after:inset-0 hover:underline focus-visible:outline-2 focus-visible:outline-accent"
        >
          {product.name}
        </Link>
      </h2>
      <p className="mt-1 text-sm text-ink-muted">{details}</p>

      <p className="mt-4 text-lg font-semibold tabular-nums">{formatPrice(product.priceCents)}</p>
      <p className="text-sm text-ink-muted tabular-nums">
        {formatPricePerUnit(product.pricePerKgCents, product.unit)}
      </p>

      {/* Umbrochen wird nur am „·“, die beiden Angaben bleiben jeweils zusammen. */}
      <p className="mt-2 text-sm text-ink-muted tabular-nums">
        {hasNutrients ? (
          <>
            <span className="whitespace-nowrap">
              {per100(product.unit)}: {formatKcal(product.kcal100g)}
            </span>{' '}
            · <span className="whitespace-nowrap">Eiweiß {formatGrams(product.protein100g)}</span>
          </>
        ) : (
          'Keine Nährwerte'
        )}
      </p>

      <button
        type="button"
        onClick={() => onAdd(product)}
        aria-label={`${product.name} zur Liste hinzufügen`}
        className="relative z-10 mt-4 self-end rounded-md border border-line-strong px-3 py-1 text-lg leading-none hover:bg-accent-soft focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
      >
        +
      </button>
    </article>
  )
}
