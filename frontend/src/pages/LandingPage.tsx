import {
    BrainCircuit,
    CalendarClock,
    CheckCircle2,
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
      <div className="mx-auto w-full max-w-6xl px-4 pt-6 sm:px-6 lg:px-8">
        <header className="reveal-up flex items-center justify-between rounded-2xl border border-white/70 bg-white/70 px-4 py-3 shadow-panel backdrop-blur-sm sm:px-5">
          <Logo />
          <nav className="hidden items-center gap-6 text-sm text-muted-foreground md:flex">
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
        </header>

        <section className="grid gap-8 pb-12 pt-12 lg:grid-cols-[1.1fr_0.9fr] lg:gap-12 lg:pt-16">
          <div>
            <p className="reveal-up display-font inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/10 px-4 py-2 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
              <Sparkles size={14} />
              AI-powered speech growth
            </p>
            <h1 className="reveal-up delay-1 display-font mt-6 text-4xl font-semibold leading-tight tracking-tight text-foreground sm:text-5xl lg:text-6xl">
              Become the clearest voice in every room.
            </h1>
            <p className="reveal-up delay-2 mt-6 max-w-2xl text-base leading-relaxed text-muted-foreground sm:text-lg">
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
            </div>
            <div className="mt-8 grid grid-cols-2 gap-3 sm:grid-cols-3">
              {stats.map((stat) => (
                <Card key={stat.label} className="p-4">
                  <p className="display-font text-xl font-semibold text-foreground sm:text-2xl">
                    {stat.value}
                  </p>
                  <p className="mt-1 text-xs text-muted-foreground">{stat.label}</p>
                </Card>
              ))}
            </div>
          </div>

          <Card className="reveal-up delay-2 relative h-fit border-primary/20 bg-gradient-to-b from-white via-white to-primary/5 p-5 sm:p-7">
            <div className="mb-5 flex items-center justify-between">
              <p className="display-font text-lg font-semibold">Today Practice Plan</p>
              <span className="rounded-full bg-accent px-3 py-1 text-xs font-semibold text-accent-foreground">
                14 min
              </span>
            </div>
            <div className="space-y-3 text-sm">
              {[
                'Warm-up articulation drill',
                'Interview story response',
                'Public speaking pacing challenge',
              ].map((item) => (
                <div
                  key={item}
                  className="flex items-center gap-2 rounded-xl border border-border bg-white px-3 py-2"
                >
                  <CheckCircle2 size={16} className="text-primary" />
                  <span>{item}</span>
                </div>
              ))}
            </div>
            <div className="mt-5 rounded-xl border border-primary/20 bg-primary/10 p-4">
              <p className="text-xs uppercase tracking-[0.12em] text-primary">
                AI Tip of the day
              </p>
              <p className="mt-2 text-sm text-foreground">
                Pause one second before key points. That tiny silence makes your
                message sound intentional and confident.
              </p>
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
            </div>
          </Card>
        </section>
      </div>
    </div>
  )
}
