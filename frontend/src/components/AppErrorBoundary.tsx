import { AlertCircle, RefreshCw } from 'lucide-react'
import { Component, type ErrorInfo, type ReactNode } from 'react'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'

type Props = {
  children: ReactNode
}

type State = {
  hasError: boolean
  error: string | null
}

export class AppErrorBoundary extends Component<Props, State> {
  state: State = {
    hasError: false,
    error: null,
  }

  static getDerivedStateFromError(error: Error): State {
    return {
      hasError: true,
      error: error.message,
    }
  }

  override componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Unhandled application error:', error, info)
  }

  reset = () => {
    this.setState({ hasError: false, error: null })
    window.location.reload()
  }

  override render() {
    if (this.state.hasError) {
      return (
        <div className="flex min-h-screen items-center justify-center px-4 py-8">
          <Card className="w-full max-w-xl border-red-200 bg-red-50">
            <div className="flex items-start gap-4">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-red-100 text-red-600">
                <AlertCircle size={22} />
              </div>
              <div className="flex-1">
                <h1 className="display-font text-2xl font-semibold text-red-900">Something broke</h1>
                <p className="mt-2 text-sm text-red-800">
                  Something unexpected happened. Reloading usually fixes it, and your progress is still saved.
                </p>
                {this.state.error && (
                  <p className="mt-4 rounded-xl bg-white/80 p-3 text-xs text-red-700">
                    {this.state.error}
                  </p>
                )}
                <div className="mt-6 flex flex-wrap gap-3">
                  <Button onClick={this.reset}>
                    <RefreshCw size={16} />
                    Reload app
                  </Button>
                  <Button variant="secondary" onClick={() => window.history.back()}>
                    Go back
                  </Button>
                </div>
              </div>
            </div>
          </Card>
        </div>
      )
    }

    return this.props.children
  }
}
