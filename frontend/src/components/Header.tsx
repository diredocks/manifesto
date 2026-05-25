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
  const role = currentUser?.role ?? ''
  const isMod = role === 'ROLE_MODERATOR' || role === 'ROLE_ADMIN'

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
        <Link to="/new" className="text-white visited:text-white no-underline">new</Link>
        <span className="text-white/70">|</span>
        <Link to="/ask" className="text-white visited:text-white no-underline">ask</Link>
        <span className="text-white/70">|</span>
        <Link to="/submit" className="text-white visited:text-white no-underline">submit</Link>
      </nav>
      <div className="ml-auto flex gap-2 items-center">
        {loggedIn ? (
          <>
            {isMod && (
              <>
                <Link to="/mod" className="text-white visited:text-white no-underline">mod</Link>
                <span className="text-white/70">|</span>
              </>
            )}
            <Link to={`/user/${currentUser?.username}`} className="text-white visited:text-white no-underline">
              {currentUser?.username}
            </Link>
            <span className="text-white/70">|</span>
            <button onClick={handleLogout} className="text-white cursor-pointer">
              logout
            </button>
          </>
        ) : (
          <Link to="/login" className="text-white visited:text-white no-underline">login</Link>
        )}
      </div>
    </header>
  )
}
