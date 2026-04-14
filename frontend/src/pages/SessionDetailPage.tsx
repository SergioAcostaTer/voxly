import {
  AlertCircle,
  ArrowLeft,
  CheckCircle,
  Clock,
  Gauge,
  Loader2,
  MessageSquare,
  Mic2,
  RefreshCw,
  Timer,
  Video,
} from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { api, ApiClientError } from '../lib/api'
import { cn } from '../lib/cn'
import type { Evaluation, FeedbackNote, FeedbackResponse } from '../types/evaluation'
import type { Session } from '../types/sessions'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { Logo } from '../ui/Logo'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

const severityColors = {
  info: 'bg-blue-100 text-blue-700 border-blue-200',
  warning: 'bg-amber-100 text-amber-700 border-amber-200',
  suggestion: 'bg-green-100 text-green-700 border-green-200',
}

export function SessionDetailPage() {
  const { sessionId } = useParams<{ sessionId: string }>()
  const navigate = useNavigate()
  const { accessToken, logout } = useAuth()
  const videoRef = useRef<HTMLVideoElement>(null)

  const [session, setSession] = useState<Session | null>(null)
  const [evaluation, setEvaluation] = useState<Evaluation | null>(null)
  const [feedback, setFeedback] = useState<FeedbackResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null)
  const [isRefreshing, setIsRefreshing] = useState(false)

  const fetchData = useCallback(async () => {
    if (!accessToken || !sessionId) return

    try {
      const sessionData = await api.getSession(accessToken, sessionId)
      setSession(sessionData)

      if (sessionData.evaluationId || sessionData.status === 'completed' || sessionData.status === 'analyzing') {
        try {
          const evalData = await api.getEvaluation(accessToken, sessionId)
          setEvaluation(evalData)

          if (evalData.status === 'completed') {
            const feedbackData = await api.getFeedback(accessToken, sessionId)
            setFeedback(feedbackData)
          }
        } catch {
          // Evaluation might not exist yet
        }
      }
    } catch (err) {
      if (err instanceof ApiClientError && err.status === 401) {
        await logout()
        navigate('/login', { replace: true })
        return
      }
      if (err instanceof ApiClientError && err.status === 404) {
        navigate('/sessions', { replace: true })
        return
      }
      setError(err instanceof Error ? err.message : 'Failed to load session')
    } finally {
      setIsLoading(false)
      setIsRefreshing(false)
    }
  }, [accessToken, sessionId, logout, navigate])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  // Auto-refresh while analyzing
  useEffect(() => {
    if (session?.status === 'analyzing' || evaluation?.status === 'transcribing' || evaluation?.status === 'analyzing') {
      const interval = setInterval(fetchData, 5000)
      return () => clearInterval(interval)
    }
  }, [session?.status, evaluation?.status, fetchData])

  function handleRefresh() {
    setIsRefreshing(true)
    fetchData()
  }

  function seekToTimestamp(seconds: number) {
    if (videoRef.current) {
      videoRef.current.currentTime = seconds
      videoRef.current.play()
    }
  }

  function formatTimestamp(seconds: number) {
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  function formatDate(dateString: string) {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'long',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    })
  }

  const feedbackNotes: FeedbackNote[] = feedback?.notes ?? []
  const categories = [...new Set(feedbackNotes.map((n) => n.category))]
  const filteredNotes = selectedCategory
    ? feedbackNotes.filter((n) => n.category === selectedCategory)
    : feedbackNotes

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="animate-spin text-primary" size={48} />
      </div>
    )
  }

  if (!session) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Card>
          <div className="text-center">
            <AlertCircle className="mx-auto mb-4 text-red-500" size={48} />
            <h2 className="display-font text-lg font-semibold">Session not found</h2>
            <Link to="/sessions" className="mt-4 block">
              <Button>Back to Sessions</Button>
            </Link>
          </div>
        </Card>
      </div>
    )
  }

  const isAnalyzing = session.status === 'analyzing' || evaluation?.status === 'transcribing' || evaluation?.status === 'analyzing'
  const videoUrl = session.mediaFile ? `${API_BASE_URL}${session.mediaFile.url}` : null

  return (
    <div className="px-4 py-6 sm:px-6 sm:py-8 lg:py-12">
      <div className="mx-auto w-full max-w-5xl space-y-6">
        <header className="flex items-center justify-between rounded-2xl border border-white/70 bg-white/80 px-4 py-3 shadow-panel backdrop-blur-sm">
          <Logo />
          <Link to="/sessions">
            <Button variant="ghost">
              <ArrowLeft size={18} />
              Back
            </Button>
          </Link>
        </header>

        {error && (
          <Card className="border-red-200 bg-red-50">
            <div className="flex items-center gap-2 text-red-600">
              <AlertCircle size={18} />
              <p>{error}</p>
            </div>
          </Card>
        )}

        {/* Session Header */}
        <Card className="bg-gradient-to-br from-primary to-cyan-700 text-primary-foreground">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p className="display-font text-xs font-semibold uppercase tracking-[0.18em] text-primary-foreground/80">
                {session.sessionType.toLowerCase()} session
              </p>
              <h1 className="display-font mt-2 text-2xl font-semibold sm:text-3xl">
                {session.title}
              </h1>
              <p className="mt-2 text-sm text-primary-foreground/80">
                Created {formatDate(session.createdAt)}
              </p>
            </div>
            <div className="flex items-center gap-2">
              {isAnalyzing && (
                <span className="flex items-center gap-2 rounded-full bg-white/20 px-3 py-1 text-sm">
                  <Loader2 className="animate-spin" size={14} />
                  Analyzing...
                </span>
              )}
              {session.status === 'completed' && (
                <span className="flex items-center gap-2 rounded-full bg-white/20 px-3 py-1 text-sm">
                  <CheckCircle size={14} />
                  Completed
                </span>
              )}
              <Button
                variant="ghost"
                onClick={handleRefresh}
                disabled={isRefreshing}
                className="text-primary-foreground hover:bg-white/20"
              >
                <RefreshCw className={cn(isRefreshing && 'animate-spin')} size={18} />
              </Button>
            </div>
          </div>
        </Card>

        {/* Analyzing State */}
        {isAnalyzing && (
          <Card>
            <div className="py-8 text-center">
              <Loader2 className="mx-auto mb-4 animate-spin text-primary" size={48} />
              <h2 className="display-font text-lg font-semibold text-foreground">
                {evaluation?.status === 'transcribing' ? 'Transcribing audio...' : 'Analyzing your presentation...'}
              </h2>
              <p className="mt-2 text-sm text-muted-foreground">
                We're processing your recording. This page will update automatically.
              </p>
            </div>
          </Card>
        )}

        {/* Video Player */}
        {videoUrl && (
          <Card>
            <h2 className="display-font mb-4 text-lg font-semibold text-foreground">
              <Video className="mr-2 inline-block" size={20} />
              Recording
            </h2>
            <video
              ref={videoRef}
              src={videoUrl}
              controls
              className="w-full rounded-xl bg-black"
              preload="metadata"
            >
              Your browser does not support video playback.
            </video>
            {session.mediaFile && (
              <p className="mt-2 text-xs text-muted-foreground">
                {session.mediaFile.originalFileName}
                {session.mediaFile.durationSeconds && (
                  <> &middot; {formatTimestamp(session.mediaFile.durationSeconds)}</>
                )}
              </p>
            )}
          </Card>
        )}

        {/* Metrics */}
        {evaluation?.status === 'completed' && evaluation.metrics && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Card>
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
                  <Gauge className="text-primary" size={20} />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Clarity Score</p>
                  <p className="display-font text-2xl font-semibold text-foreground">
                    {evaluation.metrics.clarityScore?.toFixed(1) ?? '--'}
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
                  <p className="text-sm text-muted-foreground">Words/Min</p>
                  <p className="display-font text-2xl font-semibold text-foreground">
                    {evaluation.metrics.wordsPerMinute ?? '--'}
                  </p>
                </div>
              </div>
            </Card>

            <Card>
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-100">
                  <Mic2 className="text-amber-600" size={20} />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Filler Words</p>
                  <p className="display-font text-2xl font-semibold text-foreground">
                    {evaluation.metrics.fillerWordCount ?? '--'}
                  </p>
                </div>
              </div>
            </Card>

            <Card>
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-green-100">
                  <Clock className="text-green-600" size={20} />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Pauses</p>
                  <p className="display-font text-2xl font-semibold text-foreground">
                    {evaluation.metrics.pauseCount ?? '--'}
                  </p>
                </div>
              </div>
            </Card>
          </div>
        )}

        {/* Transcription */}
        {evaluation?.transcription && (
          <Card>
            <h2 className="display-font mb-4 text-lg font-semibold text-foreground">
              Transcription
            </h2>
            <div className="max-h-64 overflow-y-auto rounded-xl bg-muted/30 p-4 text-sm leading-relaxed text-foreground">
              {evaluation.transcription.fullText}
            </div>
            {evaluation.transcription.detectedLanguage && (
              <p className="mt-2 text-xs text-muted-foreground">
                Detected language: {evaluation.transcription.detectedLanguage}
              </p>
            )}
          </Card>
        )}

        {/* Feedback */}
        {feedbackNotes.length > 0 && (
          <Card>
            <div className="mb-4 flex flex-wrap items-center justify-between gap-4">
              <h2 className="display-font text-lg font-semibold text-foreground">
                <MessageSquare className="mr-2 inline-block" size={20} />
                Feedback Notes
              </h2>
              <div className="flex flex-wrap gap-2">
                <button
                  onClick={() => setSelectedCategory(null)}
                  className={cn(
                    'rounded-full px-3 py-1 text-sm transition-colors',
                    selectedCategory === null
                      ? 'bg-primary text-primary-foreground'
                      : 'bg-muted text-muted-foreground hover:bg-muted/80',
                  )}
                >
                  All ({feedbackNotes.length})
                </button>
                {categories.map((cat) => (
                  <button
                    key={cat}
                    onClick={() => setSelectedCategory(cat)}
                    className={cn(
                      'rounded-full px-3 py-1 text-sm capitalize transition-colors',
                      selectedCategory === cat
                        ? 'bg-primary text-primary-foreground'
                        : 'bg-muted text-muted-foreground hover:bg-muted/80',
                    )}
                  >
                    {cat} ({feedbackNotes.filter((n) => n.category === cat).length})
                  </button>
                ))}
              </div>
            </div>

            <div className="space-y-3">
              {filteredNotes.map((note, index) => (
                <div
                  key={index}
                  className={cn(
                    'rounded-xl border p-4',
                    severityColors[note.severity as keyof typeof severityColors] || 'bg-gray-100 text-gray-700 border-gray-200',
                  )}
                >
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex-1">
                      <div className="mb-1 flex items-center gap-2">
                        <span className="font-semibold capitalize">{note.category}</span>
                        <span className="rounded bg-black/10 px-2 py-0.5 text-xs font-medium">
                          {note.severity}
                        </span>
                      </div>
                      <p className="text-sm">{note.message}</p>
                    </div>
                    {note.timestampSeconds != null && (
                      <button
                        onClick={() => seekToTimestamp(note.timestampSeconds!)}
                        className="shrink-0 font-mono text-sm underline decoration-dotted hover:opacity-70"
                        title="Jump to timestamp in video"
                      >
                        {formatTimestamp(note.timestampSeconds)}
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </Card>
        )}

        {/* Overall Summary */}
        {feedback?.overallSummary && (
          <Card>
            <h2 className="display-font mb-4 text-lg font-semibold text-foreground">
              Overall Summary
            </h2>
            <p className="text-muted-foreground">{feedback.overallSummary}</p>

            {feedback.strengths.length > 0 && (
              <div className="mt-4">
                <h3 className="mb-2 text-sm font-semibold text-green-700">Strengths</h3>
                <ul className="space-y-1 text-sm text-muted-foreground">
                  {feedback.strengths.map((s, i) => (
                    <li key={i} className="flex items-start gap-2">
                      <CheckCircle className="mt-0.5 shrink-0 text-green-500" size={14} />
                      {s}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {feedback.areasForImprovement.length > 0 && (
              <div className="mt-4">
                <h3 className="mb-2 text-sm font-semibold text-amber-700">Areas for Improvement</h3>
                <ul className="space-y-1 text-sm text-muted-foreground">
                  {feedback.areasForImprovement.map((a, i) => (
                    <li key={i} className="flex items-start gap-2">
                      <AlertCircle className="mt-0.5 shrink-0 text-amber-500" size={14} />
                      {a}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </Card>
        )}

        {/* Error State */}
        {session.status === 'failed' && (
          <Card className="border-red-200 bg-red-50">
            <div className="flex items-start gap-3">
              <AlertCircle className="mt-0.5 shrink-0 text-red-500" size={20} />
              <div>
                <h3 className="font-semibold text-red-700">Analysis Failed</h3>
                <p className="mt-1 text-sm text-red-600">
                  {evaluation?.errorMessage ?? 'An error occurred during analysis. Please try uploading again.'}
                </p>
              </div>
            </div>
          </Card>
        )}
      </div>
    </div>
  )
}
