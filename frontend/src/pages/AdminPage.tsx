import { useAuthStore } from '@/features/auth/store'
import { useAdminUsers, useChangeRole } from '@/features/admin/hooks'
import { Navigate } from 'react-router-dom'
import type { UserListItem } from '@/api/generated/model'

const ROLES = ['ROLE_USER', 'ROLE_MODERATOR', 'ROLE_ADMIN'] as const

function roleLabel(role: string) {
  return role.replace('ROLE_', '')
}

function UserRow({ user, onChangeRole }: { user: UserListItem; onChangeRole: (userId: number, role: string) => void }) {
  return (
    <tr className="border-b border-[#d9d9d9] text-sm">
      <td className="py-1 pr-4">{user.username}</td>
      <td className="py-1 pr-4 text-gray-500">{user.email}</td>
      <td className="py-1 pr-4 text-gray-500 text-xs">{user.karma}</td>
      <td className="py-1">
        <span className="font-medium">{roleLabel(user.role)}</span>
        {ROLES.filter((r) => r !== user.role).map((r) => (
          <button
            key={r}
            onClick={() => onChangeRole(user.id, r)}
            className="ml-2 text-xs text-[#ff6600] hover:underline cursor-pointer"
          >
            make {roleLabel(r).toLowerCase()}
          </button>
        ))}
      </td>
    </tr>
  )
}

export function AdminPage() {
  const token = useAuthStore((s) => s.token)
  const user = useAuthStore((s) => s.user)
  const { data, isLoading, isError, error } = useAdminUsers()
  const changeRole = useChangeRole()

  if (!token) {
    return <Navigate to="/login" replace />
  }

  if (user?.role !== 'ROLE_ADMIN') {
    return <div className="py-4 text-sm text-red-600">Access denied. Admins only.</div>
  }

  const handleChangeRole = (userId: number, role: string) => {
    changeRole.mutate({ id: userId, params: { role } })
  }

  const users = data?.data

  return (
    <div className="py-4">
      <h1 className="text-base font-bold mb-3">Admin Dashboard</h1>

      {changeRole.isError && (
        <p className="text-xs text-red-600 mb-2">Failed to change role.</p>
      )}

      {isLoading && <p className="text-sm text-gray-500">Loading...</p>}

      {isError && (
        <p className="text-sm text-red-600">
          {error instanceof Error ? error.message : 'Failed to load users.'}
        </p>
      )}

      {users && users.length === 0 && (
        <p className="text-sm text-gray-500">No users found.</p>
      )}

      {users && users.length > 0 && (
        <table className="w-full">
          <thead>
            <tr className="text-left text-xs text-gray-500 border-b border-[#d9d9d9]">
              <th className="py-1 pr-4 font-normal">user</th>
              <th className="py-1 pr-4 font-normal">email</th>
              <th className="py-1 pr-4 font-normal">karma</th>
              <th className="py-1 pr-4 font-normal">role</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <UserRow key={u.id} user={u} onChangeRole={handleChangeRole} />
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
