export type ApiError = {
  code: string
  message: string
}

export type ApiResponse<T> = {
  success: boolean
  data: T | null
  errors: ApiError[] | null
  timestamp: string
}

export type User = {
  id: string
  email: string
  username: string
  active: boolean
  emailVerified: boolean
  twoFactorEnabled: boolean
  roles: string[]
  createdAt: string
  modifiedAt: string
}

export type AuthTokens = {
  accessToken: string
  accessTokenExpiresAt: string
}

export type LoginApiResponse = {
  user: User | null
  tokens: AuthTokens | null
  requiresTwoFactor: boolean
}

export type RegisterPayload = {
  email: string
  username: string
  password: string
}

export type LoginPayload = {
  identifier: string
  password: string
  twoFactorCode?: string
}
