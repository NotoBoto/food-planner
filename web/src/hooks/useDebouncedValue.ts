import { useEffect, useState } from 'react'

/** Gibt den Wert erst weiter, wenn er sich `delay` Millisekunden lang nicht geändert hat. */
export function useDebouncedValue<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay)
    // Ändert sich der Wert vorher, wird der alte Timer verworfen.
    return () => clearTimeout(timer)
  }, [value, delay])

  return debounced
}
