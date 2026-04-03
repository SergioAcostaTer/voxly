export type SessionType = 'PRESENTATION' | 'INTERVIEW' | 'PITCH' | 'FREESTYLE'
export type SessionStatus = 'DRAFT' | 'UPLOADED' | 'ANALYZING' | 'COMPLETED' | 'FAILED'

export type Session = {
  id: string
  title: string
  type: SessionType
  status: SessionStatus
  mediaPath: string | null
  contentType: string | null
  durationSeconds: number | null
  evaluationId: string | null
  createdAt: string
  updatedAt: string
}

export type CreateSessionRequest = {
  title: string
  type: SessionType
}

export type SessionListResponse = {
  items: Session[]
  pageNumber: number
  pageSize: number
  totalItems: number
  totalPages: number
  hasNextPage: boolean
  hasPreviousPage: boolean
}
