import {
  AlertCircle,
  CheckCircle,
  Clock,
  Loader2,
  Mic2,
  Plus,
  Trash2,
  Video,
} from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AUTH_BYPASS_ENABLED } from '../auth/testing-auth'
import { useAuth } from '../auth/useAuth'
import { AppHeader } from '../components/AppHeader'
import { api, ApiClientError } from '../lib/api'
import { cn } from '../lib/cn'
import type { Session, SessionStatus } from '../types/sessions'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'

const statusConfig: Record<SessionStatus, { icon: typeof Clock; color: string; label: string }> = {
  draft: { icon: Clock, color: 'text-muted-foreground', label: 'Draft' },
  uploaded: { icon: Video, color: 'text-blue-500', label: 'Uploaded' },
  analyzing: { icon: Loader2, color: 'text-amber-500', label: 'Analyzing' },
  completed: { icon: CheckCircle, color: 'text-green-500', label: 'Completed' },
  failed: { icon: AlertCircle, color: 'text-red-500', label: 'Failed' },
}

export function SessionsPage() {
  const navigate = useNavigate()
  const { accessToken, logout } = useAuth()
  const [sessions, setSessions] = useState<Session[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const fetchSessions = useCallback(async () => {
    if (!accessToken) return
    try {
      const response = await api.getSessions(accessToken)
      setSessions(response.sessions)
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
      setError(err instanceof Error ? err.message : 'Failed to load sessions')
    } finally {
      setIsLoading(false)
    }
  }, [accessToken, logout, navigate])

  useEffect(() => {
    fetchSessions()
  }, [fetchSessions])

  async function handleDelete(sessionId: string) {
    if (!accessToken) return
    setDeletingId(sessionId)
    try {
      await api.deleteSession(accessToken, sessionId)
      setSessions((prev) => prev.filter((s) => s.id !== sessionId))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete session')
    } finally {
      setDeletingId(null)
    }
  }

  function formatDate(dateString: string) {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    })
  }

  function formatDuration(seconds: number | null) {
    if (!seconds) return '--:--'
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  return (
    <div className="px-4 pb-6 pt-28 sm:px-6 sm:pb-8 lg:px-8 lg:pb-12">
      <div className="mx-auto w-full max-w-6xl space-y-6">
        <AppHeader />

        <div className="flex items-center justify-between">
          <div>
            <h1 className="display-font text-2xl font-semibold text-foreground">
              Practice Sessions
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Record and analyze your presentations
            </p>
          </div>
          <Link to="/sessions/new">
            <Button>
              <Plus size={18} />
              New Session
            </Button>
          </Link>
        </div>

        {error && (
          <Card className="border-red-200 bg-red-50">
            <div className="flex items-center gap-2 text-red-600">
              <AlertCircle size={18} />
              <p>{error}</p>
            </div>
          </Card>
        )}

        {isLoading ? (
          <Card>
            <div className="flex items-center justify-center py-12">
              <Loader2 className="animate-spin text-primary" size={32} />
            </div>
          </Card>
        ) : sessions.length === 0 ? (
          <Card>
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <Mic2 className="mb-4 text-muted-foreground/50" size={48} />
              <h2 className="display-font text-lg font-semibold text-foreground">
                No sessions yet
              </h2>
              <p className="mt-2 max-w-sm text-sm text-muted-foreground">
                Create your first practice session to start improving your presentation skills.
              </p>
              <Link to="/sessions/new" className="mt-6">
                <Button>
                  <Plus size={18} />
                  Create First Session
                </Button>
              </Link>
            </div>
          </Card>
        ) : (
          <div className="space-y-3">
            {sessions.map((session) => {
              const StatusIcon = statusConfig[session.status].icon
              const statusColor = statusConfig[session.status].color
              const statusLabel = statusConfig[session.status].label

              return (
                <Card
                  key={session.id}
                  className="transition-all hover:border-primary/30 hover:shadow-md"
                >
                  <div className="flex items-center justify-between gap-4">
                    <Link
                      to={`/sessions/${session.id}`}
                      className="flex min-w-0 flex-1 items-center gap-4"
                    >
                      <div
                        className={cn(
                          'flex h-12 w-12 shrink-0 items-center justify-center rounded-xl',
                          session.status === 'completed'
                            ? 'bg-green-100'
                            : session.status === 'analyzing'
                              ? 'bg-amber-100'
                              : 'bg-muted/50',
                        )}
                      >
                        <StatusIcon
                          className={cn(
                            statusColor,
                            session.status === 'analyzing' && 'animate-spin',
                          )}
                          size={24}
                        />
                      </div>
                      <div className="min-w-0 flex-1">
                        <h3 className="display-font truncate font-semibold text-foreground">
                          {session.title}
                        </h3>
                        <div className="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-muted-foreground">
                          <span className="capitalize">{session.sessionType.toLowerCase()}</span>
                          <span>{formatDate(session.createdAt)}</span>
                          {session.mediaFile?.durationSeconds && (
                            <span>{formatDuration(session.mediaFile.durationSeconds)}</span>
                          )}
                          <span className={statusColor}>{statusLabel}</span>
                        </div>
                      </div>
                    </Link>
                    <Button
                      variant="ghost"
                      className="shrink-0 text-muted-foreground hover:text-red-500"
                      onClick={() => handleDelete(session.id)}
                      disabled={deletingId === session.id}
                    >
                      {deletingId === session.id ? (
                        <Loader2 className="animate-spin" size={18} />
                      ) : (
                        <Trash2 size={18} />
                      )}
                    </Button>
                  </div>
                </Card>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
