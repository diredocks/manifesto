import { useGetNotifications, useGetUnreadCount, useMarkAsRead } from '@/api/generated/notifications/notifications'
import { useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/features/auth/store'

export function useNotifications(page = 0, size = 20) {
  return useGetNotifications({ page, size })
}

export function useUnreadCount() {
  const token = useAuthStore((s) => s.token)
  return useGetUnreadCount({
    query: {
      enabled: !!token,
    },
  })
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient()
  return useMarkAsRead({
    mutation: {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['/api/v1/notifications/unread-count'] })
        queryClient.invalidateQueries({ queryKey: ['/api/v1/notifications'] })
      },
    },
  })
}
