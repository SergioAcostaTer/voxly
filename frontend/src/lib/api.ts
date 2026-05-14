import type {
    ApiResponse,
    LoginApiResponse,
    LoginPayload,
    RegisterPayload,
    User,
} from '../types/auth'
import type { Evaluation, FeedbackResponse } from '../types/evaluation'
import type { CategoryProgress, ProgressSummary, SessionComparison, SessionTrend } from '../types/progress'
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
type RecordingUploadResponse = {
  uploadId: string
  sizeBytes: number
  nextSequence: number
  completed: boolean
  modifiedAt: string
}

const RETRYABLE_STATUSES = new Set([429, 500, 502, 503, 504])
const RECORDING_UPLOAD_CHUNK_SIZE = 5 * 1024 * 1024

function normalizeMediaUploadFile(file: File): File {
  const extension = file.name.toLowerCase().split('.').pop() ?? ''
  const currentType = file.type.toLowerCase()

  let normalizedType = currentType

  if (extension === 'm4a') {
    normalizedType = 'audio/mp4'
  } else if (extension === 'mp3') {
    normalizedType = 'audio/mpeg'
  } else if (extension === 'wav') {
    normalizedType = 'audio/wav'
  } else if (extension === 'ogg') {
    normalizedType = 'audio/ogg'
  } else if (extension === 'webm') {
    normalizedType = currentType.startsWith('video/') ? 'video/webm' : 'audio/webm'
  } else if (extension === 'mp4' && (currentType === '' || currentType === 'application/octet-stream')) {
    normalizedType = 'video/mp4'
  }

  if (normalizedType === currentType || normalizedType === '') {
    return file
  }

  return new File([file], file.name, {
    type: normalizedType,
    lastModified: file.lastModified,
  })
}

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

