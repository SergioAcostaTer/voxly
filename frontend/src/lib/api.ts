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

type UploadProgressCallback = (percent: number) => void
type SessionEventCallback = (event: { event: string; data: unknown }) => void

const RETRYABLE_STATUSES = new Set([429, 500, 502, 503, 504])

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function safeParseJson<T>(response: Response): Promise<ApiResponse<T> | null> {
  try {
    return (await response.json()) as ApiResponse<T>
  } catch {
    return null
  }
}

async function request<T>(path: string, options: RequestOptions = {}, attempt = 0): Promise<T> {
  const headers = new Headers({
    'Content-Type': 'application/json',
  })

  if (options.accessToken) {
    headers.set('Authorization', `Bearer ${options.accessToken}`)
  }

  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      method: options.method ?? 'GET',
      credentials: 'include',
      headers,
      body: options.body ? JSON.stringify(options.body) : undefined,
    })

    const payload = await safeParseJson<T>(response)
    const firstError = payload?.errors?.[0]

    if (!response.ok || !payload?.success) {
      const status = response.status
      const error = new ApiClientError(
        firstError?.message ?? `Request failed with status ${status}`,
        status,
        firstError?.code,
      )

      if (attempt < 2 && RETRYABLE_STATUSES.has(status) && options.method !== 'POST') {
        await sleep(300 * (attempt + 1))
        return request<T>(path, options, attempt + 1)
      }

      throw error
    }

    return payload.data as T
  } catch (error) {
    if (
      attempt < 2 &&
      options.method !== 'POST' &&
      (!(error instanceof ApiClientError) || error.status === 0)
    ) {
      await sleep(300 * (attempt + 1))
      return request<T>(path, options, attempt + 1)
    }

    throw error
  }
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

  createSessionWithMedia(
    accessToken: string,
    data: CreateSessionRequest,
    file: File,
    onProgress?: UploadProgressCallback,
  ) {
    return new Promise<Session>((resolve, reject) => {
      const formData = new FormData()
      formData.append('title', data.title)
      formData.append('sessionType', data.sessionType)
      formData.append('language', data.language)
      if (data.description) {
        formData.append('description', data.description)
      }
      formData.append('file', file)

      const xhr = new XMLHttpRequest()
      xhr.open('POST', `${API_BASE_URL}/v1/sessions/with-media`)
      xhr.withCredentials = true
      xhr.setRequestHeader('Authorization', `Bearer ${accessToken}`)

      xhr.upload.onprogress = (event) => {
        if (!event.lengthComputable || !onProgress) return
        const percent = Math.round((event.loaded / event.total) * 100)
        onProgress(percent)
      }

      xhr.onerror = () => {
        reject(new ApiClientError('Upload failed due to a network error.', 0))
      }

      xhr.onload = () => {
        const status = xhr.status
        const payload = (() => {
          try {
            return JSON.parse(xhr.responseText || 'null') as ApiResponse<Session> | null
          } catch {
            return null
          }
        })()
        const firstError = payload?.errors?.[0]

        if (status < 200 || status >= 300 || !payload?.success) {
          reject(
            new ApiClientError(
              firstError?.message ?? `Upload failed with status ${status}`,
              status,
              firstError?.code,
            ),
          )
          return
        }

        resolve(payload.data as Session)
      }

      xhr.send(formData)
    })
  },

  createRecordingUpload(accessToken: string, fileName: string, contentType: string) {
    const params = new URLSearchParams({ fileName, contentType })
    return request<{ uploadId: string; sizeBytes: number; nextSequence: number; completed: boolean; modifiedAt: string }>(
      `/v1/sessions/recording-uploads?${params.toString()}`,
      {
        method: 'POST',
        accessToken,
      },
    )
  },

  appendRecordingChunk(
    accessToken: string,
    uploadId: string,
    sequence: number,
    chunk: Blob,
    isLastChunk = false,
  ) {
    return new Promise<{ uploadId: string; sizeBytes: number; nextSequence: number; completed: boolean; modifiedAt: string }>((resolve, reject) => {
      const formData = new FormData()
      formData.append('chunk', chunk, `chunk-${sequence}.webm`)

      const xhr = new XMLHttpRequest()
      xhr.open('POST', `${API_BASE_URL}/v1/sessions/recording-uploads/${uploadId}/chunks?sequence=${sequence}&isLastChunk=${isLastChunk}`)
      xhr.withCredentials = true
      xhr.setRequestHeader('Authorization', `Bearer ${accessToken}`)

      xhr.onerror = () => reject(new ApiClientError('Chunk upload failed due to a network error.', 0))
      xhr.onload = () => {
        const status = xhr.status
        const payload = (() => {
          try {
            return JSON.parse(xhr.responseText || 'null') as ApiResponse<{ uploadId: string; sizeBytes: number; nextSequence: number; completed: boolean; modifiedAt: string }> | null
          } catch {
            return null
          }
        })()
        const firstError = payload?.errors?.[0]

        if (status < 200 || status >= 300 || !payload?.success) {
          reject(new ApiClientError(
            firstError?.message ?? `Chunk upload failed with status ${status}`,
            status,
            firstError?.code,
          ))
          return
        }

        resolve(payload.data as { uploadId: string; sizeBytes: number; nextSequence: number; completed: boolean; modifiedAt: string })
      }

      xhr.send(formData)
    })
  },

  createSessionWithRecordingUpload(
    accessToken: string,
    data: CreateSessionRequest,
    uploadId: string,
  ) {
    const formData = new FormData()
    formData.append('title', data.title)
    formData.append('sessionType', data.sessionType)
    formData.append('language', data.language)
    if (data.description) {
      formData.append('description', data.description)
    }
    formData.append('uploadId', uploadId)

    return new Promise<Session>((resolve, reject) => {
      const xhr = new XMLHttpRequest()
      xhr.open('POST', `${API_BASE_URL}/v1/sessions/with-recording-upload`)
      xhr.withCredentials = true
      xhr.setRequestHeader('Authorization', `Bearer ${accessToken}`)

      xhr.onerror = () => reject(new ApiClientError('Recording upload finalization failed due to a network error.', 0))
      xhr.onload = () => {
        const status = xhr.status
        const payload = (() => {
          try {
            return JSON.parse(xhr.responseText || 'null') as ApiResponse<Session> | null
          } catch {
            return null
          }
        })()
        const firstError = payload?.errors?.[0]

        if (status < 200 || status >= 300 || !payload?.success) {
          reject(new ApiClientError(
            firstError?.message ?? `Recording finalization failed with status ${status}`,
            status,
            firstError?.code,
          ))
          return
        }

        resolve(payload.data as Session)
      }

      xhr.send(formData)
    })
  },

  deleteRecordingUpload(accessToken: string, uploadId: string) {
    return request<void>(`/v1/sessions/recording-uploads/${uploadId}`, {
      method: 'DELETE',
      accessToken,
    })
  },

  deleteSession(accessToken: string, sessionId: string) {
    return request<void>(`/v1/sessions/${sessionId}`, {
      method: 'DELETE',
      accessToken,
    })
  },

  uploadSessionMedia(
    accessToken: string,
    sessionId: string,
    file: File,
    onProgress?: UploadProgressCallback,
  ) {
    return new Promise<Session>((resolve, reject) => {
      const formData = new FormData()
      formData.append('file', file)

      const xhr = new XMLHttpRequest()
      xhr.open('POST', `${API_BASE_URL}/v1/sessions/${sessionId}/media`)
      xhr.withCredentials = true
      xhr.setRequestHeader('Authorization', `Bearer ${accessToken}`)

      xhr.upload.onprogress = (event) => {
        if (!event.lengthComputable || !onProgress) return
        const percent = Math.round((event.loaded / event.total) * 100)
        onProgress(percent)
      }

      xhr.onerror = () => {
        reject(new ApiClientError('Upload failed due to a network error.', 0))
      }

      xhr.onload = () => {
        const status = xhr.status
        const payload = (() => {
          try {
            return JSON.parse(xhr.responseText || 'null') as ApiResponse<Session> | null
          } catch {
            return null
          }
        })()
        const firstError = payload?.errors?.[0]

        if (status < 200 || status >= 300 || !payload?.success) {
          reject(
            new ApiClientError(
              firstError?.message ?? `Upload failed with status ${status}`,
              status,
              firstError?.code,
            ),
          )
          return
        }

        resolve(payload.data as Session)
      }

      xhr.send(formData)
    })
  },

  requestAnalysis(accessToken: string, sessionId: string) {
    return request<void>(`/v1/sessions/${sessionId}/analyze`, {
      method: 'POST',
      accessToken,
    })
  },

  async streamSessionEvents(
    accessToken: string,
    sessionId: string,
    onEvent: SessionEventCallback,
    signal?: AbortSignal,
  ) {
    const response = await fetch(`${API_BASE_URL}/v1/sessions/${sessionId}/events`, {
      method: 'GET',
      credentials: 'include',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        Accept: 'text/event-stream',
      },
      signal,
    })

    if (!response.ok || !response.body) {
      throw new ApiClientError(`Session stream failed with status ${response.status}`, response.status)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let currentEvent = 'message'
    let dataLines: string[] = []

    const flushEvent = () => {
      if (dataLines.length === 0) {
        currentEvent = 'message'
        return
      }

      const rawData = dataLines.join('\n')
      let parsedData: unknown = rawData
      try {
        parsedData = JSON.parse(rawData)
      } catch {
        parsedData = rawData
      }

      onEvent({ event: currentEvent, data: parsedData })
      currentEvent = 'message'
      dataLines = []
    }

    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        flushEvent()
        break
      }

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split(/\r?\n/)
      buffer = lines.pop() ?? ''

      for (const line of lines) {
        if (line === '') {
          flushEvent()
          continue
        }
        if (line.startsWith(':')) {
          continue
        }
        if (line.startsWith('event:')) {
          currentEvent = line.slice(6).trim() || 'message'
          continue
        }
        if (line.startsWith('data:')) {
          dataLines.push(line.slice(5).trim())
        }
      }
    }
  },

  // Evaluation
  getEvaluation(accessToken: string, sessionId: string) {
    return request<Evaluation>(`/v1/evaluations/session/${sessionId}`, {
      accessToken,
    })
  },

  requestTranscription(accessToken: string, sessionId: string, file: File) {
    return new Promise<{
      id: string
      status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
      originalText: string | null
      durationSeconds: number | null
      wordCount: number | null
      language: string
      errorMessage: string | null
    }>((resolve, reject) => {
      const formData = new FormData()
      formData.append('file', file)

      const xhr = new XMLHttpRequest()
      xhr.open('POST', `${API_BASE_URL}/v1/evaluations/${sessionId}/transcribe`)
      xhr.withCredentials = true
      xhr.setRequestHeader('Authorization', `Bearer ${accessToken}`)

      xhr.onerror = () => {
        reject(new ApiClientError('Transcription request failed due to a network error.', 0))
      }

      xhr.onload = () => {
        const status = xhr.status
        let payload: ApiResponse<{
          id: string
          status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
          originalText: string | null
          durationSeconds: number | null
          wordCount: number | null
          language: string
          errorMessage: string | null
        }> | null = null

        try {
          payload = JSON.parse(xhr.responseText || 'null') as ApiResponse<{
            id: string
            status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
            originalText: string | null
            durationSeconds: number | null
            wordCount: number | null
            language: string
            errorMessage: string | null
          }> | null
        } catch {
          payload = null
        }
        const firstError = payload?.errors?.[0]

        if (status < 200 || status >= 300 || !payload?.success) {
          reject(
            new ApiClientError(
              firstError?.message ?? `Transcription request failed with status ${status}`,
              status,
              firstError?.code,
            ),
          )
          return
        }

        resolve(payload.data as {
          id: string
          status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
          originalText: string | null
          durationSeconds: number | null
          wordCount: number | null
          language: string
          errorMessage: string | null
        })
      }

      xhr.send(formData)
    })
  },

  getTranscription(accessToken: string, sessionId: string) {
    return request<{
      id: string
      status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
      originalText: string | null
      durationSeconds: number | null
      wordCount: number | null
      language: string
      errorMessage: string | null
    }>(`/v1/evaluations/${sessionId}/transcription`, {
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
