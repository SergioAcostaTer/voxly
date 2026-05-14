export type ProgressSummary = {
  totalSessions: number
  completedSessions: number
  averageClarityScore: number | null
  averageWordsPerMinute: number | null
  averageFillerWords: number | null
  recentSessionCount: number
  averagePostureScore: number | null
  completedVideoAnalyses: number
  processingSessions: number
  failedSessions: number
  aiVideoModuleEnabled: boolean
}

export type SessionTrend = {
  sessionId: string
  sessionTitle: string
  date: string
  clarityScore: number | null
  wordsPerMinute: number | null
  fillerWordCount: number | null
}

export type CategoryProgress = {
  key: string
  label: string
  averageValue: number | null
  targetValue: number | null
  higherIsBetter: boolean
  status: 'on-track' | 'needs-work' | 'no-data'
}

export type SessionComparisonSnapshot = {
  sessionId: string
  title: string
  date: string
}

export type MetricComparison = {
  label: string
  firstValue: number | null
  secondValue: number | null
  delta: number | null
  winner: 'first' | 'second' | 'tie'
}

export type SessionComparison = {
  firstSession: SessionComparisonSnapshot
  secondSession: SessionComparisonSnapshot
  metrics: MetricComparison[]
}
