import { describe, expect, it } from 'vitest'
import {
  centsToEuroInput,
  formatDate,
  formatGrams,
  formatKcal,
  formatPackage,
  formatPrice,
  formatPricePerUnit,
  parseEuroToCents,
  per100,
} from './format'

// Intl trennt Betrag und Einheit mit einem geschützten Leerzeichen.
const nbsp = '\u00a0'

describe('format', () => {
  it('schreibt Preise deutsch', () => {
    expect(formatPrice(109)).toBe(`1,09${nbsp}€`)
    expect(formatPricePerUnit(178, 'G')).toBe(`1,78${nbsp}€/kg`)
    expect(formatPricePerUnit(99, 'ML')).toBe(`0,99${nbsp}€/l`)
  })

  it('zeigt Packungsgrößen in g / kg bzw. ml / l', () => {
    expect(formatPackage(500, 'G')).toBe('500 g')
    expect(formatPackage(1000, 'G')).toBe('1 kg')
    expect(formatPackage(750, 'ML')).toBe('750 ml')
    expect(formatPackage(1500, 'ML')).toBe('1,5 l')
  })

  it('nennt den Bezug der Nährwerte je nach Einheit', () => {
    expect(per100('G')).toBe('pro 100 g')
    expect(per100('ML')).toBe('pro 100 ml')
  })

  it('zeigt fehlende Nährwerte als Strich', () => {
    expect(formatGrams(3.4)).toBe(`3,4${nbsp}g`)
    expect(formatGrams(12)).toBe(`12,0${nbsp}g`)
    expect(formatGrams(null)).toBe('–')
    expect(formatKcal(64.4)).toBe(`64${nbsp}kcal`)
    expect(formatKcal(null)).toBe('–')
  })

  it('schreibt Daten in deutscher Zeit', () => {
    expect(formatDate('2026-11-17T10:00:00Z')).toBe('17.11.2026')
    // 23:30 UTC ist in Deutschland schon der nächste Tag
    expect(formatDate('2026-11-17T23:30:00Z')).toBe('18.11.2026')
  })

  it('liest Euro-Eingaben mit Komma oder Punkt', () => {
    expect(parseEuroToCents('1,50')).toBe(150)
    expect(parseEuroToCents('2.5')).toBe(250)
    expect(parseEuroToCents(' 3 ')).toBe(300)
    expect(parseEuroToCents('')).toBeUndefined()
    expect(parseEuroToCents('abc')).toBeUndefined()
    expect(parseEuroToCents('-1')).toBeUndefined()
    expect(centsToEuroInput(150)).toBe('1,5')
    expect(centsToEuroInput(undefined)).toBe('')
  })
})
