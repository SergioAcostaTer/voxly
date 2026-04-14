export type EvaluationStatus = 'pending' | 'transcribing' | 'analyzing' | 'completed' | 'failed'

export type TranscriptionData = {
  fullText: string
  durationSeconds: number | null
  detectedLanguage: string | null
}

export type EvaluationMetrics = {
  wordsPerMinute: number | null
  totalWords: number | null
  fillerWordCount: number | null
  pauseCount: number | null
  clarityScore: number | null
}

export type FeedbackData = {
  overallSummary: string | null
  notesJson: string | null
}

export type Evaluation = {
  id: string
  sessionId: string
  status: EvaluationStatus
  sessionType: string | null
  transcription: TranscriptionData | null
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
