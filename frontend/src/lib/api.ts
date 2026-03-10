import type {
    ApiResponse,
    LoginApiResponse,
    LoginPayload,
    RegisterPayload,
    User,
} from '../types/auth'

export class ApiClientError extends Error {
  public readonly status: number
  public readonly code?: string

  constructor(
    message: string,
    status: number,
    code?: string,
  ) {
    super(message)
    this.status = status
    this.code = code
  }
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''
const AUTH_BASE_CANDIDATES = [
  import.meta.env.VITE_AUTH_BASE_PATH,
  '/v1/auth',
].filter(Boolean) as string[]

const USERS_BASE_PATH = import.meta.env.VITE_USERS_BASE_PATH ?? '/v1/users'

type RequestOptions = {
  method?: 'GET' | 'POST'
  body?: unknown
  accessToken?: string | null
}

async function request<T>(path: string, options: RequestOptions = {}) {
  const headers = new Headers({
    'Content-Type': 'application/json',
  })

  if (options.accessToken) {
    headers.set('Authorization', `Bearer ${options.accessToken}`)
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: options.method ?? 'GET',
    credentials: 'include',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  })

  const payload = (await response.json().catch(() => null)) as ApiResponse<T> | null
  const firstError = payload?.errors?.[0]

  if (!response.ok || !payload?.success) {
    throw new ApiClientError(
      firstError?.message ?? `Request failed with status ${response.status}`,
      response.status,
      firstError?.code,
    )
  }

  return payload.data as T
}

async function requestAuth<T>(suffixPath: string, options: RequestOptions = {}) {
  let lastError: unknown = null

  for (const authBasePath of AUTH_BASE_CANDIDATES) {
    try {
      return await request<T>(`${authBasePath}${suffixPath}`, options)
    } catch (error) {
      lastError = error
      if (error instanceof ApiClientError && error.status !== 404) {
        throw error
      }
    }
  }

  throw lastError instanceof Error
    ? lastError
    : new Error('Unable to reach authentication endpoint.')
}

export const api = {
  register(payload: RegisterPayload) {
    return requestAuth<User>('/register', {
      method: 'POST',
      body: payload,
    })
  },

  login(payload: LoginPayload) {
    return requestAuth<LoginApiResponse>('/login', {
      method: 'POST',
      body: payload,
    })
  },

  requestAccessToken() {
    return requestAuth<{ accessToken: string; accessTokenExpiresAt: string }>(
      '/refresh-token',
      {
        method: 'POST',
      },
    )
  },

  logout() {
    return requestAuth<void>('/logout', {
      method: 'POST',
    })
  },

  me(accessToken: string) {
    return request<User>(`${USERS_BASE_PATH}/me`, {
      accessToken,
    })
  },
}
