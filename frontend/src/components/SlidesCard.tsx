import { FileText, Trash2, Upload } from 'lucide-react'
import { useRef, useState } from 'react'
import { ApiClientError, api } from '../lib/api'
import type { MediaFile, Session } from '../types/sessions'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'

const ALLOWED_MIME_TYPES = [
  'application/pdf',
  'application/vnd.ms-powerpoint',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation',
]
const ALLOWED_EXTENSIONS = ['.pdf', '.ppt', '.pptx']
const MAX_SLIDES_SIZE_BYTES = 50 * 1024 * 1024

type SlidesCardProps = {
  accessToken: string
  sessionId: string
  slides: MediaFile | null
  onChange: (session: Session) => void
}

function formatSize(bytes: number) {
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(0)} KB`
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function isPdf(file: MediaFile) {
  return file.contentType === 'application/pdf'
    || file.originalFileName.toLowerCase().endsWith('.pdf')
}

function isAllowed(file: File) {
  const lowerName = file.name.toLowerCase()
  if (ALLOWED_MIME_TYPES.includes(file.type)) return true
  return ALLOWED_EXTENSIONS.some((ext) => lowerName.endsWith(ext))
}

export function SlidesCard({ accessToken, sessionId, slides, onChange }: SlidesCardProps) {
  const inputRef = useRef<HTMLInputElement | null>(null)
  const [isUploading, setIsUploading] = useState(false)
  const [progress, setProgress] = useState(0)
  const [error, setError] = useState<string | null>(null)

  async function handleFile(file: File) {
    setError(null)
    if (!isAllowed(file)) {
      setError('Only PDF, PPT, or PPTX files are allowed.')
      return
    }
    if (file.size > MAX_SLIDES_SIZE_BYTES) {
      setError('Slides file must be under 50 MB.')
      return
    }

    setIsUploading(true)
    setProgress(0)
    try {
      const session = await api.uploadSessionSlides(
        accessToken,
        sessionId,
        file,
        setProgress,
      )
      onChange(session)
    } catch (err) {
      setError(
        err instanceof ApiClientError
          ? err.message
          : 'Failed to upload slides. Please try again.',
      )
    } finally {
      setIsUploading(false)
    }
  }

  async function handleRemove() {
    setError(null)
    setIsUploading(true)
    try {
      const session = await api.deleteSessionSlides(accessToken, sessionId)
      onChange(session)
    } catch (err) {
      setError(
        err instanceof ApiClientError
          ? err.message
          : 'Failed to remove slides.',
      )
    } finally {
      setIsUploading(false)
    }
  }

  return (
    <Card>
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="display-font text-lg font-semibold text-foreground">Slides</p>
          <p className="text-sm text-muted-foreground">
            Attach a PDF or PPTX to keep alongside this session.
          </p>
        </div>
        {slides ? (
          <Button
            variant="ghost"
            onClick={handleRemove}
            disabled={isUploading}
            aria-label="Remove slides"
          >
            <Trash2 size={16} />
          </Button>
        ) : null}
      </div>

      <input
        ref={inputRef}
        type="file"
        accept=".pdf,.ppt,.pptx,application/pdf,application/vnd.ms-powerpoint,application/vnd.openxmlformats-officedocument.presentationml.presentation"
        className="hidden"
        onChange={(event) => {
          const f = event.target.files?.[0]
          if (f) void handleFile(f)
          event.target.value = ''
        }}
      />

      {!slides ? (
        <div className="mt-4">
          <Button
            type="button"
            variant="secondary"
            onClick={() => inputRef.current?.click()}
            disabled={isUploading}
            className="w-full"
          >
            <Upload size={16} className="mr-2" />
            {isUploading ? `Uploading… ${progress}%` : 'Upload slides'}
          </Button>
        </div>
      ) : (
        <div className="mt-4 space-y-3">
          <div className="flex items-center gap-3 rounded-xl border border-border bg-muted/40 p-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <FileText size={18} />
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-foreground">
                {slides.originalFileName}
              </p>
              <p className="text-xs text-muted-foreground">
                {formatSize(slides.sizeBytes)}
              </p>
            </div>
            <a
              href={slides.url}
              target="_blank"
              rel="noopener noreferrer"
              className="text-sm font-medium text-primary hover:underline"
            >
              Open
            </a>
          </div>

          {isPdf(slides) ? (
            <iframe
              src={slides.url}
              title="Slides preview"
              className="h-[480px] w-full rounded-xl border border-border bg-white"
            />
          ) : (
            <p className="rounded-xl border border-dashed border-border bg-muted/30 p-3 text-sm text-muted-foreground">
              PPTX files can be downloaded — convert to PDF for inline preview.
            </p>
          )}
        </div>
      )}

      {error ? (
        <p
          role="alert"
          className="mt-3 rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700"
        >
          {error}
        </p>
      ) : null}
    </Card>
  )
}
