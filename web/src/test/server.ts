import { setupServer } from 'msw/node'
import { defaultHandlers } from './handlers'

// Nachgebaute API für die Tests. Ohne server.use(...) antwortet sie mit den Standardantworten.
export const server = setupServer(...defaultHandlers)
