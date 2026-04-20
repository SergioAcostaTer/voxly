import {
  AlertCircle,
  BarChart3,
  CheckCircle,
  Gauge,
  Loader2,
  Mic2,
  Timer,
  TrendingUp,
  Video,
} from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AUTH_BYPASS_ENABLED } from '../auth/testing-auth'
import { useAuth } from '../auth/useAuth'
import { AppHeader } from '../components/AppHeader'
import { api, ApiClientError } from '../lib/api'
import type { ProgressSummary, SessionTrend } from '../types/progress'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'

export function ProgressPage() {
  const navigate = useNavigate()
  const { accessToken, logout } = useAuth()

  const [summary, setSummary] = useState<ProgressSummary | null>(null)
  const [trends, setTrends] = useState<SessionTrend[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchData = useCallback(async () => {
    if (!accessToken) return

    try {
      const [summaryData, trendsData] = await Promise.all([
        api.getProgressSummary(accessToken),
        api.getProgressTrends(accessToken, 10),
      ])
      setSummary(summaryData)
      setTrends(trendsData)
    } catch (err) {
      if (err instanceof ApiClientError && err.status === 401) {
        if (AUTH_BYPASS_ENABLED) {
          setError('We could not verify your access right now. Please try again.')
          return
        }
        await logout()
        navigate('/login', { replace: true })
        return
      }
      setError(err instanceof Error ? err.message : 'Failed to load progress')
    } finally {
      setIsLoading(false)
    }
  }, [accessToken, logout, navigate])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  function formatDate(dateString: string) {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
    })
  }

  function getScoreColor(score: number | null) {
    if (score === null) return 'text-muted-foreground'
    if (score >= 8) return 'text-green-600'
    if (score >= 6) return 'text-amber-600'
    return 'text-red-600'
  }

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="animate-spin text-primary" size={48} />
      </div>
    )
  }

  return (
    <div className="px-4 pb-6 pt-28 sm:px-6 sm:pb-8 lg:px-8 lg:pb-12">
      <div className="mx-auto w-full max-w-6xl space-y-6">
        <AppHeader />

        {error && (
          <Card className="border-red-200 bg-red-50">
            <div className="flex items-center gap-2 text-red-600">
              <AlertCircle size={18} />
              <p>{error}</p>
            </div>
          </Card>
        )}

        {/* Header */}
        <Card className="bg-gradient-to-br from-primary to-cyan-700 text-primary-foreground">
          <div className="flex items-center gap-3">
            <TrendingUp size={32} />
            <div>
              <h1 className="display-font text-2xl font-semibold sm:text-3xl">
                Your Progress
              </h1>
              <p className="mt-1 text-sm text-primary-foreground/80">
                Track your presentation skills over time
              </p>
            </div>
          </div>
        </Card>

        {/* Summary Stats */}
        {summary && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Card>
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
                  <Video className="text-primary" size={20} />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Total Sessions</p>
                  <p className="display-font text-2xl font-semibold text-foreground">
                    {summary.totalSessions}
                  </p>
                </div>
              </div>
            </Card>

            <Card>
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-green-100">
                  <CheckCircle className="text-green-600" size={20} />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Completed</p>
                  <p className="display-font text-2xl font-semibold text-foreground">
                    {summary.completedSessions}
                  </p>
                </div>
              </div>
            </Card>

            <Card>
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
                  <Gauge className="text-primary" size={20} />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Avg. Clarity</p>
                  <p className="display-font text-2xl font-semibold text-foreground">
                    {summary.averageClarityScore?.toFixed(1) ?? '--'}
                  </p>
                </div>
              </div>
            </Card>

            <Card>
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-100">
                  <Timer className="text-blue-600" size={20} />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Avg. WPM</p>
                  <p className="display-font text-2xl font-semibold text-foreground">
                    {summary.averageWordsPerMinute ?? '--'}
                  </p>
                </div>
              </div>
            </Card>
          </div>
        )}

        {/* Additional Stats */}
        {summary && (
          <div className="grid gap-4 sm:grid-cols-2">
            <Card>
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-100">
                  <Mic2 className="text-amber-600" size={20} />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Avg. Filler Words</p>
                  <p className="display-font text-2xl font-semibold text-foreground">
                    {summary.averageFillerWords ?? '--'}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {summary.averageFillerWords !== null && summary.averageFillerWords <= 5
                      ? 'Great job keeping fillers low!'
                      : 'Try to reduce um, uh, and like'}
                  </p>
                </div>
              </div>
            </Card>

            <Card>
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-purple-100">
                  <BarChart3 className="text-purple-600" size={20} />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Recent Sessions</p>
                  <p className="display-font text-2xl font-semibold text-foreground">
                    {summary.recentSessionCount}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    Keep practicing regularly!
                  </p>
                </div>
              </div>
            </Card>
          </div>
        )}

        {/* Trends */}
        {trends.length > 0 ? (
          <Card>
            <h2 className="display-font mb-4 text-lg font-semibold text-foreground">
              <TrendingUp className="mr-2 inline-block" size={20} />
              Recent Sessions
            </h2>

            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-border text-left text-sm text-muted-foreground">
                    <th className="pb-3 pr-4 font-medium">Session</th>
                    <th className="pb-3 pr-4 font-medium">Date</th>
                    <th className="pb-3 pr-4 font-medium text-center">Clarity</th>
                    <th className="pb-3 pr-4 font-medium text-center">WPM</th>
                    <th className="pb-3 font-medium text-center">Fillers</th>
                  </tr>
                </thead>
                <tbody className="text-sm">
                  {trends.map((trend) => (
                    <tr key={trend.sessionId} className="border-b border-border/50">
                      <td className="py-3 pr-4">
                        <Link
                          to={`/sessions/${trend.sessionId}`}
                          className="font-medium text-foreground hover:text-primary hover:underline"
                        >
                          {trend.sessionTitle}
                        </Link>
                      </td>
                      <td className="py-3 pr-4 text-muted-foreground">
                        {formatDate(trend.date)}
                      </td>
                      <td className={`py-3 pr-4 text-center font-semibold ${getScoreColor(trend.clarityScore)}`}>
                        {trend.clarityScore?.toFixed(1) ?? '--'}
                      </td>
                      <td className="py-3 pr-4 text-center text-foreground">
                        {trend.wordsPerMinute ?? '--'}
                      </td>
                      <td className="py-3 text-center text-foreground">
                        {trend.fillerWordCount ?? '--'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>
        ) : (
          <Card>
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <BarChart3 className="mb-4 text-muted-foreground/50" size={48} />
              <h2 className="display-font text-lg font-semibold text-foreground">
                No completed sessions yet
              </h2>
              <p className="mt-2 max-w-sm text-sm text-muted-foreground">
                Complete your first practice session to start tracking your progress.
              </p>
              <Link to="/sessions/new" className="mt-6">
                <Button>Start Practicing</Button>
              </Link>
            </div>
          </Card>
        )}

        {/* Tips Card */}
        <Card className="bg-gradient-to-r from-accent/10 to-amber-50">
          <h3 className="display-font mb-3 font-semibold text-foreground">
            Tips for Improvement
          </h3>
          <ul className="space-y-2 text-sm text-muted-foreground">
            <li>• Aim for a speaking pace of 120-150 words per minute for clarity</li>
            <li>• Practice pausing instead of using filler words</li>
            <li>• Record yourself regularly to track improvement</li>
            <li>• Review your feedback notes after each session</li>
          </ul>
        </Card>
      </div>
    </div>
  )
}
