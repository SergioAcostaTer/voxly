import { AlertCircle, ArrowLeft, Loader2, Mic2, Square, Upload } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { api, ApiClientError } from '../lib/api'
import type { SessionType, SupportedLanguage } from '../types/sessions'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { Input } from '../ui/Input'
import { Logo } from '../ui/Logo'
import { Select } from '../ui/Select'

const SESSION_TYPES: { value: SessionType; label: string; description: string }[] = [
  { value: 'presentation', label: 'Presentation', description: 'Formal business or educational presentation' },
  { value: 'pitch', label: 'Pitch', description: 'Startup or product pitch' },
  { value: 'interview', label: 'Interview', description: 'Practice for job or media interviews' },
  { value: 'freestyle', label: 'Freestyle', description: 'Impromptu speaking practice' },
]

const LANGUAGES: { value: SupportedLanguage; label: string }[] = [
  { value: 'en', label: '🇬🇧 English' },
  { value: 'es', label: '🇪🇸 Español (Spanish)' },
]

const MAX_FILE_SIZE = 100 * 1024 * 1024 // 100MB
const SUPPORTED_UPLOAD_TYPES = ['audio/webm', 'audio/mp4', 'audio/mpeg', 'audio/wav', 'audio/ogg', 'video/mp4']
const RECORDING_MIME_TYPES = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4']

