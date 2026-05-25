import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import {
  useLogin as useLoginMutation,
  useRegister as useRegisterMutation,
  useMe,
} from '@/api/generated/authentication/authentication'
import { useAuthStore } from './store'

export function useLogin() {
  const setAuth = useAuthStore((s) => s.setAuth)
  const queryClient = useQueryClient()

  return useLoginMutation({
    mutation: {
      onSuccess: (response) => {
        if (response.data) {
          setAuth(response.data.token, {
            id: 0,
            username: response.data.username,
            email: '',
            karma: 0,
            role: response.data.role,
          })
          queryClient.invalidateQueries({ queryKey: ['/api/v1/auth/me'] })
        }
      },
    },
  })
}

export function useRegister() {
  const setAuth = useAuthStore((s) => s.setAuth)
  const queryClient = useQueryClient()

  return useRegisterMutation({
    mutation: {
      onSuccess: (response) => {
        if (response.data) {
          setAuth(response.data.token, {
            id: 0,
            username: response.data.username,
            email: '',
            karma: 0,
            role: response.data.role,
          })
          queryClient.invalidateQueries({ queryKey: ['/api/v1/auth/me'] })
        }
      },
    },
  })
}

export function useCurrentUser() {
  const token = useAuthStore((s) => s.token)
  const logout = useAuthStore((s) => s.logout)

  const query = useMe({
    query: {
      enabled: !!token,
      retry: false,
    },
  })

  useEffect(() => {
    if (query.data?.data && token) {
      useAuthStore.getState().setAuth(token, query.data.data)
    }
    if (query.isError) {
      logout()
    }
  }, [query.data, query.isError, token, logout])

  return query
}
