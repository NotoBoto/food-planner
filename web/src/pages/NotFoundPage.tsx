import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <>
      <h1 className="text-2xl font-semibold">Seite nicht gefunden</h1>
      <p className="mt-2 text-ink-muted">
        Diese Adresse gibt es nicht. Prüf die URL oder geh zurück zur Suche.
      </p>
      <Link
        to="/"
        className="mt-6 inline-block rounded-md bg-accent px-4 py-2 text-white hover:bg-accent/90"
      >
        Zur Suche
      </Link>
    </>
  )
}
