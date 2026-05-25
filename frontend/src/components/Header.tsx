import { Link } from 'react-router-dom'

export function Header() {
  return (
    <header className="flex items-center gap-2 bg-primary px-2 py-1 text-sm text-white">
      <Link to="/" className="font-bold text-white no-underline visited:text-white">
        Manifesto
      </Link>
      <nav className="flex gap-2">
        <Link to="/new" className="text-white visited:text-white">new</Link>
        <span className="text-white/70">|</span>
        <Link to="/ask" className="text-white visited:text-white">ask</Link>
      </nav>
      <div className="ml-auto flex gap-2">
        <Link to="/login" className="text-white visited:text-white">login</Link>
      </div>
    </header>
  )
}
