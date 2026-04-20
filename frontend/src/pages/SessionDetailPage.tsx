import {
    AlertCircle,
    ArrowLeft,
    Bot,
    CheckCircle,
    Clock,
    Gauge,
    Loader2,
    MessageSquare,
    Mic2,
    PlayCircle,
    RefreshCw,
    SkipForward,
    Timer,
    Video,
    X,
} from 'lucide-react'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { AUTH_BYPASS_ENABLED } from '../auth/testing-auth'
import { useAuth } from '../auth/useAuth'
import { AppHeader } from '../components/AppHeader'
import { TypewriterText } from '../components/TypewriterText'
import { api, ApiClientError } from '../lib/api'
import { cn } from '../lib/cn'
import type { Evaluation, FeedbackNote, FeedbackResponse, SegmentData } from '../types/evaluation'
import type { Session } from '../types/sessions'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

const severityColors = {
  info: 'bg-blue-100 text-blue-700 border-blue-200',
  warning: 'bg-amber-100 text-amber-700 border-amber-200',
  suggestion: 'bg-green-100 text-green-700 border-green-200',
  critical: 'bg-rose-100 text-rose-700 border-rose-200',
}

const GUIDED_TRIGGER_SEVERITIES = new Set(['warning', 'suggestion', 'critical'])

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
      description: 'Your results are being prepared now.',
      spinner: true,
      tone: 'accent',
    }
  }

  if (evaluation?.status === 'pending' || session.status === 'uploaded') {
    return {
      label: 'Queued for processing',
      description: 'Your recording is uploaded and will start shortly.',
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
    description: 'Upload your recording to get feedback.',
    spinner: false,
    tone: 'neutral',
  }
}

