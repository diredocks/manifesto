import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="py-8 text-center text-sm">
      <p>Page not found.</p>
      <Link to="/" className="text-primary">Go home</Link>
    </div>
  )
}
