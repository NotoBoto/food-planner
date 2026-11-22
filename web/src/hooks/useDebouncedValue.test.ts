import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DEBOUNCE_MS } from '../pages/SearchPage'
import { useDebouncedValue } from './useDebouncedValue'

// Mit Fake-Timern lässt sich die Zeit millisekundengenau vorspulen.
beforeEach(() => vi.useFakeTimers())
afterEach(() => vi.useRealTimers())

describe('useDebouncedValue', () => {
  it('gibt einen neuen Wert erst 300 ms nach der letzten Änderung weiter', () => {
    expect(DEBOUNCE_MS).toBe(300)
    const { result, rerender } = renderHook(({ value }) => useDebouncedValue(value, DEBOUNCE_MS), {
      initialProps: { value: '' },
    })

    rerender({ value: 'milch' })
    act(() => vi.advanceTimersByTime(299))
    expect(result.current).toBe('')

    act(() => vi.advanceTimersByTime(1))
    expect(result.current).toBe('milch')
  })

  it('startet die Wartezeit bei jeder Änderung neu', () => {
    const { result, rerender } = renderHook(({ value }) => useDebouncedValue(value, DEBOUNCE_MS), {
      initialProps: { value: '' },
    })

    rerender({ value: 'mil' })
    act(() => vi.advanceTimersByTime(200))
    rerender({ value: 'milch' })
    act(() => vi.advanceTimersByTime(200))
    // 400 ms seit „mil“, aber erst 200 ms seit „milch“: noch nichts weitergegeben
    expect(result.current).toBe('')

    act(() => vi.advanceTimersByTime(100))
    expect(result.current).toBe('milch')
  })
})
