import {
  AlertCircle,
  BarChart3,
  CheckCircle,
  Download,
  Gauge,
  Loader2,
  Mic2,
  Scale,
  SlidersHorizontal,
  Timer,
  TrendingUp,
  Video,
} from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AUTH_BYPASS_ENABLED } from '../auth/testing-auth'
import { useAuth } from '../auth/useAuth'
import { AppHeader } from '../components/AppHeader'
import { api, ApiClientError } from '../lib/api'
import type { CategoryProgress, ProgressSummary, SessionComparison, SessionTrend } from '../types/progress'
import type { Session } from '../types/sessions'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { Input } from '../ui/Input'
import { Select } from '../ui/Select'

type GoalTargets = Record<string, number>

const GOALS_STORAGE_KEY = 'voxly-progress-goals'

export function ProgressPage() {
  const navigate = useNavigate()
  const { accessToken, logout } = useAuth()

  const [summary, setSummary] = useState<ProgressSummary | null>(null)
  const [trends, setTrends] = useState<SessionTrend[]>([])
  const [categories, setCategories] = useState<CategoryProgress[]>([])
  const [completedSessions, setCompletedSessions] = useState<Session[]>([])
  const [firstCompareId, setFirstCompareId] = useState('')
  const [secondCompareId, setSecondCompareId] = useState('')
  const [comparison, setComparison] = useState<SessionComparison | null>(null)
  const [isComparing, setIsComparing] = useState(false)
  const [isExporting, setIsExporting] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [goalTargets, setGoalTargets] = useState<GoalTargets>(() => {
    try {
      return JSON.parse(window.localStorage.getItem(GOALS_STORAGE_KEY) ?? '{}') as GoalTargets
    } catch {
      return {}
    }
  })

  const fetchData = useCallback(async () => {
    if (!accessToken) return

    try {
      const [summaryData, trendsData, categoryData, sessionsResponse] = await Promise.all([
        api.getProgressSummary(accessToken),
        api.getProgressTrends(accessToken, 10),
        api.getProgressByCategory(accessToken),
        api.getSessions(accessToken, 1, 50),
      ])

      const eligibleSessions = sessionsResponse.sessions.filter((session) => session.status === 'completed')

      setSummary(summaryData)
      setTrends(trendsData)
      setCategories(categoryData)
      setCompletedSessions(eligibleSessions)

      if (!firstCompareId && eligibleSessions[0]) {
        setFirstCompareId(eligibleSessions[0].id)
      }
      if (!secondCompareId && eligibleSessions[1]) {
        setSecondCompareId(eligibleSessions[1].id)
      }
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
  }, [accessToken, logout, navigate, firstCompareId, secondCompareId])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  useEffect(() => {
    if (!accessToken || !firstCompareId || !secondCompareId || firstCompareId === secondCompareId) {
      setComparison(null)
      return
    }

    let cancelled = false
    setIsComparing(true)
    api.compareSessions(accessToken, firstCompareId, secondCompareId)
      .then((data) => {
        if (!cancelled) {
          setComparison(data)
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to compare sessions')
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsComparing(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [accessToken, firstCompareId, secondCompareId])

  const categoryTone = useMemo(() => ({
    'on-track': 'bg-emerald-100 text-emerald-700',
    'needs-work': 'bg-amber-100 text-amber-700',
    'no-data': 'bg-slate-100 text-slate-600',
  }), [])

  const displayedCategories = useMemo(() => categories.map((category) => {
    const targetValue = goalTargets[category.key] ?? category.targetValue
    let status = category.status
    if (category.averageValue != null && targetValue != null) {
      const healthy = category.higherIsBetter
        ? category.averageValue >= targetValue * 0.8
        : category.averageValue <= targetValue * 1.2
      status = healthy ? 'on-track' : 'needs-work'
    }

    return {
      ...category,
      targetValue,
      status,
    }
  }), [categories, goalTargets])

  function updateGoalTarget(key: string, rawValue: string) {
    const value = Number(rawValue)
    const nextTargets = { ...goalTargets }

    if (rawValue === '' || Number.isNaN(value)) {
      delete nextTargets[key]
    } else {
      nextTargets[key] = value
    }

    setGoalTargets(nextTargets)
    window.localStorage.setItem(GOALS_STORAGE_KEY, JSON.stringify(nextTargets))
  }

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

  async function handleExport() {
    if (!accessToken) return

    try {
      setIsExporting(true)
      const blob = await api.exportProgressSummary(accessToken)
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = 'voxly-progress.csv'
      anchor.click()
      URL.revokeObjectURL(url)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to export progress summary')
    } finally {
      setIsExporting(false)
    }
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

        <Card className="bg-gradient-to-br from-primary to-cyan-700 text-primary-foreground">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <TrendingUp size={32} />
              <div>
                <h1 className="display-font text-2xl font-semibold sm:text-3xl">
                  Your Progress
                </h1>
                <p className="mt-1 text-sm text-primary-foreground/80">
                  Compare sessions, export results, and track each category.
                </p>
              </div>
            </div>
            <Button
              onClick={handleExport}
              disabled={isExporting}
              className="bg-white text-slate-900 hover:bg-white"
            >
              {isExporting ? <Loader2 className="animate-spin" size={18} /> : <Download size={18} />}
              Export CSV
            </Button>
          </div>
        </Card>

        {summary && (
          <>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <Card>
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
                    <Video className="text-primary" size={20} />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Total Sessions</p>
                    <p className="display-font text-2xl font-semibold text-foreground">{summary.totalSessions}</p>
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
                    <p className="display-font text-2xl font-semibold text-foreground">{summary.completedSessions}</p>
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

            <div className="grid gap-4 lg:grid-cols-4">
              <Card>
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-100">
                    <Mic2 className="text-amber-600" size={20} />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Avg. Filler Words</p>
                    <p className="display-font text-2xl font-semibold text-foreground">{summary.averageFillerWords ?? '--'}</p>
                  </div>
                </div>
              </Card>
              <Card>
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-cyan-100">
                    <Video className="text-cyan-700" size={20} />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Video Analyses</p>
                    <p className="display-font text-2xl font-semibold text-foreground">{summary.completedVideoAnalyses}</p>
                  </div>
                </div>
              </Card>
              <Card>
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-violet-100">
                    <BarChart3 className="text-violet-700" size={20} />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">Avg. Body Score</p>
                    <p className="display-font text-2xl font-semibold text-foreground">
                      {summary.averagePostureScore?.toFixed(0) ?? '--'}
                    </p>
                  </div>
                </div>
              </Card>
              <Card>
                <div className="space-y-1">
                  <p className="text-sm text-muted-foreground">Pipeline</p>
                  <p className="display-font text-2xl font-semibold text-foreground">
                    {summary.processingSessions}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    Failed: {summary.failedSessions} · AI video: {summary.aiVideoModuleEnabled ? 'on' : 'mock'}
                  </p>
                </div>
              </Card>
            </div>
          </>
        )}

        {displayedCategories.length > 0 && (
          <Card>
            <div className="mb-4 flex items-center gap-3">
              <BarChart3 className="text-primary" size={20} />
              <h2 className="display-font text-lg font-semibold text-foreground">Progress by Category</h2>
            </div>
            <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
              {displayedCategories.map((category) => (
                <div key={category.key} className="rounded-2xl border border-border bg-muted/15 p-4">
                  <div className="flex items-center justify-between gap-3">
                    <p className="text-sm font-semibold text-foreground">{category.label}</p>
                    <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${categoryTone[category.status]}`}>
                      {category.status}
                    </span>
                  </div>
                  <p className="mt-3 display-font text-2xl font-semibold text-foreground">
                    {category.averageValue != null ? category.averageValue.toFixed(category.key === 'clarity' ? 1 : 0) : '--'}
                  </p>
                  <p className="mt-1 text-xs text-muted-foreground">
                    Target: {category.targetValue != null ? category.targetValue.toFixed(category.key === 'clarity' ? 1 : 0) : '--'}
                  </p>
                </div>
              ))}
            </div>
          </Card>
        )}

        {displayedCategories.length > 0 && (
          <Card>
            <div className="mb-4 flex items-center gap-3">
              <SlidersHorizontal className="text-primary" size={20} />
              <h2 className="display-font text-lg font-semibold text-foreground">Improvement Goals</h2>
            </div>
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
              {displayedCategories.map((category) => (
                <label key={category.key} className="block space-y-1.5">
                  <span className="text-sm font-medium text-foreground">{category.label}</span>
                  <Input
                    type="number"
                    value={goalTargets[category.key] ?? ''}
                    placeholder={category.targetValue != null ? String(category.targetValue) : 'Target'}
                    onChange={(event) => updateGoalTarget(category.key, event.target.value)}
                  />
                </label>
              ))}
            </div>
          </Card>
        )}

        {completedSessions.length >= 2 && (
          <Card>
            <div className="mb-4 flex items-center gap-3">
              <Scale className="text-primary" size={20} />
              <h2 className="display-font text-lg font-semibold text-foreground">Compare Sessions</h2>
            </div>
            <div className="grid gap-3 lg:grid-cols-2">
              <div>
                <p className="mb-2 text-sm font-medium text-foreground">First session</p>
                <Select value={firstCompareId} onChange={(event) => setFirstCompareId(event.target.value)}>
                  <option value="">Select session</option>
                  {completedSessions.map((session) => (
                    <option key={session.id} value={session.id}>{session.title}</option>
                  ))}
                </Select>
              </div>
              <div>
                <p className="mb-2 text-sm font-medium text-foreground">Second session</p>
                <Select value={secondCompareId} onChange={(event) => setSecondCompareId(event.target.value)}>
                  <option value="">Select session</option>
                  {completedSessions.map((session) => (
                    <option key={session.id} value={session.id}>{session.title}</option>
                  ))}
                </Select>
              </div>
            </div>

            {isComparing ? (
              <div className="mt-6 flex items-center justify-center">
                <Loader2 className="animate-spin text-primary" size={28} />
              </div>
            ) : comparison ? (
              <div className="mt-6 space-y-3">
                {comparison.metrics.map((metric) => (
                  <div key={metric.label} className="rounded-2xl border border-border bg-muted/15 p-4">
                    <div className="flex items-center justify-between gap-3">
                      <p className="font-semibold text-foreground">{metric.label}</p>
                      <span className="text-sm text-muted-foreground">
                        {metric.delta != null ? `${metric.delta > 0 ? '+' : ''}${metric.delta.toFixed(1)}` : '--'}
                      </span>
                    </div>
                    <div className="mt-3 grid gap-3 sm:grid-cols-2">
                      <div className="rounded-xl bg-background p-3">
                        <p className="text-xs uppercase tracking-[0.14em] text-muted-foreground">{comparison.firstSession.title}</p>
                        <p className="mt-2 text-lg font-semibold text-foreground">{metric.firstValue?.toFixed(1) ?? '--'}</p>
                      </div>
                      <div className="rounded-xl bg-background p-3">
                        <p className="text-xs uppercase tracking-[0.14em] text-muted-foreground">{comparison.secondSession.title}</p>
                        <p className="mt-2 text-lg font-semibold text-foreground">{metric.secondValue?.toFixed(1) ?? '--'}</p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : null}
          </Card>
        )}

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
                      <td className="py-3 pr-4 text-muted-foreground">{formatDate(trend.date)}</td>
                      <td className={`py-3 pr-4 text-center font-semibold ${getScoreColor(trend.clarityScore)}`}>
                        {trend.clarityScore?.toFixed(1) ?? '--'}
                      </td>
                      <td className="py-3 pr-4 text-center text-foreground">{trend.wordsPerMinute ?? '--'}</td>
                      <td className="py-3 text-center text-foreground">{trend.fillerWordCount ?? '--'}</td>
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
              <h2 className="display-font text-lg font-semibold text-foreground">No completed sessions yet</h2>
              <p className="mt-2 max-w-sm text-sm text-muted-foreground">
                Complete your first practice session to start tracking your progress.
              </p>
              <Link to="/sessions/new" className="mt-6">
                <Button>Start Practicing</Button>
              </Link>
            </div>
          </Card>
        )}
      </div>
    </div>
  )
}
