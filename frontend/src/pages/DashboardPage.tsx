import { BarChart3, LogOut, Mic2, Plus, TrendingUp, Video } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { Logo } from '../ui/Logo'

export function DashboardPage() {
  const navigate = useNavigate()
  const { user, logout } = useAuth()

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="px-4 py-6 sm:px-6 sm:py-8 lg:py-12">
      <div className="mx-auto w-full max-w-5xl space-y-6">
        <header className="flex items-center justify-between rounded-2xl border border-white/70 bg-white/80 px-4 py-3 shadow-panel backdrop-blur-sm">
          <Logo />
          <Button variant="secondary" onClick={handleLogout}>
            <LogOut size={16} />
            Log out
          </Button>
        </header>

        <Card className="bg-gradient-to-br from-primary to-cyan-700 text-primary-foreground">
          <p className="display-font text-xs font-semibold uppercase tracking-[0.18em] text-primary-foreground/80">
            Welcome back
          </p>
          <h1 className="display-font mt-3 text-3xl font-semibold">
            {user?.username ?? 'Voxly user'}
          </h1>
          <p className="mt-2 max-w-2xl text-sm text-primary-foreground/85 sm:text-base">
            Practice your presentation skills with AI-powered feedback and analysis.
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <Link to="/sessions/new">
              <Button className="bg-white text-primary hover:bg-white/90">
                <Plus size={18} />
                New Session
              </Button>
            </Link>
            <Link to="/sessions">
              <Button variant="ghost" className="border border-white/30 text-white hover:bg-white/20">
                <Video size={18} />
                View Sessions
              </Button>
            </Link>
          </div>
        </Card>

        <div className="grid gap-4 md:grid-cols-3">
          <Link to="/sessions/new" className="block">
            <Card className="h-full transition-all hover:border-primary/40 hover:shadow-md">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10">
                <Mic2 className="text-primary" size={24} />
              </div>
              <p className="display-font mt-4 text-lg font-semibold text-foreground">
                Start Practicing
              </p>
              <p className="mt-2 text-sm text-muted-foreground">
                Record a new presentation and get instant AI feedback on your delivery.
              </p>
            </Card>
          </Link>

          <Link to="/sessions" className="block">
            <Card className="h-full transition-all hover:border-primary/40 hover:shadow-md">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-100">
                <Video className="text-blue-600" size={24} />
              </div>
              <p className="display-font mt-4 text-lg font-semibold text-foreground">
                My Sessions
              </p>
              <p className="mt-2 text-sm text-muted-foreground">
                Review past recordings, transcriptions, and feedback from your sessions.
              </p>
            </Card>
          </Link>

          <Link to="/progress" className="block">
            <Card className="h-full transition-all hover:border-primary/40 hover:shadow-md">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-green-100">
                <TrendingUp className="text-green-600" size={24} />
              </div>
              <p className="display-font mt-4 text-lg font-semibold text-foreground">
                Track Progress
              </p>
              <p className="mt-2 text-sm text-muted-foreground">
                See your improvement over time with detailed metrics and trends.
              </p>
            </Card>
          </Link>
        </div>

        <Card>
          <div className="flex items-center gap-3">
            <BarChart3 className="text-primary" size={20} />
            <p className="display-font text-lg font-semibold text-foreground">Quick Stats</p>
          </div>
          <div className="mt-4 grid gap-4 sm:grid-cols-3">
            <div className="rounded-xl bg-muted/30 p-4 text-center">
              <p className="text-2xl font-bold text-foreground">--</p>
              <p className="mt-1 text-sm text-muted-foreground">Total Sessions</p>
            </div>
            <div className="rounded-xl bg-muted/30 p-4 text-center">
              <p className="text-2xl font-bold text-foreground">--</p>
              <p className="mt-1 text-sm text-muted-foreground">Avg. Clarity Score</p>
            </div>
            <div className="rounded-xl bg-muted/30 p-4 text-center">
              <p className="text-2xl font-bold text-foreground">--</p>
              <p className="mt-1 text-sm text-muted-foreground">Avg. WPM</p>
            </div>
          </div>
          <p className="mt-4 text-center text-sm text-muted-foreground">
            <Link to="/progress" className="text-primary hover:underline">
              View detailed progress
            </Link>
          </p>
        </Card>
      </div>
    </div>
  )
}
