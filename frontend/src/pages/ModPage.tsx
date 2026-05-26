import { useAuthStore } from '@/features/auth/store'
import { useBanUser, useUnbanUser } from '@/features/moderation/hooks'
import { Navigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { customInstance } from '@/api/client/axios-client'
import type { ApiResponseListUserListItem, UserListItem } from '@/api/generated/model'

const BAN_DURATIONS = [1, 6, 24, 72, 168] as const

function banLabel(hours: number) {
  if (hours < 24) return `${hours}h`
  return `${hours / 24}d`
}

function isBanned(bannedUntil?: string): boolean {
  if (!bannedUntil) return false
  return new Date(bannedUntil).getTime() > Date.now()
}

function formatBanExpiry(bannedUntil?: string): string {
  if (!bannedUntil) return ''
  return new Date(bannedUntil).toLocaleString()
}

function useModUsers() {
  return useQuery({
    queryKey: ['/api/v1/moderator/users'],
    queryFn: ({ signal }) =>
      customInstance<ApiResponseListUserListItem>({
        url: '/api/v1/moderator/users',
        method: 'GET',
        signal,
      }),
  })
}

export function ModPage() {
  const user = useAuthStore((s) => s.user)
  const token = useAuthStore((s) => s.token)
  const { data, isLoading, isError, error } = useModUsers()
  const banUser = useBanUser()
  const unbanUser = useUnbanUser()

  if (!token) {
    return <Navigate to="/login" replace />
  }

  const role = user?.role ?? ''
  const isMod = role === 'ROLE_MODERATOR' || role === 'ROLE_ADMIN'

  if (!isMod) {
    return <div className="py-4 text-sm text-red-600">Access denied. Moderators only.</div>
  }

  const handleBan = (userId: number, durationHours: number) => {
    banUser.mutate({ id: userId, durationHours })
  }

  const handleUnban = (userId: number) => {
    unbanUser.mutate(userId)
  }

  const users = data?.data

  return (
    <div className="py-4">
      <h1 className="text-base font-bold mb-3">Moderator Dashboard</h1>

      {banUser.isError && (
        <p className="text-xs text-red-600 mb-2">Failed to ban user.</p>
      )}
      {unbanUser.isError && (
        <p className="text-xs text-red-600 mb-2">Failed to unban user.</p>
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
              <th className="py-1 pr-4 font-normal">ban</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u: UserListItem) => {
              const banned = isBanned(u.bannedUntil)
              return (
                <tr
                  key={u.id}
                  className={`border-b border-[#d9d9d9] text-sm ${banned ? 'bg-red-50' : ''}`}
                >
                  <td className="py-1 pr-4">
                    {u.username}
                    {banned && (
                      <span className="ml-1 text-xs text-red-600">
                        (banned until {formatBanExpiry(u.bannedUntil)})
                      </span>
                    )}
                  </td>
                  <td className="py-1 pr-4 text-gray-500">{u.email}</td>
                  <td className="py-1 pr-4 text-gray-500 text-xs">{u.karma}</td>
                  <td className="py-1 pr-4">{u.role.replace('ROLE_', '')}</td>
                  <td className="py-1">
                    {banned ? (
                      <button
                        onClick={() => handleUnban(u.id)}
                        className="text-xs text-green-600 hover:underline cursor-pointer"
                      >
                        unban
                      </button>
                    ) : (
                      <span className="text-xs">
                        ban:{' '}
                        {BAN_DURATIONS.map((h) => (
                          <button
                            key={h}
                            onClick={() => handleBan(u.id, h)}
                            className="mr-1 text-xs text-red-600 hover:underline cursor-pointer"
                          >
                            {banLabel(h)}
                          </button>
                        ))}
                      </span>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      )}
    </div>
  )
}
