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
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { AUTH_BYPASS_ENABLED } from '../auth/testing-auth'
import { useAuth } from '../auth/useAuth'
import { AppHeader } from '../components/AppHeader'
import { api, ApiClientError } from '../lib/api'
import { cn } from '../lib/cn'
import type { Evaluation, FeedbackNote, FeedbackResponse } from '../types/evaluation'
import type { Session } from '../types/sessions'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

const severityColors = {
  info: 'bg-blue-100 text-blue-700 border-blue-200',
  warning: 'bg-amber-100 text-amber-700 border-amber-200',
  suggestion: 'bg-green-100 text-green-700 border-green-200',
}

type ProcessingState = {
  label: string
  description: string
  spinner: boolean
  tone: 'neutral' | 'accent' | 'success' | 'danger'
}

function getProcessingState(session: Session, evaluation: Evaluation | null): ProcessingState {
  if (session.status === 'failed' || evaluation?.status === 'failed') {
    return {
      label: 'Processing failed',
      description: evaluation?.errorMessage ?? 'Something went wrong while processing this session.',
      spinner: false,
      tone: 'danger',
    }
  }

  if (evaluation?.status === 'transcribing') {
    return {
      label: 'Transcribing audio',
      description: 'The video is being converted into text. You can safely refresh or come back later.',
      spinner: true,
      tone: 'accent',
    }
  }

  if (evaluation?.status === 'analyzing' || session.status === 'analyzing') {
    return {
      label: 'Analyzing presentation',
      description: 'The transcript is ready and the analysis engine is generating feedback.',
      spinner: true,
      tone: 'accent',
    }
  }

  if (evaluation?.status === 'pending' || session.status === 'uploaded') {
    return {
      label: 'Queued for processing',
      description: 'The session is uploaded and waiting for the async pipeline to pick it up.',
      spinner: true,
      tone: 'neutral',
    }
  }

  if (evaluation?.status === 'completed' || session.status === 'completed') {
    return {
      label: 'Processing complete',
      description: 'Transcript and feedback are ready below.',
      spinner: false,
      tone: 'success',
    }
  }

  return {
    label: 'Draft session',
    description: 'Upload media to start the analysis pipeline.',
    spinner: false,
    tone: 'neutral',
  }
}

