import { useChangeUserRole } from '@/api/generated/admin/admin'
import { useQueryClient } from '@tanstack/react-query'

export function useChangeRole() {
  const queryClient = useQueryClient()

  return useChangeUserRole({
    mutation: {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['/api/v1/admin/users'] })
        queryClient.invalidateQueries({ queryKey: ['/api/v1/moderator/users'] })
      },
    },
  })
}
