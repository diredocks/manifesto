import { create } from 'zustand'
import type { UserInfoResponse } from '@/api/generated/model'

interface AuthState {
  token: string | null
  user: UserInfoResponse | null
  isAuthenticated: boolean
  setAuth: (token: string, user: UserInfoResponse) => void
  logout: () => void
}

const storedToken = localStorage.getItem('token')

export const useAuthStore = create<AuthState>((set) => ({
  token: storedToken,
  user: null,
  isAuthenticated: !!storedToken,
  setAuth: (token, user) => {
    localStorage.setItem('token', token)
    set({ token, user, isAuthenticated: true })
  },
  logout: () => {
    localStorage.removeItem('token')
    set({ token: null, user: null, isAuthenticated: false })
  },
}))
