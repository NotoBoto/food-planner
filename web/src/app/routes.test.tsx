import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'
import { routes } from './routes'

// Zeigt die App so, als hätte man die Adresse im Browser eingegeben.
function renderPage(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(<RouterProvider router={router} />)
}

describe('Routing', () => {
  it.each([
    ['/', 'Produkte suchen'],
    ['/product/42', 'Produkt: 42'],
    ['/list', 'Einkaufsliste'],
    ['/admin', 'Admin Dashboard'],
  ])('zeigt unter %s die Überschrift „%s“', async (path, heading) => {
    renderPage(path)
    expect(await screen.findByRole('heading', { name: heading })).toBeInTheDocument()
  })

  it.each(['/gibt-es-nicht', '/product/abc', '/product/0'])(
    'zeigt „Seite nicht gefunden“ für %s',
    async (path) => {
      renderPage(path)
      expect(
        await screen.findByRole('heading', { name: 'Seite nicht gefunden' }),
      ).toBeInTheDocument()
    },
  )
})
