import { useAuthStore } from '@/features/auth/store'
import { Navigate } from 'react-router-dom'

export function ModPage() {
  const user = useAuthStore((s) => s.user)
  const token = useAuthStore((s) => s.token)

  if (!token) {
    return <Navigate to="/login" replace />
  }

  const role = user?.role ?? ''
  const isMod = role === 'ROLE_MODERATOR' || role === 'ROLE_ADMIN'

  if (!isMod) {
    return <div className="py-4 text-sm text-red-600">Access denied. Moderators only.</div>
  }

  return (
    <div className="py-4">
      <h1 className="text-base font-bold mb-3">Moderator Dashboard</h1>
      <p className="text-sm text-gray-500">
        Moderator tools are available throughout the site. You can delete any post or comment using
        the delete buttons on post detail pages and comments.
      </p>
    </div>
  )
}
