import { NavLink, Outlet } from 'react-router'

export function Layout() {
  return (
    <div className="min-h-screen">
      <header className="border-b border-line bg-panel">
        <div className="mx-auto flex max-w-5xl items-center gap-8 px-4 py-3">
          <NavLink to="/" className="font-semibold text-accent">
            FoodPlanner
          </NavLink>
          <nav className="flex flex-1 gap-6">
            <NavLink to="/" end className={linkClass}>
              Suche
            </NavLink>
            <NavLink to="/list" className={linkClass}>
              Einkaufsliste
            </NavLink>
          </nav>
          <NavLink to="/admin" className={linkClass}>
            Admin
          </NavLink>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8">
        {/* Hier setzt der Router die aktuelle Seite ein */}
        <Outlet />
      </main>
    </div>
  )
}

// NavLink fragt diese Funktion: aktiv → unterstrichen, sonst grau.
function linkClass({ isActive }: { isActive: boolean }) {
  return isActive
    ? 'text-sm text-ink underline decoration-accent decoration-2 underline-offset-8'
    : 'text-sm text-ink-muted hover:text-ink'
}