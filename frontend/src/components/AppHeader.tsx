import { LayoutDashboard, LogOut, Plus, Presentation, TrendingUp } from 'lucide-react'
import type { PropsWithChildren, ReactNode } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { cn } from '../lib/cn'
import { Button } from '../ui/Button'
import { Logo } from '../ui/Logo'

type AppHeaderProps = PropsWithChildren<{
  rightSlot?: ReactNode
}>

export function AppHeader({ children, rightSlot }: AppHeaderProps) {
  const location = useLocation()
  const navigate = useNavigate()
  const { user, logout } = useAuth()

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  const navItems = [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/sessions', label: 'Sessions', icon: Presentation },
    { to: '/progress', label: 'Progress', icon: TrendingUp },
  ]

  const initials = (user?.username ?? 'VU')
    .slice(0, 2)
    .toUpperCase()

  return (
    <header className="fixed inset-x-0 top-0 z-40 border-b border-slate-200 bg-white shadow-[0_1px_0_rgba(15,23,42,0.04)]">
      <div className="mx-auto flex h-[72px] w-full max-w-7xl items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
        <div className="flex min-w-0 items-center gap-8">
          <Logo />
          <nav className="hidden items-center gap-2 lg:flex">
            {navItems.map((item) => {
              const Icon = item.icon
              const isActive = location.pathname === item.to || location.pathname.startsWith(`${item.to}/`)

              return (
                <Link key={item.to} to={item.to}>
                  <span
                    className={cn(
                      'inline-flex h-10 items-center gap-2 rounded-xl px-4 text-sm font-semibold transition-colors',
                      isActive
                        ? 'bg-slate-900 text-white'
                        : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900',
                    )}
                  >
                    <Icon size={16} />
                    {item.label}
                  </span>
                </Link>
              )
            })}
          </nav>
          {children ? <div className="hidden items-center gap-2 xl:flex">{children}</div> : null}
        </div>
        <div className="flex items-center gap-3">
          <Link to="/sessions/new" className="hidden sm:block">
            <Button className="h-10 rounded-xl px-4 text-sm">
              <Plus size={16} />
              New Session
            </Button>
          </Link>
          {rightSlot ? <div className="flex items-center gap-2">{rightSlot}</div> : null}
          <div className="hidden items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2 md:flex">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-slate-900 text-sm font-bold text-white">
              {initials}
            </div>
            <div className="min-w-0">
              <p className="max-w-[140px] truncate text-sm font-semibold text-slate-900">
                {user?.username ?? 'Voxly User'}
              </p>
              <p className="max-w-[180px] truncate text-xs text-slate-500">
                {user?.email ?? 'local session'}
              </p>
            </div>
          </div>
          <Button variant="secondary" className="h-10 rounded-xl px-4 text-sm" onClick={handleLogout}>
            <LogOut size={16} />
            <span className="hidden sm:inline">Log out</span>
          </Button>
        </div>
      </div>
    </header>
  )
}
