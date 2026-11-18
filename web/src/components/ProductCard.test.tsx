import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { product } from '../test/handlers'
import { ProductCard } from './ProductCard'

function renderCard(overrides: Parameters<typeof product>[0], onAdd = vi.fn()) {
  render(
    <MemoryRouter>
      <ProductCard product={product(overrides)} onAdd={onAdd} />
    </MemoryRouter>,
  )
  return onAdd
}

// Testing Library macht aus dem geschützten Leerzeichen vor „€“ ein normales,
// deshalb steht hier ein normales Leerzeichen.
describe('ProductCard', () => {
  it('zeigt Gramm, Kilopreis und Nährwerte pro 100 g', () => {
    renderCard({ unit: 'G', packageGrams: 500, pricePerKgCents: 178 })
    expect(screen.getByText('Golden Bridge · 500 g')).toBeInTheDocument()
    expect(screen.getByText('1,78 €/kg')).toBeInTheDocument()
    expect(screen.getByText('pro 100 g: 372 kcal')).toBeInTheDocument()
    expect(screen.getByText('Eiweiß 13,5 g')).toBeInTheDocument()
  })

  it('zeigt Liter, Literpreis und Nährwerte pro 100 ml', () => {
    renderCard({
      name: 'Hafer Drink',
      unit: 'ML',
      packageGrams: 1500,
      pricePerKgCents: 99,
      kcal100g: 46,
      protein100g: 0.6,
    })
    expect(screen.getByText('Golden Bridge · 1,5 l')).toBeInTheDocument()
    expect(screen.getByText('0,99 €/l')).toBeInTheDocument()
    expect(screen.getByText('pro 100 ml: 46 kcal')).toBeInTheDocument()
    expect(screen.getByText('Eiweiß 0,6 g')).toBeInTheDocument()
  })

  it('kommt ohne Marke und ohne Nährwerte aus', () => {
    renderCard({ brand: null, protein100g: null, fat100g: null, carbs100g: null, kcal100g: null })
    expect(screen.getByText('500 g')).toBeInTheDocument()
    expect(screen.getByText('Keine Nährwerte')).toBeInTheDocument()
  })

  it('verlinkt auf die Produktseite und legt das Produkt auf die Liste', async () => {
    const onAdd = renderCard({ id: 42 })
    expect(screen.getByRole('link', { name: 'Haferflocken zart' })).toHaveAttribute(
      'href',
      '/product/42',
    )
    await userEvent.click(
      screen.getByRole('button', { name: 'Haferflocken zart zur Liste hinzufügen' }),
    )
    expect(onAdd).toHaveBeenCalledWith(expect.objectContaining({ id: 42 }))
  })
})
