import { useParams } from 'react-router'
import { NotFoundPage } from './NotFoundPage'

export function ProductPage() {
  // Aus Adresse /product/42 wird id = 42
  const id = Number(useParams().id)

  // Ungültige Produkt ID leitet auf die 404-Seite weiter
  if (!Number.isInteger(id) || id <= 0) {
    return <NotFoundPage />
  }

  return <h1 className="text-2xl font-semibold">Produkt: {id}</h1>
}