export function SessionDetailPage() {
  const { sessionId } = useParams<{ sessionId: string }>()
  const navigate = useNavigate()
  const { accessToken, logout } = useAuth()
  const mediaRef = useRef<HTMLMediaElement>(null)

  const [session, setSession] = useState<Session | null>(null)
  const [evaluation, setEvaluation] = useState<Evaluation | null>(null)
  const [feedback, setFeedback] = useState<FeedbackResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null)
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [lastSyncedAt, setLastSyncedAt] = useState<Date | null>(null)
  const [isLivePolling, setIsLivePolling] = useState(false)
  const streamAbortRef = useRef<AbortController | null>(null)

  const fetchData = useCallback(async () => {
    if (!accessToken || !sessionId) return

    try {
      setError(null)
      const sessionData = await api.getSession(accessToken, sessionId)
      setSession(sessionData)

      if (sessionData.evaluationId || sessionData.status !== 'draft') {
        try {
          const evalData = await api.getEvaluation(accessToken, sessionId)
          setEvaluation(evalData)

          if (evalData.status === 'completed') {
            const feedbackData = await api.getFeedback(accessToken, sessionId)
            setFeedback(feedbackData)
          } else {
            setFeedback(null)
          }
        } catch (evaluationError) {
          if (evaluationError instanceof ApiClientError && evaluationError.status === 404) {
            setEvaluation(null)
            setFeedback(null)
          } else if (evaluationError instanceof ApiClientError && evaluationError.status === 401) {
            if (AUTH_BYPASS_ENABLED) {
              setError('Testing mode is active but the backend did not accept the test user request.')
              return
            }
            await logout()
            navigate('/login', { replace: true })
            return
          }
        }
      } else {
        setEvaluation(null)
        setFeedback(null)
      }

      setLastSyncedAt(new Date())
    } catch (err) {
      if (err instanceof ApiClientError && err.status === 401) {
        if (AUTH_BYPASS_ENABLED) {
          setError('Testing mode is active but the backend did not accept the test user request.')
          return
        }
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

  const processingState = useMemo(() => {
    if (!session) {
      return null
    }

    return getProcessingState(session, evaluation)
  }, [session, evaluation])

  const shouldPoll = Boolean(
    session && (
      session.status === 'uploaded' ||
      session.status === 'analyzing' ||
      evaluation?.status === 'pending' ||
      evaluation?.status === 'transcribing' ||
      evaluation?.status === 'analyzing'
    ),
  )

  useEffect(() => {
    if (!shouldPoll) {
      streamAbortRef.current?.abort()
      streamAbortRef.current = null
      setIsLivePolling(false)
      return
    }

    const abortController = new AbortController()
    streamAbortRef.current = abortController
    setIsLivePolling(true)

    void api.streamSessionEvents(
      accessToken!,
      sessionId!,
      async ({ event }) => {
        if (event === 'status' || event === 'connected') {
          await fetchData()
        }
      },
      abortController.signal,
    ).catch(() => {
      if (!abortController.signal.aborted) {
        setIsLivePolling(false)
      }
    })

    return () => {
      abortController.abort()
      if (streamAbortRef.current === abortController) {
        streamAbortRef.current = null
      }
    }
  }, [shouldPoll, fetchData, accessToken, sessionId])

  function handleRefresh() {
    setIsRefreshing(true)
    fetchData()
  }

  function seekToTimestamp(seconds: number) {
    if (mediaRef.current) {
      mediaRef.current.currentTime = seconds
      void mediaRef.current.play()
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
  const isProcessing = session.status === 'uploaded' || isAnalyzing || evaluation?.status === 'pending'
  const mediaUrl = session.mediaFile ? `${API_BASE_URL}${session.mediaFile.url}` : null
  const isAudioMedia = session.mediaFile?.contentType.startsWith('audio/') ?? false

  return (
    <div className="px-4 pb-6 pt-28 sm:px-6 sm:pb-8 lg:px-8 lg:pb-12">
      <div className="mx-auto w-full max-w-6xl space-y-6">
        <AppHeader
          rightSlot={
            <Link to="/sessions">
              <Button variant="ghost">
                <ArrowLeft size={18} />
                Back
              </Button>
            </Link>
          }
        />

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
              {lastSyncedAt && (
                <p className="mt-1 text-xs text-primary-foreground/70">
                  Last synced {lastSyncedAt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </p>
              )}
            </div>
            <div className="flex items-center gap-2">
              {isLivePolling && (
                <span className="flex items-center gap-2 rounded-full bg-white/20 px-3 py-1 text-sm">
                  <Loader2 className="animate-spin" size={14} />
                  Live sync
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

        {/* Live Processing Status */}
        {processingState && (
          <Card
            className={cn(
              processingState.tone === 'danger' && 'border-red-200 bg-red-50',
              processingState.tone === 'success' && 'border-emerald-200 bg-emerald-50',
              processingState.tone === 'accent' && 'border-cyan-200 bg-cyan-50',
            )}
          >
            <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
              <div className="flex items-start gap-4">
                <div className={cn(
                  'flex h-12 w-12 items-center justify-center rounded-2xl',
                  processingState.tone === 'danger' && 'bg-red-100 text-red-600',
                  processingState.tone === 'success' && 'bg-emerald-100 text-emerald-600',
                  processingState.tone === 'accent' && 'bg-cyan-100 text-cyan-600',
                  processingState.tone === 'neutral' && 'bg-muted text-muted-foreground',
                )}>
                  {processingState.spinner ? <Loader2 className="animate-spin" size={22} /> : <CheckCircle size={22} />}
                </div>
                <div>
                  <h2 className="display-font text-lg font-semibold text-foreground">
                    {processingState.label}
                  </h2>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {processingState.description}
                  </p>
                </div>
              </div>
              <div className="grid gap-2 text-sm text-muted-foreground md:text-right">
                <span>Session status: {session.status}</span>
                <span>Evaluation status: {evaluation?.status ?? 'not created yet'}</span>
                <span>Reload-safe: yes, progress is saved server-side</span>
              </div>
            </div>

            {isProcessing && (
              <div className="mt-4 grid gap-3 md:grid-cols-3">
                <div className="rounded-xl bg-white/70 p-4">
                  <p className="text-xs uppercase tracking-wide text-muted-foreground">Step 1</p>
                  <p className="mt-1 font-semibold text-foreground">Upload stored</p>
                  <p className="mt-1 text-sm text-muted-foreground">Media is saved in S3/R2 and linked to the session.</p>
                </div>
                <div className="rounded-xl bg-white/70 p-4">
                  <p className="text-xs uppercase tracking-wide text-muted-foreground">Step 2</p>
                  <p className="mt-1 font-semibold text-foreground">Transcription</p>
                  <p className="mt-1 text-sm text-muted-foreground">Whisper runs detached; this page keeps syncing status.</p>
                </div>
                <div className="rounded-xl bg-white/70 p-4">
                  <p className="text-xs uppercase tracking-wide text-muted-foreground">Step 3</p>
                  <p className="mt-1 font-semibold text-foreground">Analysis</p>
                  <p className="mt-1 text-sm text-muted-foreground">Feedback is generated and saved so you can refresh safely.</p>
                </div>
              </div>
            )}
          </Card>
        )}

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

        {/* Media Player */}
        {mediaUrl && (
          <Card>
            <h2 className="display-font mb-4 text-lg font-semibold text-foreground">
              {isAudioMedia ? <Mic2 className="mr-2 inline-block" size={20} /> : <Video className="mr-2 inline-block" size={20} />}
              {isAudioMedia ? 'Audio recording' : 'Recording'}
            </h2>
            {isAudioMedia ? (
              <audio
                ref={(node) => {
                  mediaRef.current = node
                }}
                src={mediaUrl}
                controls
                className="w-full"
                preload="metadata"
              >
                Your browser does not support audio playback.
              </audio>
            ) : (
              <video
                ref={(node) => {
                  mediaRef.current = node
                }}
                src={mediaUrl}
                controls
                className="w-full rounded-xl bg-black"
                preload="metadata"
              >
                Your browser does not support video playback.
              </video>
            )}
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

        {evaluation?.status !== 'completed' && session.mediaFile && (
          <Card className="border-dashed border-border bg-muted/20">
            <div className="flex items-start gap-3">
              <Loader2 className={cn('mt-0.5 shrink-0 text-primary', isProcessing && 'animate-spin')} size={18} />
              <div>
                <h2 className="font-semibold text-foreground">Processing in progress</h2>
                <p className="mt-1 text-sm text-muted-foreground">
                  You can reload this page at any time. The backend will recover pending work and this view will continue polling until the result is ready.
                </p>
              </div>
            </div>
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
