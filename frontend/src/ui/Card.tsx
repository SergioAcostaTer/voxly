import type { HTMLAttributes, PropsWithChildren } from 'react'
import { cn } from '../lib/cn'

type CardProps = PropsWithChildren<HTMLAttributes<HTMLDivElement>>

export function Card({ children, className, ...props }: CardProps) {
  return (
    <div
      className={cn(
        'rounded-2xl border border-white/70 bg-card/80 p-6 text-card-foreground shadow-panel backdrop-blur-sm',
        className,
      )}
      {...props}
    >
      {children}
    </div>
  )
}
