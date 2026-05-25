import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/features/auth/store'
import { useCurrentUser } from '@/features/auth/hooks'
import { useQueryClient } from '@tanstack/react-query'

export function Header() {
  const { user, token, logout } = useAuthStore()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { data: meData } = useCurrentUser()

  const currentUser = meData?.data ?? user
  const loggedIn = !!token

  const handleLogout = () => {
    logout()
    queryClient.clear()
    navigate('/')
  }

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
      <div className="ml-auto flex gap-2 items-center">
        {loggedIn ? (
          <>
            <Link to={`/user/${currentUser?.username}`} className="text-white visited:text-white">
              {currentUser?.username}
            </Link>
            <span className="text-white/70">|</span>
            <button onClick={handleLogout} className="text-white cursor-pointer">
              logout
            </button>
          </>
        ) : (
          <Link to="/login" className="text-white visited:text-white">login</Link>
        )}
      </div>
    </header>
  )
}
