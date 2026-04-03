export type ProgressSummary = {
  totalSessions: number
  completedSessions: number
  averageClarityScore: number | null
  averageWordsPerMinute: number | null
  averageFillerWords: number | null
  recentSessionCount: number
}

export type SessionTrend = {
  sessionId: string
  sessionTitle: string
  date: string
  clarityScore: number | null
  wordsPerMinute: number | null
  fillerWordCount: number | null
}
