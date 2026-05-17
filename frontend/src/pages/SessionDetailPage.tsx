import {
    AlertCircle,
    ArrowLeft,
    Bot,
    CheckCircle,
    Clock,
    Loader2,
    Maximize2,
    MessageSquare,
    Mic2,
    Minimize2,
    Pause,
    Play,
    RefreshCw,
    Rewind,
    FastForward,
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
import type {
  Evaluation,
  FeedbackNote,
  FeedbackResponse,
  PostureGestureSummary,
  PostureTimelineEvent,
  SegmentData,
} from '../types/evaluation'
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
const MIRRORED_VIDEO_STYLE = { transform: 'scaleX(-1)' } as const

const GUIDED_TRIGGER_SEVERITIES = new Set(['warning', 'suggestion', 'critical'])

type ProcessingState = {
  label: string
  description: string
  spinner: boolean
  tone: 'neutral' | 'accent' | 'success' | 'danger'
}

type ReviewLane = 'transcript' | 'comments' | 'status'

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

  if (evaluation?.status === 'completed' && !evaluation?.posture) {
    return {
      label: 'Analyzing body language',
      description: 'Speech analysis is done. Video posture analysis is still running in the background.',
      spinner: true,
      tone: 'accent',
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
  const [postureProgress, setPostureProgress] = useState<number>(0)
  const [currentTime, setCurrentTime] = useState(0)
  const [isPlaying, setIsPlaying] = useState(false)
  // Guided review (karaoke transcript + synced coach notes) is always on.
  const guidedModeEnabled = true
  const [activeCoachNote, setActiveCoachNote] = useState<FeedbackNote | null>(null)
  const [selectedFeedbackNote, setSelectedFeedbackNote] = useState<FeedbackNote | null>(null)
  const [dismissedNotes, setDismissedNotes] = useState<Set<number>>(new Set())
  const [mediaDuration, setMediaDuration] = useState<number | null>(null)
  const [isMediaReady, setIsMediaReady] = useState(false)
  const [isScrubbing, setIsScrubbing] = useState(false)
  const [isGuidedFullscreen, setIsGuidedFullscreen] = useState(false)
  const [splitPercent, setSplitPercent] = useState(66)
  const [activeReviewLane, setActiveReviewLane] = useState<ReviewLane>('transcript')
  const splitContainerRef = useRef<HTMLDivElement>(null)
  const isDraggingSplitRef = useRef(false)
  const isScrubbingRef = useRef(false)
  const streamAbortRef = useRef<AbortController | null>(null)
  const pendingSeekRef = useRef<number | null>(null)

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
          if (evalData?.posture) {
            setPostureProgress(0)
          }

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

  const posturePending = evaluation?.status === 'completed' && !evaluation?.posture

  const shouldPoll = Boolean(
    session && (
      session.status === 'uploaded' ||
      session.status === 'analyzing' ||
      evaluation?.status === 'pending' ||
      evaluation?.status === 'transcribing' ||
      evaluation?.status === 'analyzing' ||
      posturePending
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
      async ({ event, data }) => {
        if (event === 'posture_progress') {
          const progressData = data as { progress?: number }
          if (typeof progressData?.progress === 'number') {
            setPostureProgress(progressData.progress)
          }
          return
        }
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

  // Fallback polling while any processing state is active.
  // SSE handles real-time events; this interval catches cases where the stream drops or misses the completion event.
  useEffect(() => {
    if (!shouldPoll || !accessToken || !sessionId) return

    const interval = setInterval(() => {
      fetchData()
    }, 5000)

    return () => clearInterval(interval)
  }, [shouldPoll, fetchData, accessToken, sessionId])

  const reanalysisTriggeredRef = useRef(false)

  // Auto-trigger posture re-analysis when it's missing after a completed evaluation.
  // Give natural SSE/polling ~3s before requesting the backend to reprocess.
  useEffect(() => {
    if (!posturePending || !accessToken || !sessionId) {
      reanalysisTriggeredRef.current = false
      return
    }

    if (reanalysisTriggeredRef.current) return

    const timer = setTimeout(() => {
      reanalysisTriggeredRef.current = true
      api.reanalyzePosture(accessToken, sessionId).catch(() => {
        reanalysisTriggeredRef.current = false
      })
    }, 3000)

    return () => clearTimeout(timer)
  }, [posturePending, accessToken, sessionId])

  function handleRefresh() {
    setIsRefreshing(true)
    fetchData()
  }

  function seekToTimestamp(seconds: number) {
    pendingSeekRef.current = seconds
    setCurrentTime(seconds)

    if (!mediaRef.current) {
      return
    }

    if (isMediaReady || mediaRef.current.readyState >= HTMLMediaElement.HAVE_METADATA) {
      mediaRef.current.currentTime = seconds
      previousTimeRef.current = seconds
      pendingSeekRef.current = null
    }
  }

  function openFeedbackNote(note: FeedbackNote) {
    if (note.timestampSeconds != null) {
      seekToTimestamp(note.timestampSeconds)
    }
    mediaRef.current?.pause()
    setIsPlaying(false)
    setSelectedFeedbackNote(note)
  }

  function closeFeedbackNote() {
    setSelectedFeedbackNote(null)
  }

  const visibleCoachNote = selectedFeedbackNote ?? activeCoachNote

  function normalizeSeverity(severity: string | null | undefined) {
    return severity?.toLowerCase() ?? 'info'
  }

  const feedbackNotes = useMemo<FeedbackNote[]>(() => feedback?.notes ?? [], [feedback])
  const transcriptSegments = useMemo<SegmentData[]>(
    () => evaluation?.transcription?.segments ?? [],
    [evaluation],
  )
  const postureSummaries = useMemo<PostureGestureSummary[]>(() => {
    const raw = evaluation?.posture?.gestureSummariesJson
    if (!raw) return []
    try {
      const parsed = JSON.parse(raw)
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  }, [evaluation?.posture?.gestureSummariesJson])
  const postureTimeline = useMemo<PostureTimelineEvent[]>(() => {
    const raw = evaluation?.posture?.timelineJson
    if (!raw) return []
    try {
      const parsed = JSON.parse(raw)
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  }, [evaluation?.posture?.timelineJson])
  const posturePenaltyBreakdown = useMemo<Record<string, number>>(() => {
    const raw = evaluation?.posture?.penaltyBreakdownJson
    if (!raw) return {}
    try {
      const parsed = JSON.parse(raw)
      return parsed && typeof parsed === 'object' ? parsed : {}
    } catch {
      return {}
    }
  }, [evaluation?.posture?.penaltyBreakdownJson])
  const postureRecommendations = useMemo<string[]>(() => {
    const raw = evaluation?.posture?.recommendationsJson
    if (!raw) return []
    try {
      const parsed = JSON.parse(raw)
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  }, [evaluation?.posture?.recommendationsJson])

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
    if (!isGuidedFullscreen) return
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setIsGuidedFullscreen(false)
    }
    window.addEventListener('keydown', onKey)
    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', onKey)
    }
  }, [isGuidedFullscreen])

  useEffect(() => {
    if (!guidedModeEnabled && isGuidedFullscreen) {
      setIsGuidedFullscreen(false)
    }
  }, [guidedModeEnabled, isGuidedFullscreen])

  useEffect(() => {
    const container = transcriptContainerRef.current
    if (!guidedModeEnabled || activeSegmentIndex < 0 || !container) {
      return
    }

    const activeNode = container.querySelector<HTMLElement>(`[data-segment-index="${activeSegmentIndex}"]`)
    if (!activeNode) {
      return
    }

    // Scroll only the transcript container (not the page) so the active segment
    // sits roughly in the middle of the viewport.
    const containerRect = container.getBoundingClientRect()
    const nodeRect = activeNode.getBoundingClientRect()
    const nodeOffsetTop = nodeRect.top - containerRect.top + container.scrollTop
    const targetScroll = nodeOffsetTop - container.clientHeight / 2 + nodeRect.height / 2

    container.scrollTo({
      top: Math.max(0, targetScroll),
      behavior: 'smooth',
    })
  }, [activeSegmentIndex, guidedModeEnabled, isGuidedFullscreen])

  function handleTimeUpdate() {
    if (!mediaRef.current) return

    const time = mediaRef.current.currentTime

    // While the user is actively dragging the seek slider, the slider's local
    // value drives currentTime. Ignore transient timeupdate events so the thumb
    // doesn't snap back to wherever the underlying video currently reports.
    if (!isScrubbingRef.current) {
      setCurrentTime(time)
    }

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
    if (!isScrubbingRef.current) {
      setCurrentTime(time)
    }

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

  function applyPendingSeek() {
    if (!mediaRef.current || pendingSeekRef.current == null) {
      return
    }

    const nextSeek = pendingSeekRef.current
    mediaRef.current.currentTime = nextSeek
    previousTimeRef.current = nextSeek
    setCurrentTime(nextSeek)
    pendingSeekRef.current = null
  }

  function handleMediaReady() {
    setIsMediaReady(true)
    setMediaDuration(mediaRef.current?.duration ?? null)
    applyPendingSeek()
  }

  function startGuidedReview() {
    if (!mediaRef.current) {
      return
    }

    setIsGuidedFullscreen(true)
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
  const importantMoments = useMemo(() => {
    const spokenMoments = feedbackNotes
      .filter((note): note is FeedbackNote & { timestampSeconds: number } => note.timestampSeconds != null)
      .map((note) => ({
        id: `feedback-${note.timestampSeconds}-${note.category}-${note.title ?? note.message}`,
        timestamp: note.timestampSeconds,
        label: note.title ?? note.category,
        detail: note.message,
        severity: normalizeSeverity(note.severity),
        source: 'feedback' as const,
        note,
      }))

    const bodyMoments = postureTimeline.map((event, index) => ({
      id: `posture-${event.sec}-${event.gesture}-${index}`,
      timestamp: event.sec,
      label: event.gesture.replace(/_/g, ' '),
      detail: `${event.severity} posture signal · penalty ${event.penalty.toFixed(1)}`,
      severity: event.severity.toLowerCase(),
      source: 'posture' as const,
      event,
    }))

    return [...spokenMoments, ...bodyMoments]
      .sort((a, b) => a.timestamp - b.timestamp)
      .slice(0, 12)
  }, [feedbackNotes, postureTimeline])

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
  const currentDuration = mediaDuration ?? session?.mediaFile?.durationSeconds ?? evaluation?.transcription?.durationSeconds ?? null

  return (
    <div className="px-4 pb-6 pt-28 sm:px-6 sm:pb-8 lg:px-8 lg:pb-12">
      <div className="mx-auto w-full max-w-7xl space-y-4 sm:space-y-5 lg:space-y-6">
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
                aria-label="Refresh session"
                title="Refresh session"
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
              <p className="text-sm text-muted-foreground md:max-w-xs md:text-right">
                Progress is saved automatically — you can leave and come back anytime.
              </p>
            </div>

            {isProcessing && (
              <div className="mt-4 grid gap-3 sm:grid-cols-3">
                <div className="rounded-xl bg-white/70 p-3">
                  <p className="text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">Step 1</p>
                  <p className="mt-1 text-sm font-semibold text-foreground">Upload received</p>
                </div>
                <div className="rounded-xl bg-white/70 p-3">
                  <p className="text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">Step 2</p>
                  <p className="mt-1 text-sm font-semibold text-foreground">Transcribing</p>
                </div>
                <div className="rounded-xl bg-white/70 p-3">
                  <p className="text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">Step 3</p>
                  <p className="mt-1 text-sm font-semibold text-foreground">Coaching feedback</p>
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
          <div
            ref={splitContainerRef}
            style={
              isGuidedFullscreen
                ? {
                    marginTop: 0,
                    // On lg+ this CSS var drives the 3-column split (left | 1px divider | right).
                    // Mobile falls back to the `grid-cols-1` class below.
                    ['--split-cols' as string]: `minmax(0, ${splitPercent}fr) 1px minmax(0, ${100 - splitPercent}fr)`,
                  }
                : undefined
            }
            className={cn(
              isGuidedFullscreen
                ? 'fixed inset-0 z-50 overflow-hidden bg-[#fafaf7]'
                : 'grid gap-4',
              guidedModeEnabled && !isGuidedFullscreen && 'lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]',
              isGuidedFullscreen && 'grid h-screen max-h-screen w-screen grid-cols-1 gap-0 lg:[grid-template-columns:var(--split-cols)]',
            )}
            role={isGuidedFullscreen ? 'dialog' : undefined}
            aria-modal={isGuidedFullscreen || undefined}
            aria-label={isGuidedFullscreen ? 'Guided review fullscreen' : undefined}
          >
            <Card
              className={cn(
                guidedModeEnabled && 'ring-1 ring-primary/30',
                isGuidedFullscreen && 'flex h-full min-h-0 flex-col overflow-hidden rounded-none border-0 p-3 sm:p-4',
              )}
            >
              <div className="mb-4 flex shrink-0 flex-wrap items-center justify-between gap-3">
                <h2 className="display-font text-lg font-semibold text-foreground">
                  {isAudioMedia ? <Mic2 className="mr-2 inline-block" size={20} /> : <Video className="mr-2 inline-block" size={20} />}
                  {isAudioMedia ? 'Audio recording' : 'Recording'}
                </h2>
                <div className="flex flex-wrap gap-2">
                  <Button
                    variant="secondary"
                    onClick={skipToNextGuidedMoment}
                    disabled={guidedNotes.length === 0}
                  >
                    <SkipForward size={18} />
                    Next moment
                  </Button>
                  <Button
                    onClick={() => {
                      if (!isGuidedFullscreen) {
                        startGuidedReview()
                      } else {
                        setIsGuidedFullscreen(false)
                      }
                    }}
                    aria-label={isGuidedFullscreen ? 'Exit focus mode (Esc)' : 'Open focus mode'}
                    title={isGuidedFullscreen ? 'Exit focus mode (Esc)' : 'Open focus mode'}
                  >
                    {isGuidedFullscreen ? <Minimize2 size={18} /> : <Maximize2 size={18} />}
                    {isGuidedFullscreen ? 'Exit focus' : 'Focus mode'}
                  </Button>
                </div>
              </div>

              <div
                className={cn(
                  'overflow-hidden rounded-xl border border-slate-900/10 bg-slate-950 shadow-sm',
                  isGuidedFullscreen && 'mx-auto flex w-full max-w-4xl shrink-0 flex-col',
                )}
              >
                {isAudioMedia ? (
                  <>
                    <audio
                      ref={(node) => {
                        mediaRef.current = node
                      }}
                      src={mediaUrl}
                      className="hidden"
                      preload="metadata"
                      onLoadedMetadata={handleMediaReady}
                      onCanPlay={handleMediaReady}
                      onDurationChange={handleMediaReady}
                      onTimeUpdate={handleTimeUpdate}
                      onSeeking={handleSeeking}
                      onSeeked={handleSeeked}
                      onPlay={handlePlay}
                      onPause={handlePause}
                    >
                      Your browser does not support audio playback.
                    </audio>
                    <div className="flex h-48 items-center justify-center bg-gradient-to-br from-slate-900 to-slate-800 sm:h-56">
                      <div className="flex flex-col items-center gap-3 text-slate-200">
                        <div className="flex h-16 w-16 items-center justify-center rounded-full bg-white/10 ring-1 ring-white/20">
                          <Mic2 size={28} aria-hidden="true" />
                        </div>
                        <p className="text-sm font-medium">Audio recording</p>
                      </div>
                    </div>
                  </>
                ) : (
                  <div
                    className={cn(
                      'flex w-full items-center justify-center bg-black overflow-hidden',
                      isGuidedFullscreen
                        ? 'shrink-0 h-[42vh] lg:h-[45vh]'
                        : guidedModeEnabled
                          ? 'h-[55vh] lg:h-[50vh]'
                          : 'h-[65vh] lg:h-[60vh]',
                    )}
                  >
                    <video
                      ref={(node) => {
                        mediaRef.current = node
                      }}
                      src={mediaUrl}
                      className="h-full w-full object-contain"
                      style={MIRRORED_VIDEO_STYLE}
                      preload="metadata"
                      playsInline
                      onLoadedMetadata={handleMediaReady}
                      onCanPlay={handleMediaReady}
                      onDurationChange={handleMediaReady}
                      onTimeUpdate={handleTimeUpdate}
                      onSeeking={handleSeeking}
                      onSeeked={handleSeeked}
                      onPlay={handlePlay}
                      onPause={handlePause}
                    >
                      Your browser does not support video playback.
                    </video>
                  </div>
                )}

                <div className="shrink-0 bg-slate-900/95 px-3 py-3 sm:px-4">
                  <input
                    type="range"
                    min={0}
                    max={Math.max(0, currentDuration ?? 0)}
                    step={0.01}
                    value={Math.min(currentTime, currentDuration ?? currentTime)}
                    aria-label="Seek playback"
                    aria-valuemin={0}
                    aria-valuemax={Math.max(0, currentDuration ?? 0)}
                    aria-valuenow={Math.min(currentTime, currentDuration ?? currentTime)}
                    aria-valuetext={formatTimestamp(currentTime)}
                    onPointerDown={(event) => {
                      isScrubbingRef.current = true
                      setIsScrubbing(true)
                      handleSeeking()
                      event.currentTarget.setPointerCapture(event.pointerId)
                    }}
                    onPointerUp={(event) => {
                      isScrubbingRef.current = false
                      setIsScrubbing(false)
                      try {
                        event.currentTarget.releasePointerCapture(event.pointerId)
                      } catch {
                        // ignore
                      }
                    }}
                    onPointerCancel={() => {
                      isScrubbingRef.current = false
                      setIsScrubbing(false)
                    }}
                    onChange={(event) => {
                      const nextTime = Number(event.target.value)
                      if (!Number.isNaN(nextTime)) {
                        seekToTimestamp(nextTime)
                      }
                    }}
                    style={{
                      background: `linear-gradient(to right, hsl(var(--primary)) 0%, hsl(var(--primary)) ${
                        currentDuration && currentDuration > 0
                          ? Math.min(100, (currentTime / currentDuration) * 100)
                          : 0
                      }%, rgba(255,255,255,0.18) ${
                        currentDuration && currentDuration > 0
                          ? Math.min(100, (currentTime / currentDuration) * 100)
                          : 0
                      }%, rgba(255,255,255,0.18) 100%)`,
                    }}
                    className="h-1.5 w-full cursor-pointer appearance-none rounded-full accent-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/60"
                  />

                  <div className="mt-3 flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => {
                        if (!mediaRef.current) return
                        if (isPlaying) {
                          mediaRef.current.pause()
                        } else {
                          void mediaRef.current.play()
                        }
                      }}
                      disabled={!mediaRef.current}
                      aria-label={isPlaying ? 'Pause' : 'Play'}
                      title={isPlaying ? 'Pause' : 'Play'}
                      className="inline-flex h-10 w-10 items-center justify-center rounded-full bg-white text-slate-900 transition-colors hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/80 disabled:pointer-events-none disabled:opacity-40"
                    >
                      {isPlaying ? <Pause size={18} /> : <Play size={18} className="translate-x-0.5" />}
                    </button>

                    <button
                      type="button"
                      onClick={() => seekToTimestamp(Math.max(0, currentTime - 5))}
                      disabled={!mediaRef.current}
                      aria-label="Rewind 5 seconds"
                      title="Rewind 5 seconds"
                      className="inline-flex h-9 w-9 items-center justify-center rounded-full text-slate-200 transition-colors hover:bg-white/10 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/70 disabled:pointer-events-none disabled:opacity-40"
                    >
                      <Rewind size={18} />
                    </button>

                    <button
                      type="button"
                      onClick={() => seekToTimestamp(Math.min(currentDuration ?? currentTime + 5, currentTime + 5))}
                      disabled={!mediaRef.current}
                      aria-label="Forward 5 seconds"
                      title="Forward 5 seconds"
                      className="inline-flex h-9 w-9 items-center justify-center rounded-full text-slate-200 transition-colors hover:bg-white/10 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/70 disabled:pointer-events-none disabled:opacity-40"
                    >
                      <FastForward size={18} />
                    </button>

                    <div className="ml-auto font-mono text-xs tabular-nums text-slate-300">
                      {formatTimestamp(currentTime)}
                      <span className="mx-1 text-slate-500">/</span>
                      {currentDuration != null ? formatTimestamp(currentDuration) : '--:--'}
                    </div>
                  </div>
                </div>

              </div>

              <div
                className={cn(
                  'mt-3 overflow-hidden rounded-2xl border border-border bg-background/90 shadow-sm transition-all duration-300 ease-out',
                  isGuidedFullscreen
                    ? visibleCoachNote
                      ? 'min-h-0 flex-1 opacity-100 translate-y-0 overflow-y-auto'
                      : 'pointer-events-none h-0 opacity-0 -translate-y-2'
                    : visibleCoachNote
                      ? 'shrink-0 max-h-96 opacity-100 translate-y-0'
                      : 'pointer-events-none shrink-0 max-h-0 opacity-0 -translate-y-2',
                )}
              >
                {visibleCoachNote && (
                  <div className="p-4 sm:p-5">
                    <div className="flex gap-3 sm:gap-4">
                      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-cyan-500/15 text-cyan-700 ring-1 ring-cyan-500/20 sm:h-12 sm:w-12">
                        <Bot size={20} />
                      </div>

                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-center justify-between gap-2">
                          <div>
                            <h4 className="text-xs font-bold uppercase tracking-[0.14em] text-cyan-700 sm:text-sm">
                              {visibleCoachNote.title ?? 'Coach Insight'}
                            </h4>
                            <p className="mt-1 text-xs text-muted-foreground sm:text-sm">
                              {visibleCoachNote.category} · {visibleCoachNote.severity}
                              {visibleCoachNote.timestampSeconds != null && (
                                <> · {formatTimestamp(visibleCoachNote.timestampSeconds)}</>
                              )}
                            </p>
                          </div>

                          <div className="flex flex-wrap gap-2">
                            <Button
                              variant="secondary"
                              onClick={replayRecentClip}
                              disabled={!mediaRef.current}
                            >
                              Rewind 5s
                            </Button>
                            <Button
                              variant="ghost"
                              onClick={() => openFeedbackNote(visibleCoachNote)}
                              className="border border-border bg-white/60"
                            >
                              Open note
                            </Button>
                          </div>
                        </div>

                        <div className="mt-3 rounded-2xl bg-muted/35 p-4 text-sm leading-relaxed text-foreground">
                          <TypewriterText
                            key={`${visibleCoachNote.timestampSeconds ?? 'none'}-${visibleCoachNote.coachScript ?? visibleCoachNote.message}`}
                            text={visibleCoachNote.coachScript ?? visibleCoachNote.message}
                          />
                        </div>

                        <div className="mt-4 flex flex-wrap gap-2">
                          <Button onClick={continueGuidedReview}>
                            Continue
                          </Button>
                          <Button
                            variant="secondary"
                            onClick={() => setSelectedFeedbackNote(null)}
                          >
                            Dismiss
                          </Button>
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {guidedModeEnabled && playbackDuration && guidedNotes.length > 0 && (
                <div className="mt-4 shrink-0">
                  <div className="relative h-2 rounded-full bg-muted">
                    {guidedNotes.map((note, index) => {
                      const left = Math.min(100, Math.max(0, (note.timestampSeconds / playbackDuration) * 100))
                      const isActive = activeCoachNote?.timestampSeconds === note.timestampSeconds
                      return (
                        <button
                          key={`${note.timestampSeconds}-${index}`}
                          type="button"
                          onClick={() => openFeedbackNote(note)}
                          className={cn(
                            'absolute top-1/2 h-3 w-3 -translate-x-1/2 -translate-y-1/2 rounded-full border border-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/60',
                            isActive ? 'bg-primary shadow-glow' : 'bg-cyan-500/90',
                          )}
                          style={{ left: `${left}%` }}
                          aria-label={`Coaching moment at ${formatTimestamp(note.timestampSeconds)}`}
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

            {guidedModeEnabled && isGuidedFullscreen && (
              <div
                role="separator"
                aria-orientation="vertical"
                aria-label="Resize panels"
                tabIndex={0}
                onPointerDown={(e) => {
                  isDraggingSplitRef.current = true
                  e.currentTarget.setPointerCapture(e.pointerId)
                }}
                onPointerMove={(e) => {
                  if (!isDraggingSplitRef.current || !splitContainerRef.current) return
                  const rect = splitContainerRef.current.getBoundingClientRect()
                  const pct = ((e.clientX - rect.left) / rect.width) * 100
                  setSplitPercent(Math.max(25, Math.min(85, pct)))
                }}
                onPointerUp={(e) => {
                  isDraggingSplitRef.current = false
                  e.currentTarget.releasePointerCapture(e.pointerId)
                }}
                onKeyDown={(e) => {
                  if (e.key === 'ArrowLeft') setSplitPercent((v) => Math.max(25, v - 2))
                  if (e.key === 'ArrowRight') setSplitPercent((v) => Math.min(85, v + 2))
                }}
                className="group relative hidden cursor-col-resize items-stretch justify-center bg-slate-200 transition-colors hover:bg-primary/40 lg:flex"
              >
                {/* Invisible expanded hit zone — 32px wide, receives drag events via bubbling */}
                <span className="absolute inset-y-0 -left-4 -right-4 cursor-col-resize" aria-hidden="true" />
                <span className="pointer-events-none my-auto h-14 w-1 rounded-full bg-slate-400 transition-colors group-hover:bg-primary" aria-hidden="true" />
              </div>
            )}
            {guidedModeEnabled && (
              <Card
                className={cn(
                  'flex min-h-0 flex-col',
                  isGuidedFullscreen ? 'h-full rounded-none border-0 p-3 sm:p-4' : 'lg:h-full lg:max-h-[60vh]',
                )}
              >
                <div className="mb-3 flex shrink-0 flex-wrap items-center justify-between gap-3">
                  <div>
                    <h3 className="display-font text-base font-semibold text-foreground">
                      Review Console
                    </h3>
                    <p className="text-xs text-muted-foreground">
                      Switch between transcript, reviewer notes, and AI/video status.
                    </p>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      onClick={() => setActiveReviewLane('transcript')}
                      className={cn(
                        'rounded-full px-3 py-1.5 text-sm transition-colors',
                        activeReviewLane === 'transcript'
                          ? 'bg-primary text-primary-foreground'
                          : 'bg-muted text-muted-foreground hover:bg-muted/80',
                      )}
                    >
                      Transcript
                    </button>
                    <button
                      type="button"
                      onClick={() => setActiveReviewLane('comments')}
                      className={cn(
                        'rounded-full px-3 py-1.5 text-sm transition-colors',
                        activeReviewLane === 'comments'
                          ? 'bg-primary text-primary-foreground'
                          : 'bg-muted text-muted-foreground hover:bg-muted/80',
                      )}
                    >
                      Reviewer Notes
                    </button>
                    <button
                      type="button"
                      onClick={() => setActiveReviewLane('status')}
                      className={cn(
                        'rounded-full px-3 py-1.5 text-sm transition-colors',
                        activeReviewLane === 'status'
                          ? 'bg-primary text-primary-foreground'
                          : 'bg-muted text-muted-foreground hover:bg-muted/80',
                      )}
                    >
                      AI Status
                    </button>
                  </div>
                </div>

                {activeReviewLane === 'transcript' && (
                  transcriptSegments.length > 0 ? (
                    <div
                      ref={transcriptContainerRef}
                      className={cn(
                        'min-h-0 flex-1 overflow-y-auto rounded-xl bg-muted/25 p-4 leading-7',
                        isGuidedFullscreen
                          ? 'text-base leading-8 lg:text-lg lg:leading-9'
                          : 'h-80 text-sm lg:h-auto',
                      )}
                    >
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
                  )
                )}

                {activeReviewLane === 'comments' && (
                  filteredNotes.length > 0 ? (
                    <div className={cn('min-h-0 flex-1 space-y-3 overflow-y-auto', !isGuidedFullscreen && 'h-80 lg:h-auto')}>
                      {filteredNotes.map((note, index) => (
                        <button
                          key={`${note.category}-${index}`}
                          type="button"
                          onClick={() => openFeedbackNote(note)}
                          className={cn(
                            'w-full rounded-2xl border p-4 text-left transition-transform hover:-translate-y-0.5 hover:shadow-md',
                            severityColors[normalizeSeverity(note.severity) as keyof typeof severityColors] || 'bg-gray-100 text-gray-700 border-gray-200',
                          )}
                        >
                          <div className="flex items-start justify-between gap-3">
                            <div>
                              <p className="text-xs uppercase tracking-[0.14em] opacity-70">{note.category}</p>
                              <p className="mt-1 font-semibold">{note.title ?? note.category}</p>
                              <p className="mt-2 text-sm">{note.message}</p>
                            </div>
                            {note.timestampSeconds != null && (
                              <span className="shrink-0 font-mono text-xs underline decoration-dotted">
                                {formatTimestamp(note.timestampSeconds)}
                              </span>
                            )}
                          </div>
                        </button>
                      ))}
                    </div>
                  ) : (
                    <p className="text-sm text-muted-foreground">
                      Reviewer notes will appear here after analysis completes.
                    </p>
                  )
                )}

                {activeReviewLane === 'status' && (
                  <div className={cn('min-h-0 flex-1 space-y-4 overflow-y-auto', !isGuidedFullscreen && 'h-80 lg:h-auto')}>
                    <div className="rounded-2xl border border-border bg-muted/25 p-4">
                      <div className="flex items-center justify-between gap-3">
                        <div>
                          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                            Pipeline
                          </p>
                          <p className="mt-1 text-base font-semibold text-foreground">{processingState?.label ?? 'Status unavailable'}</p>
                        </div>
                        <span className={cn(
                          'rounded-full px-3 py-1 text-xs font-medium',
                          processingState?.tone === 'success' && 'bg-emerald-100 text-emerald-700',
                          processingState?.tone === 'danger' && 'bg-rose-100 text-rose-700',
                          processingState?.tone === 'accent' && 'bg-cyan-100 text-cyan-700',
                          processingState?.tone === 'neutral' && 'bg-slate-100 text-slate-700',
                        )}>
                          {evaluation?.status ?? session.status}
                        </span>
                      </div>
                      <p className="mt-2 text-sm text-muted-foreground">{processingState?.description}</p>
                    </div>

                    <div className="grid gap-3 sm:grid-cols-2">
                      <div className="rounded-2xl border border-border bg-background p-4">
                        <p className="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">Media path</p>
                        <p className="mt-2 break-all text-sm text-foreground">
                          {session.mediaFile?.originalFileName ?? 'No media attached'}
                        </p>
                        <p className="mt-1 text-xs text-muted-foreground">
                          {session.mediaFile?.contentType ?? 'Unknown type'}
                        </p>
                      </div>
                      <div className="rounded-2xl border border-border bg-background p-4">
                        <p className="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">Slides</p>
                        <p className="mt-2 break-all text-sm text-foreground">
                          {session.slideFile?.originalFileName ?? 'No slides attached'}
                        </p>
                        <p className="mt-1 text-xs text-muted-foreground">
                          {session.slideFile?.contentType ?? 'PDF, PPT, or PPTX optional'}
                        </p>
                      </div>
                      <div className="rounded-2xl border border-border bg-background p-4">
                        <p className="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">Body language</p>
                        {posturePending ? (
                          <div className="mt-2 space-y-2">
                            <div className="flex items-center gap-2 text-sm text-muted-foreground">
                              <Loader2 className="animate-spin" size={14} />
                              Analyzing video posture...
                            </div>
                            {postureProgress > 0 && (
                              <div className="space-y-1">
                                <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
                                  <div
                                    className="h-full rounded-full bg-amber-500 transition-all duration-500"
                                    style={{ width: `${Math.round(postureProgress * 100)}%` }}
                                  />
                                </div>
                                <p className="text-xs text-muted-foreground">
                                  {Math.round(postureProgress * 100)}%
                                </p>
                              </div>
                            )}
                          </div>
                        ) : (
                          <>
                            <p className="mt-2 text-sm text-foreground">
                              {evaluation?.posture
                                ? `${evaluation.posture.grade} · ${evaluation.posture.score.toFixed(0)}/100`
                                : 'No completed video analysis yet'}
                            </p>
                            <p className="mt-1 text-xs text-muted-foreground">
                              {postureSummaries.length} gesture pattern(s) detected
                            </p>
                          </>
                        )}
                      </div>
                    </div>

                    {postureSummaries.length > 0 && (
                      <div className="rounded-2xl border border-border bg-background p-4">
                        <p className="text-sm font-semibold text-foreground">Detected gesture patterns</p>
                        <div className="mt-3 space-y-2">
                          {postureSummaries.slice(0, 5).map((summary) => (
                            <div key={`${summary.name}-${summary.first_seen_sec}`} className="flex items-center justify-between gap-3 rounded-xl bg-muted/30 px-3 py-2 text-sm">
                              <span className="capitalize text-foreground">{summary.name.replace(/_/g, ' ')}</span>
                              <span className="text-muted-foreground">
                                {summary.total_occurrences}x · {summary.total_seconds?.toFixed(1)}s
                              </span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {postureTimeline.length > 0 && (
                      <div className="rounded-2xl border border-border bg-background p-4">
                        <p className="text-sm font-semibold text-foreground">Posture moments</p>
                        <div className="mt-3 space-y-2">
                          {postureTimeline.slice(0, 6).map((event, index) => (
                            <button
                              key={`${event.sec}-${event.gesture}-${index}`}
                              type="button"
                              onClick={() => seekToTimestamp(event.sec)}
                              className="flex w-full items-center justify-between gap-3 rounded-xl bg-muted/30 px-3 py-2 text-left text-sm transition-colors hover:bg-muted/60"
                            >
                              <span className="capitalize text-foreground">
                                {event.gesture.replace(/_/g, ' ')}
                              </span>
                              <span className="font-mono text-muted-foreground">
                                {event.time_fmt ?? formatTimestamp(event.sec)}
                              </span>
                            </button>
                          ))}
                        </div>
                      </div>
                    )}

                    {Object.keys(posturePenaltyBreakdown).length > 0 && (
                      <div className="rounded-2xl border border-border bg-background p-4">
                        <p className="text-sm font-semibold text-foreground">Penalty breakdown</p>
                        <div className="mt-3 space-y-2">
                          {Object.entries(posturePenaltyBreakdown)
                            .sort((a, b) => b[1] - a[1])
                            .slice(0, 5)
                            .map(([gesture, penalty]) => (
                              <div key={gesture} className="flex items-center justify-between rounded-xl bg-muted/30 px-3 py-2 text-sm">
                                <span className="capitalize text-foreground">{gesture.replace(/_/g, ' ')}</span>
                                <span className="text-muted-foreground">{penalty.toFixed(1)} pts</span>
                              </div>
                            ))}
                        </div>
                      </div>
                    )}

                    {postureRecommendations.length > 0 && (
                      <div className="rounded-2xl border border-border bg-background p-4">
                        <p className="text-sm font-semibold text-foreground">AI recommendations</p>
                        <div className="mt-3 space-y-2">
                          {postureRecommendations.map((recommendation, index) => (
                            <div key={`${recommendation}-${index}`} className="rounded-xl bg-muted/30 px-3 py-2 text-sm text-muted-foreground">
                              {recommendation}
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {evaluation?.errorMessage && (
                      <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">
                        {evaluation.errorMessage}
                      </div>
                    )}
                  </div>
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

        {/* Score cards */}
        {evaluation?.status === 'completed' && (
          <div className="grid gap-4 sm:grid-cols-3">
            {/* Overall mark */}
            <Card className="border-2 border-primary/20 bg-primary/5">
              <div className="text-center">
                <p className="text-xs font-semibold uppercase tracking-[0.14em] text-muted-foreground">Overall</p>
                <p className="display-font mt-1 text-4xl font-bold text-foreground">
                  {evaluation.overallScore ?? '--'}
                </p>
                <p className="text-xs text-muted-foreground">/ 100</p>
              </div>
            </Card>

            {/* Voice / Clarity */}
            <Card>
              <div className="text-center">
                <div className="mb-1 flex items-center justify-center gap-1.5">
                  <Mic2 className="text-blue-600" size={16} />
                  <p className="text-xs font-semibold uppercase tracking-[0.14em] text-muted-foreground">Voice</p>
                </div>
                <p className="display-font text-3xl font-bold text-foreground">
                  {evaluation.metrics?.clarityScore?.toFixed(0) ?? '--'}
                </p>
                <p className="text-xs text-muted-foreground">/ 100</p>
              </div>
            </Card>

            {/* Posture */}
            <Card>
              <div className="text-center">
                <div className="mb-1 flex items-center justify-center gap-1.5">
                  <Video className="text-amber-600" size={16} />
                  <p className="text-xs font-semibold uppercase tracking-[0.14em] text-muted-foreground">Body</p>
                </div>
                {evaluation.posture ? (
                  <>
                    <p className="display-font text-3xl font-bold text-foreground">
                      {evaluation.posture.score?.toFixed(0)}
                    </p>
                    <p className="text-xs text-muted-foreground">/ 100 · {evaluation.posture.grade}</p>
                  </>
                ) : posturePending ? (
                  <div className="flex flex-col items-center gap-1.5 py-1">
                    <div className="flex items-center justify-center gap-1.5">
                      <Loader2 className="animate-spin text-muted-foreground" size={18} />
                      <span className="text-sm text-muted-foreground">Analyzing...</span>
                    </div>
                    {postureProgress > 0 && (
                      <div className="w-full max-w-[200px] space-y-1">
                        <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
                          <div
                            className="h-full rounded-full bg-amber-500 transition-all duration-500"
                            style={{ width: `${Math.round(postureProgress * 100)}%` }}
                          />
                        </div>
                        <p className="text-center text-xs text-muted-foreground">
                          {Math.round(postureProgress * 100)}%
                        </p>
                      </div>
                    )}
                  </div>
                ) : (
                  <>
                    <button
                      type="button"
                      onClick={async () => {
                        if (!accessToken || !sessionId) return
                        try {
                          await api.reanalyzePosture(accessToken, sessionId)
                          fetchData()
                        } catch { /* ignore */ }
                      }}
                      className="mt-1 rounded-lg bg-amber-100 px-3 py-1 text-xs font-medium text-amber-700 transition-colors hover:bg-amber-200"
                    >
                      <RefreshCw className="mr-1 inline-block" size={12} />
                      Re-analyze
                    </button>
                  </>
                )}
              </div>
            </Card>
          </div>
        )}

        {/* Detailed metrics */}
        {evaluation?.status === 'completed' && evaluation.metrics && (
          <div className="grid gap-4 sm:grid-cols-3">
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

        {evaluation?.status === 'completed' && importantMoments.length > 0 && (
          <Card>
            <div className="mb-4 flex items-center justify-between gap-3">
              <h2 className="display-font text-lg font-semibold text-foreground">
                Jump to Important Moments
              </h2>
              <p className="text-sm text-muted-foreground">Speech feedback and posture events in one rail.</p>
            </div>
            <div className="grid gap-3 lg:grid-cols-2">
              {importantMoments.map((moment) => (
                <button
                  key={moment.id}
                  type="button"
                  onClick={() => {
                    seekToTimestamp(moment.timestamp)
                    if (moment.source === 'feedback') {
                      openFeedbackNote(moment.note)
                    } else {
                      setActiveReviewLane('status')
                    }
                  }}
                  className="rounded-2xl border border-border bg-muted/20 p-4 text-left transition-transform hover:-translate-y-0.5 hover:shadow-md"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-[0.14em] text-muted-foreground">
                        {moment.source === 'feedback' ? 'Reviewer note' : 'Body language'}
                      </p>
                      <p className="mt-1 font-semibold capitalize text-foreground">{moment.label}</p>
                      <p className="mt-2 text-sm text-muted-foreground">{moment.detail}</p>
                    </div>
                    <span className="shrink-0 rounded-full bg-background px-2.5 py-1 font-mono text-xs text-foreground">
                      {formatTimestamp(moment.timestamp)}
                    </span>
                  </div>
                </button>
              ))}
            </div>
          </Card>
        )}

        {/* Posture Analysis */}
        {evaluation?.status === 'completed' && (
          <Card>
            <div className="mb-4 flex items-center justify-between">
              <h2 className="display-font text-lg font-semibold text-foreground">
                <Video className="mr-2 inline-block" size={20} />
                Body Language Analysis
              </h2>
              {evaluation.posture ? (
                <div className="flex items-center gap-3">
                  <span className={`text-3xl font-bold display-font ${
                    evaluation.posture.grade?.startsWith('A') ? 'text-green-600' :
                    evaluation.posture.grade?.startsWith('B') ? 'text-blue-600' :
                    evaluation.posture.grade?.startsWith('C') ? 'text-amber-600' : 'text-red-600'
                  }`}>
                    {evaluation.posture.grade}
                  </span>
                  <div className="text-right">
                    <p className="text-2xl font-semibold text-foreground">{evaluation.posture.score?.toFixed(0)}</p>
                    <p className="text-xs text-muted-foreground">/ 100</p>
                  </div>
                </div>
              ) : (
                <div className="flex items-center gap-2 rounded-full bg-accent/10 px-3 py-1.5 text-xs font-medium text-accent-foreground">
                  <Loader2 className="animate-spin" size={14} />
                  Analyzing
                </div>
              )}
            </div>

            {evaluation.posture ? (
              <>
                {postureSummaries.length > 0 && (
                  <div className="mb-4">
                    <p className="mb-2 text-sm font-medium text-foreground">Detected gestures</p>
                    <div className="space-y-2">
                      {postureSummaries.map((summary) => (
                        <div key={`${summary.name}-${summary.first_seen_sec}`} className="rounded-lg bg-muted/40 px-3 py-3 text-sm">
                          <div className="flex items-center justify-between gap-3">
                            <span className="capitalize text-foreground">{summary.name.replace(/_/g, ' ')}</span>
                            <span className="text-muted-foreground">
                              {summary.total_occurrences}x · {summary.total_seconds?.toFixed(1)}s
                            </span>
                          </div>
                          <div className="mt-2 flex items-center justify-between gap-3 text-xs text-muted-foreground">
                            <span>{summary.first_seen_fmt ?? formatTimestamp(summary.first_seen_sec)} to {summary.last_seen_fmt ?? formatTimestamp(summary.last_seen_sec)}</span>
                            <span>{summary.points_deducted?.toFixed(1)} pts</span>
                          </div>
                          {summary.description && (
                            <p className="mt-2 text-xs text-muted-foreground">{summary.description}</p>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {postureTimeline.length > 0 && (
                  <div className="mb-4">
                    <p className="mb-2 text-sm font-medium text-foreground">Timeline moments</p>
                    <div className="grid gap-2 md:grid-cols-2">
                      {postureTimeline.slice(0, 8).map((event, index) => (
                        <button
                          key={`${event.sec}-${event.gesture}-${index}`}
                          type="button"
                          onClick={() => seekToTimestamp(event.sec)}
                          className="flex items-center justify-between rounded-lg bg-muted/40 px-3 py-2 text-left text-sm transition-colors hover:bg-muted/70"
                        >
                          <div>
                            <p className="capitalize text-foreground">{event.gesture.replace(/_/g, ' ')}</p>
                            <p className="text-xs text-muted-foreground">{event.severity} · person {event.person_id}</p>
                          </div>
                          <span className="font-mono text-xs text-muted-foreground">
                            {event.time_fmt ?? formatTimestamp(event.sec)}
                          </span>
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {postureRecommendations.length > 0 && (
                  <div>
                    <p className="mb-2 text-sm font-medium text-foreground">Recommendations</p>
                    <ul className="space-y-1.5">
                      {postureRecommendations.map((rec, i) => (
                        <li key={i} className="flex items-start gap-2 text-sm text-muted-foreground">
                          <CheckCircle className="mt-0.5 shrink-0 text-green-500" size={15} />
                          {rec}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {postureSummaries.length === 0 && postureTimeline.length === 0 && (
                  <p className="text-sm text-muted-foreground">
                    No body language issues detected. Great posture throughout!
                  </p>
                )}

                {evaluation.posture?.renderedVideoUrl && (() => {
                  const videoSrc = evaluation.posture.renderedVideoUrl.startsWith('http')
                    ? evaluation.posture.renderedVideoUrl
                    : `${API_BASE_URL}${evaluation.posture.renderedVideoUrl}`
                  return (
                    <div className="mt-4">
                      <p className="mb-2 text-sm font-medium text-foreground">Annotated analysis video</p>
                      <div className="overflow-hidden rounded-xl border border-border bg-black">
                        <video
                          controls
                          className="w-full"
                          src={videoSrc}
                          style={{ maxHeight: 360 }}
                          onError={() => {
                            const el = document.getElementById('annotated-video-fallback')
                            if (el) el.style.display = 'block'
                          }}
                        >
                          Your browser does not support video playback.
                        </video>
                        <div
                          id="annotated-video-fallback"
                          style={{ display: 'none' }}
                          className="p-3 text-sm text-muted-foreground"
                        >
                          Video could not be loaded.{' '}
                          <a href={videoSrc} download className="underline text-primary">Download it</a> to watch locally.
                        </div>
                      </div>
                    </div>
                  )
                })()}
              </>
            ) : (
              <div className="space-y-4">
                <p className="text-sm text-muted-foreground">
                  Video posture analysis is running in the background. This card will update automatically once results are ready.
                </p>
                <div className="animate-pulse space-y-3">
                  <div className="h-4 w-2/3 rounded bg-muted" />
                  <div className="h-4 w-1/2 rounded bg-muted" />
                  <div className="h-4 w-3/4 rounded bg-muted" />
                  <div className="mt-4 grid gap-2 md:grid-cols-2">
                    <div className="h-10 rounded bg-muted" />
                    <div className="h-10 rounded bg-muted" />
                    <div className="h-10 rounded bg-muted" />
                    <div className="h-10 rounded bg-muted" />
                  </div>
                </div>
              </div>
            )}
          </Card>
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
          <div className="mt-4 rounded-2xl border border-border bg-card p-5 shadow-sm">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                  Detail view
                </p>
                <h3 className="mt-1 text-xl font-semibold text-foreground">
                  {selectedFeedbackNote.title ?? 'Feedback detail'}
                </h3>
                <p className="mt-2 text-sm text-muted-foreground">
                  {selectedFeedbackNote.category} · {selectedFeedbackNote.severity}
                  {selectedFeedbackNote.timestampSeconds != null && (
                    <> · {formatTimestamp(selectedFeedbackNote.timestampSeconds)}</>
                  )}
                </p>
              </div>
              <button
                onClick={closeFeedbackNote}
                className="rounded-full p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                aria-label="Close feedback detail"
              >
                <X size={18} />
              </button>
            </div>

            <div className="mt-4 rounded-2xl bg-muted/35 p-4">
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

            <div className="mt-5 flex flex-wrap gap-2">
              {selectedFeedbackNote.timestampSeconds != null && (
                <Button onClick={() => seekToTimestamp(selectedFeedbackNote.timestampSeconds!)}>
                  Jump to this moment
                </Button>
              )}
              <Button variant="secondary" onClick={closeFeedbackNote}>
                Close
              </Button>
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
