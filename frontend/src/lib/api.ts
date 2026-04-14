import type {
    ApiResponse,
    LoginApiResponse,
    LoginPayload,
    RegisterPayload,
    User,
} from '../types/auth'
import type { Evaluation, FeedbackResponse } from '../types/evaluation'
import type { ProgressSummary, SessionTrend } from '../types/progress'
import type { CreateSessionRequest, Session, SessionListResponse } from '../types/sessions'

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
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
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

  // Sessions
  async getSessions(accessToken: string, page = 1, size = 10) {
    const response = await request<SessionListResponse>(`/v1/sessions?page=${page}&size=${size}`, {
      accessToken,
    })
    return response
  },

  getSession(accessToken: string, sessionId: string) {
    return request<Session>(`/v1/sessions/${sessionId}`, {
      accessToken,
    })
  },

  createSession(accessToken: string, data: CreateSessionRequest) {
    return request<Session>('/v1/sessions', {
      method: 'POST',
      accessToken,
      body: data,
    })
  },

  deleteSession(accessToken: string, sessionId: string) {
    return request<void>(`/v1/sessions/${sessionId}`, {
      method: 'DELETE',
      accessToken,
    })
  },

  async uploadSessionMedia(accessToken: string, sessionId: string, file: File) {
    const formData = new FormData()
    formData.append('file', file)

    const response = await fetch(`${API_BASE_URL}/v1/sessions/${sessionId}/media`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
      body: formData,
    })

    const payload = (await response.json().catch(() => null)) as ApiResponse<Session> | null

    if (!response.ok || !payload?.success) {
      const firstError = payload?.errors?.[0]
      throw new ApiClientError(
        firstError?.message ?? `Upload failed with status ${response.status}`,
        response.status,
        firstError?.code,
      )
    }

    return payload.data as Session
  },

  requestAnalysis(accessToken: string, sessionId: string) {
    return request<void>(`/v1/sessions/${sessionId}/analyze`, {
      method: 'POST',
      accessToken,
    })
  },

  // Evaluation
  getEvaluation(accessToken: string, sessionId: string) {
    return request<Evaluation>(`/v1/evaluations/session/${sessionId}`, {
      accessToken,
    })
  },

  // Feedback
  getFeedback(accessToken: string, sessionId: string) {
    return request<FeedbackResponse>(`/v1/feedback/session/${sessionId}`, {
      accessToken,
    })
  },

  // Progress
  getProgressSummary(accessToken: string) {
    return request<ProgressSummary>('/v1/progress/summary', {
      accessToken,
    })
  },

  getProgressTrends(accessToken: string, limit = 10) {
    return request<SessionTrend[]>(`/v1/progress/trends?limit=${limit}`, {
      accessToken,
    })
  },
}