function buildUploadFailureMessage(status: number, prefix = 'Upload failed') {
  if (status === 413) {
    return `${prefix}: file exceeds the server limit of 500MB.`
  }
  return `${prefix} with status ${status}`
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
  getOAuthLoginUrl(provider: 'google' | 'microsoft') {
    return `${API_BASE_URL}/oauth2/authorization/${provider}`
  },

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

  updateProfile(
    accessToken: string,
    payload: { username: string; professionalRole?: string; coachingFocus?: string },
  ) {
    return request<User>(`${USERS_BASE_PATH}/me`, {
      method: 'PATCH',
      accessToken,
      body: payload,
    })
  },

  changePassword(
    accessToken: string,
    payload: { currentPassword: string; newPassword: string },
  ) {
    return request<void>(`${USERS_BASE_PATH}/me/change-password`, {
      method: 'POST',
      accessToken,
      body: payload,
    })
  },

  enableTwoFactor(accessToken: string) {
    return request<void>(`${USERS_BASE_PATH}/me/enable-two-factor`, {
      method: 'POST',
      accessToken,
    })
  },

  disableTwoFactor(accessToken: string) {
    return request<void>(`${USERS_BASE_PATH}/me/disable-two-factor`, {
      method: 'POST',
      accessToken,
    })
  },

  requestPasswordReset(email: string) {
    return requestAuth<void>('/request-password-reset', {
      method: 'POST',
      body: { email },
    })
  },

  resetPassword(token: string, newPassword: string) {
    return requestAuth<void>('/reset-password', {
      method: 'POST',
      body: { token, newPassword },
    })
  },

  requestTwoFactorCode(identifier: string, password: string) {
    return requestAuth<void>('/request-two-factor-code', {
      method: 'POST',
      body: { identifier, password },
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
      const normalizedFile = normalizeMediaUploadFile(file)
      const formData = new FormData()
      formData.append('title', data.title)
      formData.append('sessionType', data.sessionType)
      formData.append('language', data.language)
      if (data.description) {
        formData.append('description', data.description)
      }
      formData.append('file', normalizedFile)

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
              firstError?.message ?? buildUploadFailureMessage(status),
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

  async createSessionWithChunkedMedia(
    accessToken: string,
    data: CreateSessionRequest,
    file: File,
    onProgress?: UploadProgressCallback,
  ) {
    const normalizedFile = normalizeMediaUploadFile(file)
    const initFormData = new FormData()
    initFormData.append('fileName', normalizedFile.name)
    initFormData.append('contentType', normalizedFile.type || 'application/octet-stream')

    const upload = await new Promise<RecordingUploadResponse>((resolve, reject) => {
      const xhr = new XMLHttpRequest()
      xhr.open('POST', `${API_BASE_URL}/v1/sessions/recording-uploads`)
      xhr.withCredentials = true
      xhr.setRequestHeader('Authorization', `Bearer ${accessToken}`)
      xhr.onerror = () => reject(new ApiClientError('Upload initialization failed due to a network error.', 0))
      xhr.onload = () => {
        const status = xhr.status
        const payload = (() => {
          try {
            return JSON.parse(xhr.responseText || 'null') as ApiResponse<RecordingUploadResponse> | null
          } catch {
            return null
          }
        })()
        const firstError = payload?.errors?.[0]
        if (status < 200 || status >= 300 || !payload?.success || !payload.data) {
          reject(new ApiClientError(
            firstError?.message ?? `Upload initialization failed with status ${status}`,
            status,
            firstError?.code,
          ))
          return
        }
        resolve(payload.data)
      }
      xhr.send(initFormData)
    })

    const cleanupUpload = async () => {
      try {
        await fetch(`${API_BASE_URL}/v1/sessions/recording-uploads/${upload.uploadId}`, {
          method: 'DELETE',
          credentials: 'include',
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        })
      } catch {
        // Best-effort cleanup only.
      }
    }

    try {
      let uploadedBytes = 0
      let sequence = 0

      for (let offset = 0; offset < normalizedFile.size; offset += RECORDING_UPLOAD_CHUNK_SIZE) {
        const chunk = normalizedFile.slice(offset, offset + RECORDING_UPLOAD_CHUNK_SIZE, normalizedFile.type)
        const chunkFormData = new FormData()
        chunkFormData.append('sequence', String(sequence))
        chunkFormData.append('isLastChunk', String(offset + RECORDING_UPLOAD_CHUNK_SIZE >= normalizedFile.size))
        chunkFormData.append('chunk', chunk, `${normalizedFile.name}.part-${sequence}`)

        await new Promise<void>((resolve, reject) => {
          const xhr = new XMLHttpRequest()
          xhr.open('POST', `${API_BASE_URL}/v1/sessions/recording-uploads/${upload.uploadId}/chunks`)
          xhr.withCredentials = true
          xhr.setRequestHeader('Authorization', `Bearer ${accessToken}`)
          xhr.upload.onprogress = (event) => {
            if (!event.lengthComputable || !onProgress) return
            const currentUploadedBytes = uploadedBytes + event.loaded
            onProgress(Math.max(1, Math.min(99, Math.round((currentUploadedBytes / normalizedFile.size) * 100))))
          }
          xhr.onerror = () => reject(new ApiClientError('Chunk upload failed due to a network error.', 0))
          xhr.onload = () => {
            const status = xhr.status
            const payload = (() => {
              try {
                return JSON.parse(xhr.responseText || 'null') as ApiResponse<RecordingUploadResponse> | null
              } catch {
                return null
              }
            })()
            const firstError = payload?.errors?.[0]
            if (status < 200 || status >= 300 || !payload?.success) {
              reject(new ApiClientError(
                firstError?.message ?? buildUploadFailureMessage(status, 'Chunk upload failed'),
                status,
                firstError?.code,
              ))
              return
            }
            resolve()
          }
          xhr.send(chunkFormData)
        })

        uploadedBytes += chunk.size
        sequence += 1
        onProgress?.(Math.max(1, Math.min(99, Math.round((uploadedBytes / normalizedFile.size) * 100))))
      }

      const finalizeFormData = new FormData()
      finalizeFormData.append('title', data.title)
      finalizeFormData.append('sessionType', data.sessionType)
      finalizeFormData.append('language', data.language)
      if (data.description) {
        finalizeFormData.append('description', data.description)
      }
      finalizeFormData.append('uploadId', upload.uploadId)

      const session = await new Promise<Session>((resolve, reject) => {
        const xhr = new XMLHttpRequest()
        xhr.open('POST', `${API_BASE_URL}/v1/sessions/with-recording-upload`)
        xhr.withCredentials = true
        xhr.setRequestHeader('Authorization', `Bearer ${accessToken}`)
        xhr.onerror = () => reject(new ApiClientError('Session creation failed due to a network error.', 0))
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
              firstError?.message ?? `Session creation failed with status ${status}`,
              status,
              firstError?.code,
            ))
            return
          }
          resolve(payload.data as Session)
        }
        xhr.send(finalizeFormData)
      })

      onProgress?.(100)
      return session
    } catch (error) {
      await cleanupUpload()
      throw error
    }
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
      const normalizedFile = normalizeMediaUploadFile(file)
      const formData = new FormData()
      formData.append('file', normalizedFile)

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
              firstError?.message ?? buildUploadFailureMessage(status),
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

  uploadSessionSlides(
    accessToken: string,
    sessionId: string,
    file: File,
    onProgress?: UploadProgressCallback,
  ) {
    return new Promise<Session>((resolve, reject) => {
      const formData = new FormData()
      formData.append('file', file)

      const xhr = new XMLHttpRequest()
      xhr.open('POST', `${API_BASE_URL}/v1/sessions/${sessionId}/slides`)
      xhr.withCredentials = true
      xhr.setRequestHeader('Authorization', `Bearer ${accessToken}`)

      xhr.upload.onprogress = (event) => {
        if (!event.lengthComputable || !onProgress) return
        const percent = Math.round((event.loaded / event.total) * 100)
        onProgress(percent)
      }

      xhr.onerror = () => {
        reject(new ApiClientError('Slide upload failed due to a network error.', 0))
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
              firstError?.message ?? buildUploadFailureMessage(status, 'Slide upload failed'),
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

  reanalyzePosture(accessToken: string, sessionId: string) {
    return request<Evaluation>(`/v1/evaluations/session/${sessionId}/posture/analyze`, {
      accessToken,
      method: 'POST',
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
              firstError?.message ?? buildUploadFailureMessage(status, 'Transcription request failed'),
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

  getProgressByCategory(accessToken: string) {
    return request<CategoryProgress[]>('/v1/progress/categories', {
      accessToken,
    })
  },

  compareSessions(accessToken: string, firstSessionId: string, secondSessionId: string) {
    return request<SessionComparison>(
      `/v1/progress/compare?firstSessionId=${firstSessionId}&secondSessionId=${secondSessionId}`,
      {
        accessToken,
      },
    )
  },

  async exportProgressSummary(accessToken: string) {
    const response = await fetch(`${API_BASE_URL}/v1/progress/export`, {
      method: 'GET',
      credentials: 'include',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        Accept: 'text/csv',
      },
    })

    if (!response.ok) {
      throw new ApiClientError(`Export failed with status ${response.status}`, response.status)
    }

    return response.blob()
  },
}
