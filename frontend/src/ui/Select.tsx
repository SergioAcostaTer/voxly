import type { SelectHTMLAttributes } from 'react'
import { cn } from '../lib/cn'

type SelectProps = SelectHTMLAttributes<HTMLSelectElement>

export function Select({ className, children, ...props }: SelectProps) {
  return (
    <select
      className={cn(
        'h-12 w-full rounded-xl border border-border bg-white/90 px-4 text-base text-foreground outline-none transition focus:border-primary/50 focus:ring-4 focus:ring-primary/20',
        className,
      )}
      {...props}
    >
      {children}
    </select>
  )
}
