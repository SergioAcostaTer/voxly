export type EvaluationStatus = 'pending' | 'transcribing' | 'analyzing' | 'completed' | 'failed'

export type SegmentData = {
  text: string
  startSeconds: number
  endSeconds: number
}

export type WordData = {
  word: string
  startSeconds: number
  endSeconds: number
  confidence: number
}

export type TranscriptionData = {
  fullText: string
  segments: SegmentData[]
  words: WordData[]
  durationSeconds: number | null
  detectedLanguage: string | null
  rawJson: string | null
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
  strengthsJson: string | null
  areasForImprovementJson: string | null
}

export type PostureGestureSummary = {
  name: string
  total_occurrences: number
  total_seconds: number
  points_deducted: number
  first_seen_sec: number
  last_seen_sec: number
  severity: string
  description?: string | null
  timestamps?: number[]
  first_seen_fmt?: string
  last_seen_fmt?: string
  timestamps_fmt?: string[]
}

export type PostureTimelineEvent = {
  sec: number
  gesture: string
  severity: string
  penalty: number
  person_id: number
  time_fmt?: string
}

export type PostureData = {
  score: number
  grade: string
  gestureSummariesJson: string | null
  timelineJson: string | null
  penaltyBreakdownJson: string | null
  recommendationsJson: string | null
  renderedVideoUrl: string | null
}

export type Evaluation = {
  id: string
  sessionId: string
  status: EvaluationStatus
  sessionType: string | null
  transcription: TranscriptionData | null
  metrics: EvaluationMetrics | null
  feedback: FeedbackData | null
  posture: PostureData | null
  errorMessage: string | null
  createdAt: string
  completedAt: string | null
  overallScore: number | null
}

export type FeedbackNote = {
  category: string
  severity: string
  title?: string | null
  coachScript?: string | null
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
