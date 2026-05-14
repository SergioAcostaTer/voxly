import { ArrowLeft, KeyRound, Mail } from 'lucide-react'
import { useMemo, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../lib/api'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { Input } from '../ui/Input'
import { Logo } from '../ui/Logo'

export function PasswordResetPage() {
  const [searchParams] = useSearchParams()
  const token = useMemo(() => searchParams.get('token') ?? '', [searchParams])
  const isReset = token.length > 0

  const [email, setEmail] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setMessage(null)
    setIsSubmitting(true)

    try {
      if (isReset) {
        await api.resetPassword(token, newPassword)
        setMessage('Your password has been updated. You can log in with the new password.')
      } else {
        await api.requestPasswordReset(email)
        setMessage('If that account exists, a password reset email has been sent.')
      }
    } catch (submitError) {
      setError(
        submitError instanceof Error
          ? submitError.message
          : 'We could not process this request right now.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-[100svh] items-center px-4 py-6 sm:px-6">
      <div className="mx-auto w-full max-w-xl">
        <div className="mb-6 flex items-center justify-between">
          <Logo />
          <Link
            to="/login"
            className="inline-flex items-center gap-1 text-sm text-muted-foreground transition hover:text-foreground"
          >
            <ArrowLeft size={16} />
            Back to login
          </Link>
        </div>

        <Card className="bg-white/90 p-6 sm:p-8">
          <div className="mb-7 flex items-start gap-4">
            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary">
              {isReset ? <KeyRound size={22} /> : <Mail size={22} />}
            </div>
            <div>
              <h1 className="display-font text-3xl font-semibold tracking-tight text-foreground">
                {isReset ? 'Set a new password' : 'Recover your password'}
              </h1>
              <p className="mt-2 text-base text-muted-foreground">
                {isReset
                  ? 'Choose a new password for your Voxly account.'
                  : 'Enter the email for your account and we will send a reset link.'}
              </p>
            </div>
          </div>

          <form className="space-y-5" onSubmit={handleSubmit}>
            {isReset ? (
              <label className="block space-y-1.5">
                <span className="text-base font-medium">New password</span>
                <Input
                  type="password"
                  placeholder="********"
                  value={newPassword}
                  onChange={(event) => setNewPassword(event.target.value)}
                  required
                />
              </label>
            ) : (
              <label className="block space-y-1.5">
                <span className="text-base font-medium">Email</span>
                <Input
                  type="email"
                  placeholder="you@company.com"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  required
                />
              </label>
            )}

            {message ? (
              <p className="rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
                {message}
              </p>
            ) : null}

            {error ? (
              <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                {error}
              </p>
            ) : null}

            <Button type="submit" size="lg" className="w-full" disabled={isSubmitting}>
              {isSubmitting ? 'Please wait...' : isReset ? 'Update password' : 'Send reset link'}
            </Button>
          </form>
        </Card>
      </div>
    </div>
  )
}
