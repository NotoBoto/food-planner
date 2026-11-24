import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from './server'

// jsdom kann nicht scrollen, die Seitennavigation ruft es aber auf.
window.scrollTo = () => {}

// Anfragen ohne passende Antwort im Test sind ein Fehler, damit kein Test still ins Leere fragt.
beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))

// Nach jedem Test die gezeichnete Seite abräumen und die Antworten der nachgebauten API zurücksetzen.
afterEach(() => {
  cleanup()
  server.resetHandlers()
})

afterAll(() => server.close())
