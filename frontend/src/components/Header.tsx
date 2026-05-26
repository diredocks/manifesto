import { useEffect } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/features/auth/store'
import { useCurrentUser } from '@/features/auth/hooks'
import { useUnreadCount } from '@/features/notifications/hooks'
import { useQueryClient } from '@tanstack/react-query'

export function Header() {
  const { user, token, logout } = useAuthStore()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { data: meData } = useCurrentUser()
  const { data: unreadData } = useUnreadCount()
  const location = useLocation()
  const loggedIn = !!token

  useEffect(() => {
    if (loggedIn) {
      queryClient.invalidateQueries({ queryKey: ['/api/v1/notifications/unread-count'] })
    }
  }, [location.pathname, loggedIn, queryClient])

  const currentUser = meData?.data ?? user
  const unreadCount = unreadData?.data ?? 0
  const role = currentUser?.role ?? ''
  const isMod = role === 'ROLE_MODERATOR' || role === 'ROLE_ADMIN'
  const isAdmin = role === 'ROLE_ADMIN'

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
            <Link to="/notifications" className="text-white visited:text-white no-underline">
              notifications{unreadCount > 0 ? ` (${unreadCount})` : ''}
            </Link>
            <span className="text-white/70">|</span>
            {isMod && (
              <>
                <Link to="/mod" className="text-white visited:text-white no-underline">mod</Link>
                <span className="text-white/70">|</span>
              </>
            )}
            {isAdmin && (
              <>
                <Link to="/admin" className="text-white visited:text-white no-underline">admin</Link>
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
