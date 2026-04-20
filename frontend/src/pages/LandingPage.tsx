import {
    AudioLines,
    BrainCircuit,
    CalendarClock,
    MessageCircleMore,
    Play,
    Sparkles,
    TimerReset,
    Video,
} from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { Logo } from '../ui/Logo'
import { SectionTitle } from '../ui/SectionTitle'

const features = [
  {
    icon: BrainCircuit,
    title: 'AI Speech Coach',
    description:
      'Practice in real scenarios and get instant feedback on pace, clarity, fillers, and confidence.',
  },
  {
    icon: Video,
    title: 'Interview Simulations',
    description:
      'Run mock interviews tailored by role, seniority, and style so each session feels realistic.',
  },
  {
    icon: TimerReset,
    title: 'Daily Repetition Loops',
    description:
      'Build speaking habits with short guided sessions that fit your schedule and momentum.',
  },
]

const stats = [
  { label: 'Practice Sessions', value: '120K+' },
  { label: 'Avg. Confidence Boost', value: '37%' },
  { label: 'Countries', value: '42' },
]

export function LandingPage() {
  const navigate = useNavigate()
  const { isAuthenticated, user, logout } = useAuth()

  async function handleLogout() {
    await logout()
    navigate('/', { replace: true })
  }

  return (
    <div className="relative overflow-hidden pb-20">
      <header className="fixed inset-x-0 top-0 z-40 border-b border-slate-200 bg-white">
        <div className="mx-auto flex h-[72px] w-full max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <Logo />
          <nav className="hidden items-center gap-7 text-base text-muted-foreground md:flex">
            <a href="#features" className="hover:text-foreground">
              Features
            </a>
            <a href="#how" className="hover:text-foreground">
              How it works
            </a>
            <a href="#faq" className="hover:text-foreground">
              FAQ
            </a>
          </nav>
          <div className="flex items-center gap-2 sm:gap-3">
            {isAuthenticated ? (
              <>
                <Link to="/app">
                  <Button variant="ghost" className="hidden sm:inline-flex">
                    {user?.username ?? 'My'} workspace
                  </Button>
                </Link>
                <Button onClick={handleLogout} variant="secondary">
                  Log out
                </Button>
              </>
            ) : (
              <>
                <Link to="/login">
                  <Button variant="ghost" className="hidden sm:inline-flex">
                    Log in
                  </Button>
                </Link>
                <Link to="/register">
                  <Button>Start free</Button>
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      <div className="mx-auto w-full max-w-7xl px-4 pt-28 sm:px-6 lg:px-8">

        <section className="grid gap-8 pb-12 pt-10 lg:grid-cols-[1.05fr_0.95fr] lg:gap-12 lg:pt-16">
          <div className="order-2 lg:order-1">
            <p className="reveal-up display-font inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/10 px-4 py-2 text-sm font-semibold uppercase tracking-[0.12em] text-primary">
              <Sparkles size={14} />
              AI-powered speech growth
            </p>
            <h1 className="reveal-up delay-1 display-font mt-5 text-4xl font-semibold leading-[1.02] tracking-tight text-foreground sm:text-6xl lg:text-7xl">
              Sound sharp,
              <br />
              calm, and unforgettable.
            </h1>
            <p className="reveal-up delay-2 mt-4 max-w-2xl text-base leading-relaxed text-muted-foreground sm:mt-6 sm:text-xl">
              Voxly helps you master public speaking, interviews, and high-stakes
              conversations with adaptive AI coaching. Train with realistic prompts,
              hear your blind spots, and build delivery that feels natural.
            </p>
            <div className="reveal-up delay-3 mt-8 flex flex-wrap items-center gap-3">
              <Link to="/register">
                <Button size="lg">Create your account</Button>
              </Link>
              <Link to="/login">
                <Button variant="secondary" size="lg">
                  I already have access
                </Button>
              </Link>
              <Link to="/register">
                <Button variant="ghost" size="lg" className="border border-primary/20 bg-white/70">
                  Start first practice free
                </Button>
              </Link>
            </div>
            <div className="mt-8 hidden grid-cols-2 gap-3 sm:grid sm:grid-cols-3">
              {stats.map((stat) => (
                <Card key={stat.label} className="p-5">
                  <p className="display-font text-2xl font-semibold text-foreground sm:text-3xl">
                    {stat.value}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">{stat.label}</p>
                </Card>
              ))}
            </div>
            <div className="mt-5 hidden flex-wrap items-center gap-3 sm:flex">
              <Link to="/register" className="text-sm font-semibold text-primary hover:text-primary/85">
                Claim free onboarding session
              </Link>
              <span className="text-sm text-muted-foreground">No card required</span>
            </div>
          </div>

          <Card className="reveal-up delay-2 order-1 relative h-fit overflow-hidden border-primary/20 bg-gradient-to-b from-white via-white to-primary/5 p-5 sm:p-8 lg:order-2">
            <div className="absolute -right-10 -top-10 h-40 w-40 rounded-full bg-primary/15 blur-3xl" />
            <div className="absolute -bottom-14 left-8 h-44 w-44 rounded-full bg-accent/20 blur-3xl" />
            <div className="relative mb-5 flex items-center justify-between">
              <p className="display-font text-xl font-semibold sm:text-2xl">Live Session Preview</p>
              <span className="rounded-full bg-accent px-3 py-1 text-sm font-semibold text-accent-foreground">
                14 min
              </span>
            </div>
            <div className="relative rounded-2xl border border-border bg-white/95 p-4">
              <div className="mb-3 flex items-center justify-between">
                <p className="inline-flex items-center gap-2 text-sm font-semibold text-foreground">
                  <AudioLines size={16} className="text-primary" />
                  Speaking Audio
                </p>
                <button className="inline-flex h-8 w-8 items-center justify-center rounded-full bg-primary text-primary-foreground">
                  <Play size={14} />
                </button>
              </div>
              <div className="flex items-end gap-1.5">
                {[16, 28, 20, 38, 14, 26, 34, 18, 24, 30, 12, 26, 20].map((height, index) => (
                  <span
                    key={`${height}-${index}`}
                    className="w-1.5 rounded-full bg-primary/70"
                    style={{ height }}
                  />
                ))}
              </div>
              <div className="mt-4 sm:hidden">
                <Link to="/register">
                  <Button className="w-full">Try this voice coaching flow</Button>
                </Link>
              </div>
            </div>

            <div className="relative mt-4 hidden rounded-2xl border border-border bg-white p-4 sm:block">
              <p className="inline-flex items-center gap-2 text-sm font-semibold text-foreground">
                <MessageCircleMore size={16} className="text-primary" />
                AI Coach Suggestions
              </p>
              <div className="mt-3 space-y-2 text-sm">
                <div className="rounded-xl bg-primary/10 px-3 py-2">
                  <span className="mr-2 font-semibold text-primary">00:08</span>
                  Slow down before your key result metric.
                </div>
                <div className="rounded-xl bg-accent/15 px-3 py-2">
                  <span className="mr-2 font-semibold text-accent-foreground">00:15</span>
                  Good tone. Add a short pause after "team lead".
                </div>
              </div>
            </div>

            <div className="relative mt-4 hidden rounded-2xl border border-primary/20 bg-primary/10 p-5 sm:block">
              <p className="text-sm uppercase tracking-[0.08em] text-primary">Live Transcript</p>
              <div className="mt-3 space-y-2 text-sm text-foreground/90">
                <p>
                  <span className="font-semibold text-primary">00:05</span> I led the product kickoff for a cross-functional group...
                </p>
                <p>
                  <span className="font-semibold text-primary">00:09</span> ...and I aligned engineering and design on milestones.
                </p>
                <p>
                  <span className="font-semibold text-primary">00:14</span> We delivered two weeks early with fewer revisions.
                </p>
              </div>
            </div>
          </Card>
        </section>

        <section id="features" className="space-y-6 pb-16">
          <SectionTitle
            eyebrow="Core Features"
            description="Everything is designed to make speech practice structured, measurable, and enjoyable."
          >
            Built for speaking under pressure
          </SectionTitle>
          <div className="grid gap-4 md:grid-cols-3">
            {features.map((feature) => (
              <Card key={feature.title} className="transition hover:-translate-y-1 hover:border-primary/35">
                <feature.icon className="text-primary" size={20} />
                <h3 className="display-font mt-4 text-lg font-semibold text-foreground">
                  {feature.title}
                </h3>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                  {feature.description}
                </p>
              </Card>
            ))}
          </div>
          <div className="pt-2">
            <Link to="/register">
              <Button size="lg">Create Voxly account</Button>
            </Link>
          </div>
        </section>

        <section id="how" className="pb-16">
          <SectionTitle eyebrow="How It Works">
            Three steps to better speaking
          </SectionTitle>
          <div className="mt-6 grid gap-4 md:grid-cols-3">
            {[
              {
                icon: CalendarClock,
                title: 'Pick your scenario',
                text: 'Choose interview, pitch, meeting, or presentation practice tracks.',
              },
              {
                icon: Video,
                title: 'Speak out loud',
                text: 'Record or speak live while Voxly listens and analyzes your delivery.',
              },
              {
                icon: BrainCircuit,
                title: 'Improve with precision',
                text: 'Get actionable feedback and replay focused drills for weak points.',
              },
            ].map((item, index) => (
              <Card key={item.title} className="relative pt-9">
                <span className="display-font absolute left-6 top-4 text-xs font-semibold text-primary/60">
                  0{index + 1}
                </span>
                <item.icon size={20} className="text-primary" />
                <p className="display-font mt-3 text-lg font-semibold">{item.title}</p>
                <p className="mt-2 text-sm text-muted-foreground">{item.text}</p>
              </Card>
            ))}
          </div>
        </section>

        <section id="faq" className="pb-10">
          <Card className="bg-gradient-to-r from-primary to-cyan-500 text-primary-foreground">
            <p className="display-font text-2xl font-semibold">Ready to sound more confident?</p>
            <p className="mt-2 max-w-2xl text-sm text-primary-foreground/85 sm:text-base">
              Start free and unlock your first guided speech track in less than two
              minutes.
            </p>
            <div className="mt-5">
              <Link to="/register">
                <Button variant="secondary" className="border-white/20 bg-white/15 text-white hover:bg-white/20">
                  Get started with Voxly
                </Button>
              </Link>
              <Link to="/register" className="ml-3 inline-block text-sm font-semibold text-white/90 underline underline-offset-4 hover:text-white">
                Register and run your first interview simulation
              </Link>
            </div>
          </Card>
        </section>
      </div>
    </div>
  )
}
