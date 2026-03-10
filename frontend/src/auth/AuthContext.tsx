import {
    useCallback,
    useEffect,
    useMemo,
    useState,
    type PropsWithChildren,
} from 'react'
import { ApiClientError, api } from '../lib/api'
import type { LoginPayload, RegisterPayload, User } from '../types/auth'
import { AuthContext, type AuthContextValue } from './auth-context'

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<User | null>(null)
  const [accessToken, setAccessToken] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const refreshMe = useCallback(async () => {
    if (!accessToken) {
      setUser(null)
      return
    }

    try {
      const profile = await api.me(accessToken)
      setUser(profile)
    } catch {
      setUser(null)
      setAccessToken(null)
    }
  }, [accessToken])

  const bootstrapSession = useCallback(async () => {
    try {
      const refreshed = await api.refreshToken()
      setAccessToken(refreshed.accessToken)
      const profile = await api.me(refreshed.accessToken)
      setUser(profile)
    } catch {
      setAccessToken(null)
      setUser(null)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    void bootstrapSession()
  }, [bootstrapSession])

  const login = useCallback(async (payload: LoginPayload) => {
    const result = await api.login(payload)

    if (result.requiresTwoFactor) {
      throw new ApiClientError(
        'Two-factor authentication is required. Please provide your 2FA code.',
        400,
        'AUTH.TWO_FACTOR_REQUIRED',
      )
    }

    if (!result.tokens?.accessToken) {
      throw new ApiClientError('Missing access token in login response.', 500)
    }

    setAccessToken(result.tokens.accessToken)

    const profile = await api.me(result.tokens.accessToken)
    setUser(profile)
  }, [])

  const register = useCallback(async (payload: RegisterPayload) => {
    await api.register(payload)
  }, [])

  const logout = useCallback(async () => {
    try {
      await api.logout()
    } finally {
      setAccessToken(null)
      setUser(null)
    }
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      accessToken,
      isLoading,
      isAuthenticated: Boolean(user && accessToken),
      login,
      register,
      logout,
      refreshMe,
    }),
    [accessToken, isLoading, login, logout, refreshMe, register, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
