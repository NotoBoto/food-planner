import { setupServer } from 'msw/node'

// Nachgebaute API für die Tests. Jeder Test legt mit server.use(...) fest, was sie antwortet.
export const server = setupServer()
