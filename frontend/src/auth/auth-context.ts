import { createContext } from 'react'
import type { LoginPayload, RegisterPayload, User } from '../types/auth'

export type AuthContextValue = {
  user: User | null
  accessToken: string | null
  isLoading: boolean
  isAuthenticated: boolean
  login: (payload: LoginPayload) => Promise<void>
  register: (payload: RegisterPayload) => Promise<void>
  logout: () => Promise<void>
  syncAccessToken: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
