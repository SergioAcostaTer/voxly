export type EvaluationStatus = 'PENDING' | 'TRANSCRIBING' | 'ANALYZING' | 'COMPLETED' | 'FAILED'

export type EvaluationMetrics = {
  clarityScore: number | null
  wordsPerMinute: number | null
  fillerWordCount: number | null
  pauseCount: number | null
  averagePauseDuration: number | null
}

export type FeedbackData = {
  notesJson: string | null
  overallSummary: string | null
}

export type Evaluation = {
  id: string
  sessionId: string
  status: EvaluationStatus
  transcription: string | null
  metrics: EvaluationMetrics | null
  feedback: FeedbackData | null
  errorMessage: string | null
  createdAt: string
  completedAt: string | null
}

export type FeedbackNote = {
  category: string
  severity: string
  message: string
  timestampSeconds: number | null
  endTimestampSeconds: number | null
}

export type FeedbackResponse = {
  sessionId: string
  evaluationId: string
  notes: FeedbackNote[]
  overallSummary: string | null
  strengths: string[]
  areasForImprovement: string[]
}
