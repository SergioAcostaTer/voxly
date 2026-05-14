import { Mic2 } from 'lucide-react'
import { Link } from 'react-router-dom'
import { cn } from '../lib/cn'

type LogoProps = {
  className?: string
}

export function Logo({ className }: LogoProps) {
  return (
    <Link to="/" className={cn('inline-flex items-center gap-2', className)}>
      <span className="inline-flex h-10 w-10 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-glow">
        <Mic2 size={19} />
      </span>
      <span className="display-font text-xl font-semibold tracking-tight">Voxly</span>
    </Link>
  )
}
