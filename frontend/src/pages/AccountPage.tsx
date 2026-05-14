import { KeyRound, ShieldCheck, UserRound } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { useAuth } from '../auth/useAuth'
import { AppHeader } from '../components/AppHeader'
import { api } from '../lib/api'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { Input } from '../ui/Input'
import { SectionTitle } from '../ui/SectionTitle'

export function AccountPage() {
  const { user, accessToken, syncAccessToken, logout } = useAuth()
  const [username, setUsername] = useState(user?.username ?? '')
  const [professionalRole, setProfessionalRole] = useState(user?.professionalRole ?? '')
  const [coachingFocus, setCoachingFocus] = useState(user?.coachingFocus ?? '')
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [profileMessage, setProfileMessage] = useState<string | null>(null)
  const [securityMessage, setSecurityMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isSavingProfile, setIsSavingProfile] = useState(false)
  const [isSavingSecurity, setIsSavingSecurity] = useState(false)

  useEffect(() => {
    setUsername(user?.username ?? '')
    setProfessionalRole(user?.professionalRole ?? '')
    setCoachingFocus(user?.coachingFocus ?? '')
  }, [user?.username, user?.professionalRole, user?.coachingFocus])

  async function handleProfileSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!accessToken) return

    setError(null)
    setProfileMessage(null)
    setIsSavingProfile(true)

    try {
      await api.updateProfile(accessToken, {
        username,
        professionalRole: professionalRole.trim(),
        coachingFocus: coachingFocus.trim(),
      })
      await syncAccessToken()
      setProfileMessage('Profile updated.')
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Profile update failed.')
    } finally {
      setIsSavingProfile(false)
    }
  }

  async function handlePasswordSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!accessToken) return

    setError(null)
    setSecurityMessage(null)
    setIsSavingSecurity(true)

    try {
      await api.changePassword(accessToken, { currentPassword, newPassword })
      setSecurityMessage('Password updated. Please log in again.')
      setCurrentPassword('')
      setNewPassword('')
      await logout()
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Password update failed.')
    } finally {
      setIsSavingSecurity(false)
    }
  }

  async function handleToggleTwoFactor() {
    if (!accessToken || !user) return

    setError(null)
    setSecurityMessage(null)
    setIsSavingSecurity(true)

    try {
      if (user.twoFactorEnabled) {
        await api.disableTwoFactor(accessToken)
        setSecurityMessage('Two-factor authentication disabled.')
      } else {
        await api.enableTwoFactor(accessToken)
        setSecurityMessage('Two-factor authentication enabled. Codes will be sent by email at login.')
      }
      await syncAccessToken()
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Security update failed.')
    } finally {
      setIsSavingSecurity(false)
    }
  }

  return (
    <div className="min-h-screen bg-background">
      <AppHeader />
      <main className="mx-auto w-full max-w-5xl px-4 pb-16 pt-28 sm:px-6 lg:px-8">
        <SectionTitle
          eyebrow="Account"
          description="Manage your Voxly identity, password, and sign-in protection."
        >
          Profile and security
        </SectionTitle>

        {error ? (
          <div className="mt-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        ) : null}

        <div className="mt-8 grid gap-6 lg:grid-cols-[1fr_1fr]">
          <Card className="p-6">
            <div className="mb-5 flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
                <UserRound size={20} />
              </div>
              <div>
                <h2 className="text-lg font-semibold text-foreground">Profile</h2>
                <p className="text-sm text-muted-foreground">{user?.email}</p>
              </div>
            </div>

            <form className="space-y-5" onSubmit={handleProfileSubmit}>
              <label className="block space-y-1.5">
                <span className="text-sm font-medium">Username</span>
                <Input
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  required
                />
              </label>
              <label className="block space-y-1.5">
                <span className="text-sm font-medium">Professional role</span>
                <Input
                  value={professionalRole}
                  onChange={(event) => setProfessionalRole(event.target.value)}
                  placeholder="Product manager, teacher, founder..."
                />
              </label>
              <label className="block space-y-1.5">
                <span className="text-sm font-medium">Coaching focus</span>
                <Input
                  value={coachingFocus}
                  onChange={(event) => setCoachingFocus(event.target.value)}
                  placeholder="Investor pitches, interviews, class presentations..."
                />
              </label>

              {profileMessage ? (
                <p className="rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
                  {profileMessage}
                </p>
              ) : null}

              <Button type="submit" disabled={isSavingProfile}>
                {isSavingProfile ? 'Saving...' : 'Save profile'}
              </Button>
            </form>
          </Card>

          <Card className="p-6">
            <div className="mb-5 flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
                <ShieldCheck size={20} />
              </div>
              <div>
                <h2 className="text-lg font-semibold text-foreground">Two-factor authentication</h2>
                <p className="text-sm text-muted-foreground">
                  Status: {user?.twoFactorEnabled ? 'Enabled' : 'Disabled'}
                </p>
              </div>
            </div>

            <Button
              variant={user?.twoFactorEnabled ? 'secondary' : 'primary'}
              disabled={isSavingSecurity}
              onClick={handleToggleTwoFactor}
            >
              {user?.twoFactorEnabled ? 'Disable 2FA' : 'Enable 2FA'}
            </Button>

            {securityMessage ? (
              <p className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
                {securityMessage}
              </p>
            ) : null}
          </Card>
        </div>

        <Card className="mt-6 p-6">
          <div className="mb-5 flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <KeyRound size={20} />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-foreground">Change password</h2>
              <p className="text-sm text-muted-foreground">
                Updating your password signs out existing sessions.
              </p>
            </div>
          </div>

          <form className="grid gap-4 md:grid-cols-[1fr_1fr_auto]" onSubmit={handlePasswordSubmit}>
            <label className="block space-y-1.5">
              <span className="text-sm font-medium">Current password</span>
              <Input
                type="password"
                value={currentPassword}
                onChange={(event) => setCurrentPassword(event.target.value)}
                required
              />
            </label>
            <label className="block space-y-1.5">
              <span className="text-sm font-medium">New password</span>
              <Input
                type="password"
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
                required
              />
            </label>
            <div className="flex items-end">
              <Button type="submit" disabled={isSavingSecurity}>
                {isSavingSecurity ? 'Saving...' : 'Change'}
              </Button>
            </div>
          </form>
        </Card>
      </main>
    </div>
  )
}