export function NewSessionPage() {
  const navigate = useNavigate()
  const { accessToken, logout } = useAuth()

  const [title, setTitle] = useState('')
  const [type, setType] = useState<SessionType>('presentation')
  const [language, setLanguage] = useState<SupportedLanguage>('en')
  const [file, setFile] = useState<File | null>(null)
  const [dragActive, setDragActive] = useState(false)
  const [isRecording, setIsRecording] = useState(false)
  const [recordingPreviewUrl, setRecordingPreviewUrl] = useState<string | null>(null)

  const [step, setStep] = useState<'details' | 'upload' | 'processing'>('details')
  const [sessionId, setSessionId] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [uploadProgress, setUploadProgress] = useState(0)
  const mediaStreamRef = useRef<MediaStream | null>(null)
  const mediaRecorderRef = useRef<MediaRecorder | null>(null)
  const recordingChunksRef = useRef<BlobPart[]>([])

  useEffect(() => {
    return () => {
      mediaRecorderRef.current?.stream.getTracks().forEach((track) => track.stop())
      if (recordingPreviewUrl) {
        URL.revokeObjectURL(recordingPreviewUrl)
      }
    }
  }, [recordingPreviewUrl])

  const handleDrag = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true)
    } else if (e.type === 'dragleave') {
      setDragActive(false)
    }
  }, [])

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setDragActive(false)

    const droppedFile = e.dataTransfer.files[0]
    if (droppedFile && SUPPORTED_UPLOAD_TYPES.includes(droppedFile.type)) {
      if (droppedFile.size > MAX_FILE_SIZE) {
        setError('File size must be under 100MB')
        return
      }
      setFile(droppedFile)
      if (recordingPreviewUrl) {
        URL.revokeObjectURL(recordingPreviewUrl)
        setRecordingPreviewUrl(null)
      }
      setError(null)
    } else {
      setError('Please upload an audio or video file')
    }
  }, [recordingPreviewUrl])

  const handleFileSelect = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0]
    if (selectedFile) {
      if (!SUPPORTED_UPLOAD_TYPES.includes(selectedFile.type)) {
        setError('Please upload an audio or video file')
        return
      }
      if (selectedFile.size > MAX_FILE_SIZE) {
        setError('File size must be under 100MB')
        return
      }
      setFile(selectedFile)
      if (recordingPreviewUrl) {
        URL.revokeObjectURL(recordingPreviewUrl)
        setRecordingPreviewUrl(null)
      }
      setError(null)
    }
  }, [recordingPreviewUrl])

  const stopRecording = useCallback(() => {
    const recorder = mediaRecorderRef.current
    if (recorder && recorder.state !== 'inactive') {
      recorder.stop()
    }

    mediaStreamRef.current?.getTracks().forEach((track) => track.stop())
    mediaStreamRef.current = null
    setIsRecording(false)
  }, [])

  const startRecording = useCallback(async () => {
    try {
      if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
        setError('Audio recording is not supported in this browser.')
        return
      }

      setError(null)
      setFile(null)
      if (recordingPreviewUrl) {
        URL.revokeObjectURL(recordingPreviewUrl)
        setRecordingPreviewUrl(null)
      }

      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      mediaStreamRef.current = stream

      const mimeType = RECORDING_MIME_TYPES.find((type) => MediaRecorder.isTypeSupported(type))
      const recorder = mimeType ? new MediaRecorder(stream, { mimeType }) : new MediaRecorder(stream)

      recordingChunksRef.current = []
      recorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          recordingChunksRef.current.push(event.data)
        }
      }

      recorder.onstop = () => {
        const blob = new Blob(recordingChunksRef.current, { type: recorder.mimeType || 'audio/webm' })
        const extension = blob.type.includes('mp4') ? 'm4a' : blob.type.includes('mpeg') ? 'mp3' : 'webm'
        const recordedFile = new File(
          [blob],
          `voxly-audio-recording-${Date.now()}.${extension}`,
          { type: blob.type || 'audio/webm' },
        )

        const previewUrl = URL.createObjectURL(blob)
        setRecordingPreviewUrl(previewUrl)
        setFile(recordedFile)
        setError(null)
      }

      mediaRecorderRef.current = recorder
      recorder.start()
      setIsRecording(true)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to start audio recording')
      mediaStreamRef.current?.getTracks().forEach((track) => track.stop())
      mediaStreamRef.current = null
      setIsRecording(false)
    }
  }, [recordingPreviewUrl])

  const toggleRecording = useCallback(() => {
    if (isRecording) {
      stopRecording()
      return
    }

    void startRecording()
  }, [isRecording, startRecording, stopRecording])

  async function handleCreateSession(e: React.FormEvent) {
    e.preventDefault()
    if (!accessToken || !title.trim()) return

    setIsSubmitting(true)
    setError(null)

    try {
      const session = await api.createSession(accessToken, { title: title.trim(), sessionType: type, language })
      setSessionId(session.id)
      setStep('upload')
    } catch (err) {
      if (err instanceof ApiClientError && err.status === 401) {
        await logout()
        navigate('/login', { replace: true })
        return
      }
      setError(err instanceof Error ? err.message : 'Failed to create session')
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleUpload() {
    if (!accessToken || !sessionId || !file) return

    setIsSubmitting(true)
    setError(null)
    setStep('processing')
    setUploadProgress(0)

    try {
      await api.uploadSessionMedia(accessToken, sessionId, file, setUploadProgress)
      setUploadProgress(100)

      // Request analysis
      await api.requestAnalysis(accessToken, sessionId)

      // Navigate to session detail
      navigate(`/sessions/${sessionId}`, { replace: true })
    } catch (err) {
      setStep('upload')
      if (err instanceof ApiClientError && err.status === 401) {
        await logout()
        navigate('/login', { replace: true })
        return
      }
      setError(err instanceof Error ? err.message : 'Failed to upload file')
    } finally {
      setIsSubmitting(false)
    }
  }

  function formatFileSize(bytes: number) {
    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)} KB`
    }
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  }

  return (
    <div className="px-4 py-6 sm:px-6 sm:py-8 lg:py-12">
      <div className="mx-auto w-full max-w-2xl space-y-6">
        <header className="flex items-center justify-between rounded-2xl border border-white/70 bg-white/80 px-4 py-3 shadow-panel backdrop-blur-sm">
          <Logo />
          <Link to="/sessions">
            <Button variant="ghost">
              <ArrowLeft size={18} />
              Back
            </Button>
          </Link>
        </header>

        <Card>
          <div className="mb-6">
            <h1 className="display-font text-2xl font-semibold text-foreground">
              {step === 'details' && 'New Practice Session'}
              {step === 'upload' && 'Record or Upload Your Audio'}
              {step === 'processing' && 'Processing...'}
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {step === 'details' && 'Set up your practice session details'}
              {step === 'upload' && 'Record audio in your browser or upload an audio/video file'}
              {step === 'processing' && 'Uploading and analyzing your recording'}
            </p>
          </div>

          {error && (
            <div className="mb-6 flex items-center gap-2 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-red-600">
              <AlertCircle size={18} />
              <p className="text-sm">{error}</p>
            </div>
          )}

          {step === 'details' && (
            <form onSubmit={handleCreateSession} className="space-y-4">
              <div>
                <label className="mb-2 block text-sm font-medium text-foreground">
                  Session Title
                </label>
                <Input
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="e.g., Q4 Sales Pitch Practice"
                  required
                  maxLength={200}
                />
              </div>

              <div>
                <label className="mb-2 block text-sm font-medium text-foreground">
                  Session Type
                </label>
                <Select value={type} onChange={(e) => setType(e.target.value as SessionType)}>
                  {SESSION_TYPES.map((st) => (
                    <option key={st.value} value={st.value}>
                      {st.label}
                    </option>
                  ))}
                </Select>
                <p className="mt-2 text-sm text-muted-foreground">
                  {SESSION_TYPES.find((st) => st.value === type)?.description}
                </p>
              </div>

              <div>
                <label className="mb-2 block text-sm font-medium text-foreground">
                  Language
                </label>
                <Select value={language} onChange={(e) => setLanguage(e.target.value as SupportedLanguage)}>
                  {LANGUAGES.map((lang) => (
                    <option key={lang.value} value={lang.value}>
                      {lang.label}
                    </option>
                  ))}
                </Select>
                <p className="mt-2 text-sm text-muted-foreground">
                  Choose the language you&apos;ll be speaking
                </p>
              </div>

              <Button type="submit" className="w-full" disabled={isSubmitting || !title.trim()}>
                {isSubmitting ? (
                  <>
                    <Loader2 className="animate-spin" size={18} />
                    Creating...
                  </>
                ) : (
                  'Continue to Upload'
                )}
              </Button>
            </form>
          )}

          {step === 'upload' && (
            <div className="space-y-4">
              <div className="grid gap-3 sm:grid-cols-2">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={toggleRecording}
                  disabled={isSubmitting}
                  className="w-full"
                >
                  {isRecording ? (
                    <>
                      <Square size={18} />
                      Stop Recording
                    </>
                  ) : (
                    <>
                      <Mic2 size={18} />
                      Record Audio
                    </>
                  )}
                </Button>

                <Button
                  type="button"
                  variant="ghost"
                  onClick={() => {
                    stopRecording()
                    setFile(null)
                    if (recordingPreviewUrl) {
                      URL.revokeObjectURL(recordingPreviewUrl)
                      setRecordingPreviewUrl(null)
                    }
                    setError(null)
                  }}
                  disabled={isSubmitting && !isRecording}
                  className="w-full"
                >
                  Clear Recording
                </Button>
              </div>

              {recordingPreviewUrl && file && (
                <Card className="border-primary/20 bg-primary/5">
                  <div className="space-y-2">
                    <p className="text-sm font-medium text-foreground">Recorded audio ready</p>
                    <audio controls src={recordingPreviewUrl} className="w-full" />
                    <p className="text-xs text-muted-foreground">{file.name} · {formatFileSize(file.size)}</p>
                  </div>
                </Card>
              )}

              <div
                className={`relative rounded-xl border-2 border-dashed p-8 text-center transition-colors ${
                  dragActive
                    ? 'border-primary bg-primary/5'
                    : file
                      ? 'border-green-400 bg-green-50'
                      : 'border-border hover:border-primary/50'
                }`}
                onDragEnter={handleDrag}
                onDragLeave={handleDrag}
                onDragOver={handleDrag}
                onDrop={handleDrop}
              >
                <input
                  type="file"
                  accept="audio/*,video/*"
                  onChange={handleFileSelect}
                  className="absolute inset-0 cursor-pointer opacity-0"
                />
                <Upload
                  className={`mx-auto mb-4 ${file ? 'text-green-500' : 'text-muted-foreground'}`}
                  size={40}
                />
                {file ? (
                  <div>
                    <p className="font-semibold text-foreground">{file.name}</p>
                    <p className="mt-1 text-sm text-muted-foreground">
                      {formatFileSize(file.size)}
                    </p>
                    <button
                      type="button"
                      onClick={() => setFile(null)}
                      className="mt-2 text-sm text-primary hover:underline"
                    >
                      Choose different file
                    </button>
                  </div>
                ) : (
                  <div>
                    <p className="font-semibold text-foreground">Drag and drop your audio here</p>
                    <p className="mt-1 text-sm text-muted-foreground">
                      or click to browse (audio or video, max 100MB)
                    </p>
                  </div>
                )}
              </div>

              <Button onClick={handleUpload} className="w-full" disabled={!file || isSubmitting}>
                {isSubmitting ? (
                  <>
                    <Loader2 className="animate-spin" size={18} />
                    Uploading...
                  </>
                ) : (
                  <>
                    <Upload size={18} />
                    Upload and Analyze Audio
                  </>
                )}
              </Button>
            </div>
          )}

          {step === 'processing' && (
            <div className="py-8 text-center">
              <Loader2 className="mx-auto mb-4 animate-spin text-primary" size={48} />
              <p className="font-semibold text-foreground">Processing your presentation</p>
              <p className="mt-1 text-sm text-muted-foreground">
                This may take a moment...
              </p>
              <div className="mx-auto mt-6 h-2 w-full max-w-xs overflow-hidden rounded-full bg-muted">
                <div
                  className="h-full bg-primary transition-all duration-300"
                  style={{ width: `${uploadProgress}%` }}
                />
              </div>
              <p className="mt-2 text-sm text-muted-foreground">{uploadProgress}%</p>
            </div>
          )}
        </Card>
      </div>
    </div>
  )
}
