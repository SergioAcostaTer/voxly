import type { PropsWithChildren } from 'react'
import { cn } from '../lib/cn'

type SectionTitleProps = PropsWithChildren<{
  eyebrow: string
  className?: string
  description?: string
}>

export function SectionTitle({
  eyebrow,
  className,
  description,
  children,
}: SectionTitleProps) {
  return (
    <div className={cn('space-y-3', className)}>
      <p className="display-font text-xs font-semibold uppercase tracking-[0.18em] text-primary/80">
        {eyebrow}
      </p>
      <h2 className="display-font text-2xl font-semibold tracking-tight text-foreground sm:text-3xl">
        {children}
      </h2>
      {description ? (
        <p className="max-w-2xl text-sm leading-relaxed text-muted-foreground sm:text-base">
          {description}
        </p>
      ) : null}
    </div>
  )
}
