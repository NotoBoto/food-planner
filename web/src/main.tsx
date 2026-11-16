import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter, RouterProvider } from 'react-router'
import { ApiError } from './api/errorHandling'
import { routes } from './app/routes'
import './index.css'

const router = createBrowserRouter(routes)
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // 4xx wiederholen bringt nichts, nur Netzwerkfehler und 5xx erneut versuchen.
      retry: (failureCount, error) =>
        !(error instanceof ApiError && error.status < 500) && failureCount < 3,
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
)
