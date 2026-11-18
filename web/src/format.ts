// Deutsche Schreibweisen für Preise, Mengen und Daten (web/DESIGN.md, Gestaltung).

const euro = new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' })
const oneDecimal = new Intl.NumberFormat('de-DE', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
})
const upToTwoDecimals = new Intl.NumberFormat('de-DE', { maximumFractionDigits: 2 })
const wholeNumber = new Intl.NumberFormat('de-DE', { maximumFractionDigits: 0 })
const date = new Intl.DateTimeFormat('de-DE', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  timeZone: 'Europe/Berlin',
})

const isMl = (unit: string) => unit === 'ML'

/** 109 → „1,09 €“ */
export function formatPrice(cents: number): string {
  return euro.format(cents / 100)
}

/** Kilo- bzw. Literpreis: „1,78 €/kg“ oder „0,99 €/l“ */
export function formatPricePerUnit(cents: number, unit: string): string {
  return `${formatPrice(cents)}/${isMl(unit) ? 'l' : 'kg'}`
}

/** Packungsgröße: unter 1000 in g bzw. ml, darüber in kg bzw. l („500 g“, „1,5 l“) */
export function formatPackage(grams: number, unit: string): string {
  if (grams < 1000) {
    return `${grams} ${isMl(unit) ? 'ml' : 'g'}`
  }
  return `${upToTwoDecimals.format(grams / 1000)} ${isMl(unit) ? 'l' : 'kg'}`
}

/** Bezug der Nährwerte: „pro 100 g“ oder „pro 100 ml“ */
export function per100(unit: string): string {
  return isMl(unit) ? 'pro 100 ml' : 'pro 100 g'
}

// Zwischen Zahl und Einheit steht ein geschütztes Leerzeichen, damit „g“ nie allein umbricht.
/** Gramm mit einer Nachkommastelle („3,4 g“), fehlender Wert als „–“ */
export function formatGrams(value: number | null): string {
  return value === null ? '–' : `${oneDecimal.format(value)}\u00a0g`
}

/** Energie ganzzahlig („64 kcal“), fehlender Wert als „–“ */
export function formatKcal(value: number | null): string {
  return value === null ? '–' : `${wholeNumber.format(value)}\u00a0kcal`
}

/** Ganze Zahl mit Tausenderpunkt („1.234“) */
export function formatCount(value: number): string {
  return wholeNumber.format(value)
}

/** ISO-Zeitpunkt → „17.11.2026“ (deutsche Zeit) */
export function formatDate(iso: string): string {
  return date.format(new Date(iso))
}

/** Eingabe in Euro mit Komma oder Punkt → Cent. Leer oder ungültig → undefined. */
export function parseEuroToCents(text: string): number | undefined {
  const value = parseDecimal(text)
  return value === undefined ? undefined : Math.round(value * 100)
}

/** Cent → Text fürs Eingabefeld („1,5“ statt „1,50 €“) */
export function centsToEuroInput(cents: number | undefined): string {
  return cents === undefined ? '' : upToTwoDecimals.format(cents / 100)
}

/** Dezimalzahl mit Komma oder Punkt, nicht negativ. Leer oder ungültig → undefined. */
export function parseDecimal(text: string): number | undefined {
  const normalized = text.trim().replace(',', '.')
  if (normalized === '') {
    return undefined
  }
  const value = Number(normalized)
  return Number.isFinite(value) && value >= 0 ? value : undefined
}