export function SessionDetailPage() {
  const { sessionId } = useParams<{ sessionId: string }>()
  const navigate = useNavigate()
  const { accessToken, logout } = useAuth()
  const mediaRef = useRef<HTMLMediaElement>(null)
  const transcriptContainerRef = useRef<HTMLDivElement>(null)
  const previousTimeRef = useRef(0)
  const isSeekingRef = useRef(false)

  const [session, setSession] = useState<Session | null>(null)
  const [evaluation, setEvaluation] = useState<Evaluation | null>(null)
  const [feedback, setFeedback] = useState<FeedbackResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null)
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [lastSyncedAt, setLastSyncedAt] = useState<Date | null>(null)
  const [isLivePolling, setIsLivePolling] = useState(false)
  const [currentTime, setCurrentTime] = useState(0)
  const [isPlaying, setIsPlaying] = useState(false)
  const [guidedModeEnabled, setGuidedModeEnabled] = useState(false)
  const [activeCoachNote, setActiveCoachNote] = useState<FeedbackNote | null>(null)
  const [selectedFeedbackNote, setSelectedFeedbackNote] = useState<FeedbackNote | null>(null)
  const [dismissedNotes, setDismissedNotes] = useState<Set<number>>(new Set())
  const [mediaDuration, setMediaDuration] = useState<number | null>(null)
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
              setError('We could not verify your access right now. Please try again.')
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
          setError('We could not verify your access right now. Please try again.')
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
      previousTimeRef.current = seconds
      setCurrentTime(seconds)
    }
  }

  function openFeedbackNote(note: FeedbackNote) {
    if (note.timestampSeconds != null) {
      if (mediaRef.current) {
        mediaRef.current.currentTime = note.timestampSeconds
        previousTimeRef.current = note.timestampSeconds
        setCurrentTime(note.timestampSeconds)
      }
    }
    mediaRef.current?.pause()
    setIsPlaying(false)
    setSelectedFeedbackNote(note)
  }

  function closeFeedbackNote() {
    setSelectedFeedbackNote(null)
  }

  function normalizeSeverity(severity: string | null | undefined) {
    return severity?.toLowerCase() ?? 'info'
  }

  const feedbackNotes = useMemo<FeedbackNote[]>(() => feedback?.notes ?? [], [feedback])
  const transcriptSegments = useMemo<SegmentData[]>(
    () => evaluation?.transcription?.segments ?? [],
    [evaluation],
  )

  const guidedNotes = useMemo(() => {
    return feedbackNotes
      .filter((note): note is FeedbackNote & { timestampSeconds: number } => {
        if (note.timestampSeconds == null) {
          return false
        }

        return GUIDED_TRIGGER_SEVERITIES.has(normalizeSeverity(note.severity))
      })
      .sort((a, b) => a.timestampSeconds - b.timestampSeconds)
  }, [feedbackNotes])

  const playbackDuration = mediaDuration
    ?? session?.mediaFile?.durationSeconds
    ?? evaluation?.transcription?.durationSeconds
    ?? null

  const activeSegmentIndex = useMemo(() => {
    if (!transcriptSegments.length) {
      return -1
    }

    return transcriptSegments.findIndex((segment) => currentTime >= segment.startSeconds && currentTime <= segment.endSeconds)
  }, [transcriptSegments, currentTime])

  useEffect(() => {
    if (!guidedModeEnabled || activeSegmentIndex < 0 || !transcriptContainerRef.current) {
      return
    }

    const activeNode = transcriptContainerRef.current.querySelector<HTMLElement>(`[data-segment-index="${activeSegmentIndex}"]`)
    if (!activeNode) {
      return
    }

    activeNode.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }, [activeSegmentIndex, guidedModeEnabled])

  function handleTimeUpdate() {
    if (!mediaRef.current) return

    const time = mediaRef.current.currentTime
    setCurrentTime(time)

    if (!guidedModeEnabled || activeCoachNote || isSeekingRef.current || !isPlaying) {
      previousTimeRef.current = time
      return
    }

    const upcomingNote = guidedNotes.find((note) => {
      const isTimeMatch = Math.abs(time - note.timestampSeconds) < 0.3
      const isNotDismissed = !dismissedNotes.has(note.timestampSeconds)
      return isTimeMatch && isNotDismissed
    })

    if (upcomingNote) {
      mediaRef.current.pause()
      setIsPlaying(false)
      setActiveCoachNote(upcomingNote)
      setSelectedFeedbackNote(upcomingNote)
      setDismissedNotes((prev) => {
        const next = new Set(prev)
        next.add(upcomingNote.timestampSeconds)
        return next
      })
    }

    previousTimeRef.current = time
  }

  function handleSeeked() {
    if (!mediaRef.current) return
    isSeekingRef.current = false
    const time = mediaRef.current.currentTime
    setCurrentTime(time)

    if (time < previousTimeRef.current) {
      setDismissedNotes(new Set())
    }

    previousTimeRef.current = time
  }

  function handlePlay() {
    setIsPlaying(true)
  }

  function handlePause() {
    setIsPlaying(false)
  }

  function handleSeeking() {
    isSeekingRef.current = true
  }

  function startGuidedReview() {
    if (!mediaRef.current) {
      return
    }

    setGuidedModeEnabled(true)
    setDismissedNotes(new Set())
    setActiveCoachNote(null)
    void mediaRef.current.play()
  }

  function continueGuidedReview() {
    setActiveCoachNote(null)
    setSelectedFeedbackNote(null)
    void mediaRef.current?.play()
  }

  function replayRecentClip() {
    if (!mediaRef.current) {
      return
    }

    mediaRef.current.currentTime = Math.max(0, mediaRef.current.currentTime - 5)
    previousTimeRef.current = mediaRef.current.currentTime
    setActiveCoachNote(null)
    setSelectedFeedbackNote(null)
    void mediaRef.current.play()
  }

  function skipToNextGuidedMoment() {
    if (!mediaRef.current) {
      return
    }

    const nextMoment = guidedNotes.find((note) => note.timestampSeconds > currentTime)
    if (!nextMoment) {
      return
    }

    mediaRef.current.currentTime = Math.max(0, nextMoment.timestampSeconds - 0.2)
    previousTimeRef.current = mediaRef.current.currentTime
    setActiveCoachNote(null)
    setSelectedFeedbackNote(null)
    void mediaRef.current.play()
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
                  Updated {lastSyncedAt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </p>
              )}
            </div>
            <div className="flex items-center gap-2">
              {isLivePolling && (
                <span className="flex items-center gap-2 rounded-full bg-white/20 px-3 py-1 text-sm">
                  <Loader2 className="animate-spin" size={14} />
                  Updating
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
                <span>Your progress is saved automatically.</span>
                <span>You can leave this page and come back anytime.</span>
                <span>We'll keep this page updated for you.</span>
              </div>
            </div>

            {isProcessing && (
              <div className="mt-4 grid gap-3 md:grid-cols-3">
                <div className="rounded-xl bg-white/70 p-4">
                  <p className="text-xs uppercase tracking-wide text-muted-foreground">Step 1</p>
                  <p className="mt-1 font-semibold text-foreground">Upload received</p>
                  <p className="mt-1 text-sm text-muted-foreground">Your recording is safely saved.</p>
                </div>
                <div className="rounded-xl bg-white/70 p-4">
                  <p className="text-xs uppercase tracking-wide text-muted-foreground">Step 2</p>
                  <p className="mt-1 font-semibold text-foreground">Transcript in progress</p>
                  <p className="mt-1 text-sm text-muted-foreground">We're turning your recording into text.</p>
                </div>
                <div className="rounded-xl bg-white/70 p-4">
                  <p className="text-xs uppercase tracking-wide text-muted-foreground">Step 3</p>
                  <p className="mt-1 font-semibold text-foreground">Feedback</p>
                  <p className="mt-1 text-sm text-muted-foreground">You'll see your coaching notes here as soon as they're ready.</p>
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
          <div className={cn('grid gap-4', guidedModeEnabled && 'lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]')}>
            <Card className={cn(guidedModeEnabled && 'ring-1 ring-primary/30')}>
              <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                <h2 className="display-font text-lg font-semibold text-foreground">
                  {isAudioMedia ? <Mic2 className="mr-2 inline-block" size={20} /> : <Video className="mr-2 inline-block" size={20} />}
                  {isAudioMedia ? 'Audio recording' : 'Recording'}
                </h2>
                <div className="flex flex-wrap gap-2">
                  {!guidedModeEnabled ? (
                    <Button onClick={startGuidedReview} disabled={guidedNotes.length === 0}>
                      <PlayCircle size={18} />
                      Start Guided Review
                    </Button>
                  ) : (
                    <>
                      <Button variant="secondary" onClick={skipToNextGuidedMoment} disabled={guidedNotes.length === 0}>
                        <SkipForward size={18} />
                        Skip to next moment
                      </Button>
                      <Button
                        variant="ghost"
                        onClick={() => {
                          setGuidedModeEnabled(false)
                          setActiveCoachNote(null)
                        }}
                      >
                        Exit Guided Mode
                      </Button>
                    </>
                  )}
                </div>
              </div>

              <div className={cn('relative rounded-xl', isAudioMedia ? 'bg-muted/30 p-4' : 'bg-black')}>
                {isAudioMedia ? (
                  <audio
                    ref={(node) => {
                      mediaRef.current = node
                    }}
                    src={mediaUrl}
                    controls
                    className="w-full"
                    preload="metadata"
                    onLoadedMetadata={() => setMediaDuration(mediaRef.current?.duration ?? null)}
                    onTimeUpdate={handleTimeUpdate}
                    onSeeking={handleSeeking}
                    onSeeked={handleSeeked}
                    onPlay={handlePlay}
                    onPause={handlePause}
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
                    onLoadedMetadata={() => setMediaDuration(mediaRef.current?.duration ?? null)}
                    onTimeUpdate={handleTimeUpdate}
                    onSeeking={handleSeeking}
                    onSeeked={handleSeeked}
                    onPlay={handlePlay}
                    onPause={handlePause}
                  >
                    Your browser does not support video playback.
                  </video>
                )}

                {guidedModeEnabled && activeCoachNote && (
                  <div className="absolute inset-x-3 bottom-3 z-20 rounded-2xl border border-white/20 bg-slate-900/95 p-4 text-white shadow-2xl sm:inset-x-4 sm:bottom-4 sm:p-6">
                    <div className="flex gap-3 sm:gap-4">
                      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-cyan-500/20 text-cyan-200 sm:h-12 sm:w-12">
                        <Bot size={20} />
                      </div>

                      <div className="min-w-0 flex-1">
                        <h4 className="text-xs font-bold uppercase tracking-[0.14em] text-cyan-200 sm:text-sm">
                          {activeCoachNote.title ?? 'Coach Insight'}
                        </h4>
                        <p className="mt-1 text-xs text-white/70 sm:text-sm">
                          {activeCoachNote.category} · {activeCoachNote.severity}
                        </p>

                        <div className="mt-3 text-sm leading-relaxed text-white sm:text-base">
                          <TypewriterText
                            key={`${activeCoachNote.timestampSeconds ?? 'none'}-${activeCoachNote.coachScript ?? activeCoachNote.message}`}
                            text={activeCoachNote.coachScript ?? activeCoachNote.message}
                          />
                        </div>

                        <div className="mt-5 flex flex-wrap gap-2 sm:gap-3">
                          <Button
                            variant="secondary"
                            className="border-white/20 bg-white/10 text-white hover:bg-white/20"
                            onClick={replayRecentClip}
                          >
                            ⏪ Rewind 5s & Listen
                          </Button>

                          <Button onClick={continueGuidedReview}>
                            Got it, continue ▶
                          </Button>

                          <Button
                            variant="ghost"
                            className="border border-white/20 bg-white/5 text-white hover:bg-white/15"
                            onClick={() => openFeedbackNote(activeCoachNote)}
                          >
                            Open note details
                          </Button>
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {guidedModeEnabled && playbackDuration && guidedNotes.length > 0 && (
                <div className="mt-4">
                  <div className="relative h-2 rounded-full bg-muted">
                    {guidedNotes.map((note, index) => {
                      const left = Math.min(100, Math.max(0, (note.timestampSeconds / playbackDuration) * 100))
                      const isActive = activeCoachNote?.timestampSeconds === note.timestampSeconds
                      return (
                        <button
                          key={`${note.timestampSeconds}-${index}`}
                          onClick={() => openFeedbackNote(note)}
                          className={cn(
                            'absolute top-1/2 h-3 w-3 -translate-x-1/2 -translate-y-1/2 rounded-full border border-white',
                            isActive ? 'bg-primary shadow-glow' : 'bg-cyan-500/90',
                          )}
                          style={{ left: `${left}%` }}
                          title={`Coaching moment at ${formatTimestamp(note.timestampSeconds)}`}
                        />
                      )
                    })}
                  </div>
                  <p className="mt-2 text-xs text-muted-foreground">
                    {guidedNotes.length} coaching moments · {isPlaying ? 'Playing' : 'Paused'} · {formatTimestamp(currentTime)}
                  </p>
                </div>
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

            {guidedModeEnabled && (
              <Card className="lg:max-h-[34rem] lg:overflow-hidden">
                <h3 className="display-font mb-3 text-base font-semibold text-foreground">
                  Karaoke Transcript
                </h3>

                {transcriptSegments.length > 0 ? (
                  <div ref={transcriptContainerRef} className="h-80 overflow-y-auto rounded-xl bg-muted/25 p-4 text-sm leading-7 lg:h-full">
                    {transcriptSegments.map((segment, index) => {
                      const isActive = currentTime >= segment.startSeconds && currentTime <= segment.endSeconds
                      return (
                        <span
                          key={index}
                          data-segment-index={index}
                          className={cn(
                            'mr-1 inline rounded px-1 py-0.5 transition-colors duration-200',
                            isActive
                              ? 'bg-primary/15 font-semibold text-primary'
                              : 'text-muted-foreground',
                          )}
                        >
                          {segment.text}
                        </span>
                      )
                    })}
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground">
                    Timestamped transcript segments are not available for this session yet.
                  </p>
                )}
              </Card>
            )}
          </div>
        )}

        {evaluation?.status !== 'completed' && session.mediaFile && (
          <Card className="border-dashed border-border bg-muted/20">
            <div className="flex items-start gap-3">
              <Loader2 className={cn('mt-0.5 shrink-0 text-primary', isProcessing && 'animate-spin')} size={18} />
              <div>
                <h2 className="font-semibold text-foreground">We're working on your feedback</h2>
                <p className="mt-1 text-sm text-muted-foreground">
                  You can refresh or come back later. Your results will be waiting for you.
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
                  role="button"
                  tabIndex={0}
                  onClick={() => openFeedbackNote(note)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                      event.preventDefault()
                      openFeedbackNote(note)
                    }
                  }}
                  className={cn(
                    'cursor-pointer rounded-xl border p-4 transition-transform hover:-translate-y-0.5 hover:shadow-md',
                    severityColors[normalizeSeverity(note.severity) as keyof typeof severityColors] || 'bg-gray-100 text-gray-700 border-gray-200',
                  )}
                >
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex-1">
                      <div className="mb-1 flex items-center gap-2">
                        <span className="font-semibold capitalize">{note.title ?? note.category}</span>
                        <span className="rounded bg-black/10 px-2 py-0.5 text-xs font-medium">
                          {note.severity}
                        </span>
                      </div>
                      <p className="text-xs uppercase tracking-wide opacity-70">{note.category}</p>
                      <p className="mt-1 text-sm">{note.message}</p>
                    </div>
                    {note.timestampSeconds != null && (
                      <button
                        onClick={(event) => {
                          event.stopPropagation()
                          openFeedbackNote(note)
                        }}
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

        {selectedFeedbackNote && (
          <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 p-4 backdrop-blur-sm"
            onClick={closeFeedbackNote}
          >
            <div
              className="w-full max-w-2xl rounded-3xl bg-white p-6 shadow-2xl"
              onClick={(event) => event.stopPropagation()}
            >
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                    {selectedFeedbackNote.category}
                  </p>
                  <h3 className="mt-1 text-2xl font-semibold text-foreground">
                    {selectedFeedbackNote.title ?? 'Feedback detail'}
                  </h3>
                  <p className="mt-2 text-sm text-muted-foreground">
                    Severity: <span className="font-medium text-foreground">{selectedFeedbackNote.severity}</span>
                  </p>
                  {selectedFeedbackNote.timestampSeconds != null && (
                    <p className="mt-1 text-sm text-muted-foreground">
                      Timestamp: <span className="font-medium text-foreground">{formatTimestamp(selectedFeedbackNote.timestampSeconds)}</span>
                      {selectedFeedbackNote.endTimestampSeconds != null && (
                        <> to <span className="font-medium text-foreground">{formatTimestamp(selectedFeedbackNote.endTimestampSeconds)}</span></>
                      )}
                    </p>
                  )}
                </div>
                <button
                  onClick={closeFeedbackNote}
                  className="rounded-full p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                  aria-label="Close feedback detail"
                >
                  <X size={18} />
                </button>
              </div>

              <div className="mt-5 rounded-2xl bg-muted/35 p-4">
                <p className="text-sm font-semibold text-foreground">What this means</p>
                <p className="mt-2 text-sm leading-6 text-muted-foreground">
                  {selectedFeedbackNote.message}
                </p>
              </div>

              {selectedFeedbackNote.coachScript && (
                <div className="mt-4 rounded-2xl border border-primary/10 bg-primary/5 p-4">
                  <p className="text-sm font-semibold text-foreground">Coach script</p>
                  <p className="mt-2 text-sm leading-6 text-muted-foreground">
                    {selectedFeedbackNote.coachScript}
                  </p>
                </div>
              )}

              <div className="mt-6 flex flex-wrap gap-2">
                {selectedFeedbackNote.timestampSeconds != null && (
                  <Button
                    onClick={() => {
                      seekToTimestamp(selectedFeedbackNote.timestampSeconds!)
                    }}
                  >
                    Jump to this moment
                  </Button>
                )}
                <Button variant="secondary" onClick={closeFeedbackNote}>
                  Close
                </Button>
              </div>
            </div>
          </div>
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
