export type SessionType = 'presentation' | 'interview' | 'pitch' | 'freestyle'
export type SessionStatus = 'draft' | 'uploaded' | 'analyzing' | 'completed' | 'failed'
export type SupportedLanguage = 'en' | 'es'

export type MediaFile = {
  storagePath: string
  originalFileName: string
  contentType: string
  sizeBytes: number
  durationSeconds: number | null
  url: string
}

export type Session = {
  id: string
  userId: string
  title: string
  description: string | null
  sessionType: string
  status: SessionStatus
  mediaFile: MediaFile | null
  evaluationId: string | null
  language: SupportedLanguage
  createdAt: string
  modifiedAt: string
}

export type CreateSessionRequest = {
  title: string
  sessionType: string
  description?: string
  language: SupportedLanguage
}

export type SessionListResponse = {
  sessions: Session[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
