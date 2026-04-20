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
import { AUTH_BYPASS_ENABLED, TESTING_ACCESS_TOKEN, TESTING_USER } from './testing-auth'

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<User | null>(null)
  const [accessToken, setAccessToken] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const loadProfileWithRefreshFallback = useCallback(async (token: string) => {
    try {
      const profile = await api.me(token)
      return { profile, accessToken: token }
    } catch (error) {
      if (!(error instanceof ApiClientError) || error.status !== 401) {
        throw error
      }

      const refreshed = await api.requestAccessToken()
      const profile = await api.me(refreshed.accessToken)
      return { profile, accessToken: refreshed.accessToken }
    }
  }, [])

  const syncAccessToken = useCallback(async () => {
    if (AUTH_BYPASS_ENABLED) {
      try {
        const profile = await api.me(TESTING_ACCESS_TOKEN)
        setAccessToken(TESTING_ACCESS_TOKEN)
        setUser(profile)
      } catch {
        setAccessToken(TESTING_ACCESS_TOKEN)
        setUser(TESTING_USER)
      }
      return
    }

    if (!accessToken) {
      setUser(null)
      return
    }

    try {
      const session = await loadProfileWithRefreshFallback(accessToken)
      setAccessToken(session.accessToken)
      setUser(session.profile)
    } catch {
      setUser(null)
      setAccessToken(null)
    }
  }, [accessToken, loadProfileWithRefreshFallback])

  const bootstrapSession = useCallback(async () => {
    if (AUTH_BYPASS_ENABLED) {
      try {
        const profile = await api.me(TESTING_ACCESS_TOKEN)
        setAccessToken(TESTING_ACCESS_TOKEN)
        setUser(profile)
      } catch {
        setAccessToken(TESTING_ACCESS_TOKEN)
        setUser(TESTING_USER)
      } finally {
        setIsLoading(false)
      }
      return
    }

    try {
      const refreshed = await api.requestAccessToken()
      setAccessToken(refreshed.accessToken)
      const session = await loadProfileWithRefreshFallback(refreshed.accessToken)
      setAccessToken(session.accessToken)
      setUser(session.profile)
    } catch {
      setAccessToken(null)
      setUser(null)
    } finally {
      setIsLoading(false)
    }
  }, [loadProfileWithRefreshFallback])

  useEffect(() => {
    void bootstrapSession()
  }, [bootstrapSession])

  const login = useCallback(async (payload: LoginPayload) => {
    if (AUTH_BYPASS_ENABLED) {
      try {
        const profile = await api.me(TESTING_ACCESS_TOKEN)
        setAccessToken(TESTING_ACCESS_TOKEN)
        setUser(profile)
      } catch {
        setAccessToken(TESTING_ACCESS_TOKEN)
        setUser(TESTING_USER)
      }
      return
    }

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
    if (AUTH_BYPASS_ENABLED) {
      setAccessToken(TESTING_ACCESS_TOKEN)
      setUser(TESTING_USER)
      return
    }

    await api.register(payload)
  }, [])

  const logout = useCallback(async () => {
    if (AUTH_BYPASS_ENABLED) {
      setAccessToken(TESTING_ACCESS_TOKEN)
      setUser(TESTING_USER)
      return
    }

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
      syncAccessToken,
    }),
    [accessToken, isLoading, login, logout, register, syncAccessToken, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
