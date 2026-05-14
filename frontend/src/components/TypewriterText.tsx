import { useEffect, useState } from 'react'

type TypewriterTextProps = {
  text: string
  speedMs?: number
}

export function TypewriterText({ text, speedMs = 16 }: TypewriterTextProps) {
  const [visibleLength, setVisibleLength] = useState(0)

  useEffect(() => {
    if (!text) {
      return
    }

    const interval = window.setInterval(() => {
      setVisibleLength((previous) => {
        if (previous >= text.length) {
          window.clearInterval(interval)
          return previous
        }

        return previous + 1
      })
    }, speedMs)

    return () => window.clearInterval(interval)
  }, [text, speedMs])

  return <p>{text.slice(0, visibleLength)}</p>
}
