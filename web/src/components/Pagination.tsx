type Props = {
  page: number
  totalPages: number
  onChange: (page: number) => void
}

// „Zurück“, „Seite 3 von 10“, „Weiter“, an den Rändern ausgegraut (web/DESIGN.md, Suche).
export function Pagination({ page, totalPages, onChange }: Props) {
  if (totalPages <= 1) {
    return null
  }

  return (
    <nav aria-label="Seiten" className="mt-8 flex items-center justify-center gap-6 text-sm">
      <button
        type="button"
        onClick={() => onChange(page - 1)}
        disabled={page <= 1}
        className={buttonClass}
      >
        ‹ Zurück
      </button>
      <span className="tabular-nums">
        Seite {page} von {totalPages}
      </span>
      <button
        type="button"
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages}
        className={buttonClass}
      >
        Weiter ›
      </button>
    </nav>
  )
}

const buttonClass =
  'rounded-md border border-line-strong px-3 py-1.5 hover:bg-accent-soft disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:bg-transparent'
