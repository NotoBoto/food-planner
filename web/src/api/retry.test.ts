import { describe, expect, it } from 'vitest'
import { ApiError } from './errorHandling'
import { shouldRetry } from './retry'

describe('shouldRetry', () => {
  it('wiederholt keine 404', () => {
    expect(shouldRetry(0, new ApiError(404, null))).toBe(false)
  })

  it('wiederholt eine 503', () => {
    expect(shouldRetry(0, new ApiError(503, null))).toBe(true)
  })

  it('wiederholt bei fehlender Verbindung', () => {
    expect(shouldRetry(0, new TypeError('Failed to fetch'))).toBe(true)
  })

  it('hört nach dem dritten Versuch auf', () => {
    expect(shouldRetry(3, new ApiError(503, null))).toBe(false)
  })
})
