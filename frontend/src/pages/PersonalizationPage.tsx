import { Sparkles } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { ApiClientError, api } from '../lib/api'
import type { ExperienceLevel } from '../types/auth'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { Input } from '../ui/Input'
import { Logo } from '../ui/Logo'
import { Select } from '../ui/Select'

const ROLE_OPTIONS = [
  'Student',
  'Engineer',
  'Manager',
  'Founder',
  'Sales / Account Manager',
  'Teacher / Educator',
  'Consultant',
  'Other',
]

const GOAL_OPTIONS = [
  'Public speaking',
  'Job interviews',
  'Presentations',
  'Meetings',
  'Sales pitches',
  'Language fluency',
  'Other',
]

const EXPERIENCE_OPTIONS: { value: ExperienceLevel; label: string; help: string }[] = [
  { value: 'BEGINNER', label: 'Beginner', help: 'Just getting started with structured practice' },
  { value: 'INTERMEDIATE', label: 'Intermediate', help: 'Comfortable, working on consistency and clarity' },
  { value: 'ADVANCED', label: 'Advanced', help: 'Refining nuance, pacing, and high-stakes delivery' },
]

export function PersonalizationPage() {
  const navigate = useNavigate()
  const { accessToken, setUser, user } = useAuth()

  const [professionalRole, setProfessionalRole] = useState<string>(ROLE_OPTIONS[0])
  const [professionalRoleOther, setProfessionalRoleOther] = useState('')
  const [primaryGoal, setPrimaryGoal] = useState<string>(GOAL_OPTIONS[0])
  const [primaryGoalOther, setPrimaryGoalOther] = useState('')
  const [experienceLevel, setExperienceLevel] = useState<ExperienceLevel>('BEGINNER')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (user?.personalizationCompleted) {
    return <Navigate to="/dashboard" replace />
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!accessToken) return
    setError(null)
    setIsSubmitting(true)

    const resolvedRole = professionalRole === 'Other'
      ? professionalRoleOther.trim()
      : professionalRole
    const resolvedGoal = primaryGoal === 'Other'
      ? primaryGoalOther.trim()
      : primaryGoal

    if (!resolvedRole) {
      setError('Please describe your role.')
      setIsSubmitting(false)
      return
    }
    if (!resolvedGoal) {
      setError('Please describe your primary goal.')
      setIsSubmitting(false)
      return
    }

    try {
      const updated = await api.completePersonalization(accessToken, {
        professionalRole: resolvedRole,
        primaryGoal: resolvedGoal,
        experienceLevel,
      })
      setUser(updated)
      navigate('/dashboard', { replace: true })
    } catch (submitError) {
      setError(
        submitError instanceof ApiClientError
          ? submitError.message
          : 'We could not save your preferences. Please try again.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-[100svh] items-center px-4 py-6 sm:px-6 sm:py-8 lg:py-12">
      <div className="mx-auto w-full max-w-3xl">
        <div className="mb-6 flex items-center justify-between">
          <Logo />
          <span className="text-sm text-muted-foreground">Step 1 of 1</span>
        </div>

        <Card className="bg-white/90 p-6 sm:p-8 lg:p-10">
          <div className="flex items-center gap-3">
            <div className="inline-flex h-11 w-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <Sparkles size={22} />
            </div>
            <div>
              <p className="display-font text-2xl font-semibold tracking-tight text-foreground">
                Personalize your coaching
              </p>
              <p className="mt-1 text-sm text-muted-foreground">
                Tell us a bit about you so we can tailor exercises and feedback.
              </p>
            </div>
          </div>

          <form className="mt-7 space-y-5" onSubmit={handleSubmit}>
            <label className="block space-y-1.5">
              <span className="text-base font-medium">What best describes your role?</span>
              <Select
                value={professionalRole}
                onChange={(event) => setProfessionalRole(event.target.value)}
                required
              >
                {ROLE_OPTIONS.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </Select>
              {professionalRole === 'Other' ? (
                <Input
                  className="mt-2"
                  placeholder="Tell us your role"
                  value={professionalRoleOther}
                  onChange={(event) => setProfessionalRoleOther(event.target.value)}
                  required
                  maxLength={100}
                  autoComplete="organization-title"
                />
              ) : null}
            </label>

            <label className="block space-y-1.5">
              <span className="text-base font-medium">What's your primary goal with Voxly?</span>
              <Select
                value={primaryGoal}
                onChange={(event) => setPrimaryGoal(event.target.value)}
                required
              >
                {GOAL_OPTIONS.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </Select>
              {primaryGoal === 'Other' ? (
                <Input
                  className="mt-2"
                  placeholder="Tell us your goal"
                  value={primaryGoalOther}
                  onChange={(event) => setPrimaryGoalOther(event.target.value)}
                  required
                  maxLength={100}
                />
              ) : null}
            </label>

            <label className="block space-y-1.5">
              <span className="text-base font-medium">Your speaking experience level</span>
              <Select
                value={experienceLevel}
                onChange={(event) => setExperienceLevel(event.target.value as ExperienceLevel)}
                required
              >
                {EXPERIENCE_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label} — {option.help}
                  </option>
                ))}
              </Select>
            </label>

            {error ? (
              <p
                role="alert"
                className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700"
              >
                {error}
              </p>
            ) : null}

            <Button type="submit" className="mt-2 w-full" size="lg" disabled={isSubmitting}>
              {isSubmitting ? 'Saving...' : 'Continue to dashboard'}
            </Button>
          </form>
        </Card>
      </div>
    </div>
  )
}
