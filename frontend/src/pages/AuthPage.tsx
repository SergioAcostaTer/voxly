import { ArrowLeft, CheckCircle2 } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { Input } from '../ui/Input'
import { Logo } from '../ui/Logo'

type AuthPageProps = {
  mode: 'login' | 'register'
}

const bullets = [
  'Personalized speaking plans from day one',
  'Interview, meeting, and presentation drills',
  'Actionable AI feedback after every session',
]

export function AuthPage({ mode }: AuthPageProps) {
  const isLogin = mode === 'login'
  const navigate = useNavigate()
  const location = useLocation()
  const { login, register } = useAuth()

  const [identifier, setIdentifier] = useState('')
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [twoFactorCode, setTwoFactorCode] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)

    try {
      if (isLogin) {
        await login({
          identifier,
          password,
          twoFactorCode: twoFactorCode.trim() || undefined,
        })

        const fromPath = (location.state as { from?: string } | null)?.from
        navigate(fromPath || '/app', { replace: true })
        return
      }

      await register({
        email,
        username,
        password,
      })

      await login({
        identifier: username,
        password,
      })

      navigate('/app', { replace: true })
    } catch (submitError) {
      setError(
        submitError instanceof Error
          ? submitError.message
          : 'Unable to complete authentication request.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-[100svh] items-center px-4 py-6 sm:px-6 sm:py-8 lg:block lg:py-12">
      <div className="mx-auto w-full max-w-7xl">
        <div className="mb-6 hidden items-center justify-between lg:flex">
          <Logo />
          <Link
            to="/"
            className="inline-flex items-center gap-1 text-sm text-muted-foreground transition hover:text-foreground"
          >
            <ArrowLeft size={16} />
            Back to home
          </Link>
        </div>

        <div className="mx-auto grid w-full max-w-xl gap-5 lg:max-w-none lg:grid-cols-[0.95fr_1.05fr]">
          <Card className="relative hidden overflow-hidden border-primary/20 bg-gradient-to-br from-primary/95 to-cyan-700 text-white lg:block">
            <div className="absolute -left-8 top-8 h-40 w-40 rounded-full bg-white/10 blur-3xl" />
            <div className="absolute -bottom-10 right-2 h-48 w-48 rounded-full bg-accent/40 blur-3xl" />
            <div className="relative space-y-5 p-4">
              <p className="display-font text-xs font-semibold uppercase tracking-[0.18em] text-white/75">
                Voxly AI Coach
              </p>
              <h1 className="display-font text-3xl font-semibold leading-tight sm:text-4xl">
                {isLogin
                  ? 'Welcome back. Continue your speaking streak.'
                  : 'Create your Voxly account and start training your voice.'}
              </h1>
              <p className="max-w-lg text-sm leading-relaxed text-white/85 sm:text-base">
                Build clarity and confidence for public speaking, interviews, and
                every high-stakes conversation.
              </p>
              <div className="space-y-2">
                {bullets.map((item) => (
                  <div key={item} className="flex items-start gap-2 text-sm text-white/90">
                    <CheckCircle2 size={16} className="mt-0.5" />
                    <span>{item}</span>
                  </div>
                ))}
              </div>
            </div>
          </Card>

          <Card className="bg-white/90 p-6 sm:p-8 lg:p-10">
            <div className="mb-5 flex items-center justify-between lg:hidden">
              <Logo />
              <Link
                to="/"
                className="inline-flex items-center gap-1 text-sm text-muted-foreground transition hover:text-foreground"
              >
                <ArrowLeft size={16} />
                Home
              </Link>
            </div>
            <p className="display-font text-3xl font-semibold tracking-tight text-foreground">
              {isLogin ? 'Log in to Voxly' : 'Create your account'}
            </p>
            <p className="mt-2 text-base text-muted-foreground">
              {isLogin
                ? 'Enter your credentials to continue practicing.'
                : 'Set up your profile and begin with a free plan.'}
            </p>

            <form className="mt-7 space-y-5" onSubmit={handleSubmit}>
              {!isLogin ? (
                <label className="block space-y-1.5">
                  <span className="text-base font-medium">Username</span>
                  <Input
                    placeholder="jane_doe"
                    value={username}
                    onChange={(event) => setUsername(event.target.value)}
                    required
                  />
                </label>
              ) : null}

              <label className="block space-y-1.5">
                <span className="text-base font-medium">
                  {isLogin ? 'Email or username' : 'Email'}
                </span>
                <Input
                  type={isLogin ? 'text' : 'email'}
                  placeholder={isLogin ? 'you@company.com or jane_doe' : 'you@company.com'}
                  value={isLogin ? identifier : email}
                  onChange={(event) =>
                    isLogin
                      ? setIdentifier(event.target.value)
                      : setEmail(event.target.value)
                  }
                  required
                />
              </label>

              <label className="block space-y-1.5">
                <span className="text-base font-medium">Password</span>
                <Input
                  type="password"
                  placeholder="********"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  required
                />
              </label>

              {isLogin ? (
                <label className="block space-y-1.5">
                  <span className="text-base font-medium">2FA Code (optional)</span>
                  <Input
                    placeholder="123456"
                    value={twoFactorCode}
                    onChange={(event) => setTwoFactorCode(event.target.value)}
                  />
                </label>
              ) : null}

              {error ? (
                <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                  {error}
                </p>
              ) : null}

              <Button type="submit" className="mt-2 w-full" size="lg" disabled={isSubmitting}>
                {isSubmitting
                  ? 'Please wait...'
                  : isLogin
                    ? 'Log in'
                    : 'Create account'}
              </Button>
            </form>

            <p className="mt-6 text-center text-base text-muted-foreground">
              {isLogin ? "Don't have an account?" : 'Already have an account?'}{' '}
              <Link
                to={isLogin ? '/register' : '/login'}
                className="font-semibold text-primary hover:text-primary/85"
              >
                {isLogin ? 'Register' : 'Log in'}
              </Link>
            </p>
          </Card>
        </div>
      </div>
    </div>
  )
}
