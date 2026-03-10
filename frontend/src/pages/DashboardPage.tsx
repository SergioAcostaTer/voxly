import { LogOut, Mic2, ShieldCheck } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
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
            Your account
          </p>
          <h1 className="display-font mt-3 text-3xl font-semibold">
            Welcome, {user?.username ?? 'Voxly user'}
          </h1>
          <p className="mt-2 max-w-2xl text-sm text-primary-foreground/85 sm:text-base">
            Your auth flow is now connected to real backend endpoints with refresh-token cookie handling.
          </p>
        </Card>

        <div className="grid gap-4 md:grid-cols-2">
          <Card>
            <p className="display-font text-lg font-semibold text-foreground">Profile Snapshot</p>
            <div className="mt-4 space-y-2 text-sm text-muted-foreground">
              <p>
                <span className="font-semibold text-foreground">Email:</span> {user?.email}
              </p>
              <p>
                <span className="font-semibold text-foreground">User ID:</span> {user?.id}
              </p>
              <p>
                <span className="font-semibold text-foreground">Roles:</span> {user?.roles.join(', ') || 'None'}
              </p>
            </div>
          </Card>

          <Card>
            <p className="display-font text-lg font-semibold text-foreground">Security</p>
            <div className="mt-4 flex items-start gap-3 text-sm text-muted-foreground">
              <ShieldCheck className="mt-0.5 text-primary" size={18} />
              <p>
                Access token is sent as a bearer token for protected routes and refresh token stays in HttpOnly cookie.
              </p>
            </div>
            <div className="mt-3 flex items-start gap-3 text-sm text-muted-foreground">
              <Mic2 className="mt-0.5 text-primary" size={18} />
              <p>Next step: connect this dashboard to your real speech practice features.</p>
            </div>
          </Card>
        </div>
      </div>
    </div>
  )
}
