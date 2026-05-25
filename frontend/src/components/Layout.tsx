import { Outlet } from 'react-router-dom'
import { Header } from './Header'

export function Layout() {
  return (
    <div className="mx-auto max-w-[1100px]">
      <Header />
      <main className="px-2 py-2">
        <Outlet />
      </main>
    </div>
  )
}
