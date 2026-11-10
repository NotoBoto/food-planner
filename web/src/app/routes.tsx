import type { RouteObject } from 'react-router'
import { AdminPage } from '../pages/AdminPage'
import { ListPage } from '../pages/ListPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { ProductPage } from '../pages/ProductPage'
import { SearchPage } from '../pages/SearchPage'
import { Layout } from './Layout'

export const routes: RouteObject[] = [
  {
    element: <Layout />,
    children: [
      { index: true, element: <SearchPage /> },
      { path: 'product/:id', element: <ProductPage /> },
      { path: 'list', element: <ListPage /> },
      { path: 'admin', element: <AdminPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
