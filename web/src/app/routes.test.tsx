import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderRoute } from '../test/render'

describe('Routing', () => {
  it.each([
    ['/', 'Produkte suchen'],
    ['/product/42', 'Produkt: 42'],
    ['/list', 'Einkaufsliste'],
    ['/admin', 'Review'],
  ])('zeigt unter %s die Überschrift „%s“', async (path, heading) => {
    renderRoute(path)
    expect(await screen.findByRole('heading', { name: heading })).toBeInTheDocument()
  })

  it.each(['/gibt-es-nicht', '/product/abc', '/product/0'])(
    'zeigt „Seite nicht gefunden“ für %s',
    async (path) => {
      renderRoute(path)
      expect(
        await screen.findByRole('heading', { name: 'Seite nicht gefunden' }),
      ).toBeInTheDocument()
    },
  )
})
