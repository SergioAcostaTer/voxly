import { Wifi, WifiOff } from 'lucide-react'
import { useEffect, useState } from 'react'
import { cn } from '../lib/cn'

export function ConnectivityBanner() {
  const [isOnline, setIsOnline] = useState(navigator.onLine)

  useEffect(() => {
    const handleOnline = () => setIsOnline(true)
    const handleOffline = () => setIsOnline(false)

    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)

    return () => {
      window.removeEventListener('online', handleOnline)
      window.removeEventListener('offline', handleOffline)
    }
  }, [])

  return (
    <div
      className={cn(
        'flex items-center justify-center gap-2 px-4 py-2 text-xs font-medium transition-all',
        isOnline
          ? 'bg-emerald-50 text-emerald-700'
          : 'bg-amber-50 text-amber-800',
      )}
    >
      {isOnline ? <Wifi size={14} /> : <WifiOff size={14} />}
      {isOnline
        ? 'Connected. Live updates continue in the background.'
        : 'You are offline. The current screen will keep the last known state and reconnect automatically.'}
    </div>
  )
}
