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

/** ISO-Zeitpunkt → „17.11.2026“ (deutsche Zeit), ein ungültiger Wert als „–“ */
export function formatDate(iso: string): string {
  const parsed = new Date(iso)
  return Number.isNaN(parsed.getTime()) ? '–' : date.format(parsed)
}

// Eingaben in Zahlenfeldern: nur Ziffern, Komma oder Punkt als Dezimaltrenner, kein Tausenderpunkt.
// Euro mit höchstens zwei Nachkommastellen („1,50“), Gramm mit beliebig vielen („2,5“).
const EURO_INPUT = /^(\d{1,9})(?:[.,](\d{1,2}))?$/
const DECIMAL_INPUT = /^\d{1,6}(?:[.,]\d{1,3})?$/

// Fürs Eingabefeld ohne Tausenderpunkt, damit „1234“ beim Zurücklesen nicht zu 1,234 € wird.
const euroInput = new Intl.NumberFormat('de-DE', { maximumFractionDigits: 2, useGrouping: false })

/** Eingabe in Euro → Cent, ohne Rundung über Kommazahlen („1,5“ → 150). Leer oder ungültig → undefined. */
export function parseEuroToCents(text: string): number | undefined {
  const match = EURO_INPUT.exec(text.trim())
  if (!match) {
    return undefined
  }
  return Number(match[1]) * 100 + Number((match[2] ?? '').padEnd(2, '0'))
}

/** Ob ein Euro-Feld leer ist oder eine gültige Eingabe enthält */
export function isValidEuroInput(text: string): boolean {
  return text.trim() === '' || EURO_INPUT.test(text.trim())
}

/** Cent → Text fürs Eingabefeld („1,5“ statt „1,50 €“, „1234“ statt „1.234“) */
export function centsToEuroInput(cents: number | undefined): string {
  return cents === undefined ? '' : euroInput.format(cents / 100)
}

/** Dezimalzahl mit Komma oder Punkt („2,5“), nicht negativ. Leer oder ungültig → undefined. */
export function parseDecimal(text: string): number | undefined {
  const trimmed = text.trim()
  return DECIMAL_INPUT.test(trimmed) ? Number(trimmed.replace(',', '.')) : undefined
}

/** Ob ein Dezimal-Feld leer ist oder eine gültige Eingabe enthält */
export function isValidDecimalInput(text: string): boolean {
  return text.trim() === '' || DECIMAL_INPUT.test(text.trim())
}
