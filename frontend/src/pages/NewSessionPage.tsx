import { AlertCircle, ArrowLeft, FileText, Loader2, Mic2, Pause, Play, Square, Upload, Video } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AUTH_BYPASS_ENABLED } from '../auth/testing-auth'
import { useAuth } from '../auth/useAuth'
import { AppHeader } from '../components/AppHeader'
import { api, ApiClientError } from '../lib/api'
import type { SessionType, SupportedLanguage } from '../types/sessions'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { Input } from '../ui/Input'
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

const MAX_FILE_SIZE = 500 * 1024 * 1024 // 500MB
const AUDIO_MIME_TYPES = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4']
const VIDEO_MIME_TYPES = ['video/webm;codecs=vp9,opus', 'video/webm;codecs=vp8,opus', 'video/webm', 'video/mp4']
const MIRRORED_VIDEO_STYLE = { transform: 'scaleX(-1)' } as const

function getRecordedFileExtension(mimeType: string) {
  const normalizedMimeType = mimeType.toLowerCase()
  if (normalizedMimeType.includes('wav')) return 'wav'
  if (normalizedMimeType.includes('webm')) return 'webm'
  if (normalizedMimeType.startsWith('video/') && normalizedMimeType.includes('mp4')) return 'mp4'
  if (normalizedMimeType.includes('mp4')) return 'm4a'
  if (normalizedMimeType.includes('mpeg')) return 'mp3'
  if (normalizedMimeType.includes('ogg')) return 'ogg'
  return 'webm'
}

function isSupportedUploadType(fileType: string) {
  return fileType.startsWith('audio/') || fileType.startsWith('video/')
}

function isSupportedSlideType(fileType: string, fileName: string) {
  const normalizedType = fileType.toLowerCase()
  const normalizedName = fileName.toLowerCase()
  return (
    normalizedType === 'application/pdf' ||
    normalizedType === 'application/vnd.ms-powerpoint' ||
    normalizedType === 'application/vnd.openxmlformats-officedocument.presentationml.presentation' ||
    normalizedName.endsWith('.pdf') ||
    normalizedName.endsWith('.ppt') ||
    normalizedName.endsWith('.pptx')
  )
}

export function NewSessionPage() {
  const navigate = useNavigate()
  const { accessToken, logout } = useAuth()

  const [title, setTitle] = useState('')
  const [type, setType] = useState<SessionType>('presentation')
  const [language, setLanguage] = useState<SupportedLanguage>('en')
  const [file, setFile] = useState<File | null>(null)
  const [slideFile, setSlideFile] = useState<File | null>(null)
  const [dragActive, setDragActive] = useState(false)
  const [isRecording, setIsRecording] = useState(false)
  const [isRecordingPaused, setIsRecordingPaused] = useState(false)
  const [recordingElapsedSeconds, setRecordingElapsedSeconds] = useState(0)
  const [recordingPreviewUrl, setRecordingPreviewUrl] = useState<string | null>(null)
  const [hasLivePreview, setHasLivePreview] = useState(false)

  const [recordingMode, setRecordingMode] = useState<'audio' | 'video'>('audio')
  const videoPreviewRef = useRef<HTMLVideoElement>(null)

  const [step, setStep] = useState<'details' | 'upload' | 'processing'>('details')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [uploadProgress, setUploadProgress] = useState(0)
  const mediaStreamRef = useRef<MediaStream | null>(null)
  const mediaRecorderRef = useRef<MediaRecorder | null>(null)
  const recordingChunksRef = useRef<BlobPart[]>([])
  const recordingStartedAtRef = useRef<number | null>(null)
  const recordingPausedAtRef = useRef<number | null>(null)
  const accumulatedPausedMsRef = useRef(0)

  useEffect(() => {
    return () => {
      mediaRecorderRef.current?.stream.getTracks().forEach((track) => track.stop())
      mediaStreamRef.current?.getTracks().forEach((track) => track.stop())
      if (recordingPreviewUrl) {
        URL.revokeObjectURL(recordingPreviewUrl)
      }
    }
  }, [recordingPreviewUrl])

  useEffect(() => {
    if (recordingMode !== 'video' || !mediaStreamRef.current || !videoPreviewRef.current) {
      return
    }

    const previewElement = videoPreviewRef.current
    previewElement.srcObject = mediaStreamRef.current
    previewElement.muted = true
    previewElement.autoplay = true
    previewElement.playsInline = true
    setHasLivePreview(true)

    const tryPlay = async () => {
      try {
        await previewElement.play()
      } catch {
        // Some browsers need metadata before playback can start.
      }
    }

    void tryPlay()
    previewElement.onloadedmetadata = () => {
      void previewElement.play().catch(() => {
        // Ignore autoplay failures; the user still has the camera permission prompt.
      })
    }

    return () => {
      previewElement.onloadedmetadata = null
      previewElement.pause()
      previewElement.srcObject = null
      setHasLivePreview(false)
    }
  }, [isRecording, recordingMode])

  useEffect(() => {
    if (!isRecording || isRecordingPaused) {
      return
    }

    const timer = window.setInterval(() => {
      if (recordingStartedAtRef.current == null) return
      const pausedOffset = accumulatedPausedMsRef.current
      const elapsedMs = Date.now() - recordingStartedAtRef.current - pausedOffset
      setRecordingElapsedSeconds(Math.max(0, Math.floor(elapsedMs / 1000)))
    }, 250)

    return () => window.clearInterval(timer)
  }, [isRecording, isRecordingPaused])

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
      if (droppedFile && isSupportedUploadType(droppedFile.type)) {
        if (droppedFile.size > MAX_FILE_SIZE) {
          setError('File size must be under 500MB')
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
      if (!isSupportedUploadType(selectedFile.type)) {
        setError('Please upload an audio or video file')
        return
      }
      if (selectedFile.size > MAX_FILE_SIZE) {
        setError('File size must be under 500MB')
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

  const handleSlideSelect = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0]
    if (!selectedFile) return

    if (!isSupportedSlideType(selectedFile.type, selectedFile.name)) {
      setError('Please upload slides as a PDF, PPT, or PPTX file')
      return
    }
    if (selectedFile.size > 50 * 1024 * 1024) {
      setError('Slide file size must be under 50MB')
      return
    }

    setSlideFile(selectedFile)
    setError(null)
  }, [])

  const stopRecording = useCallback(() => {
    const recorder = mediaRecorderRef.current
    if (recorder && recorder.state !== 'inactive') {
      try {
        recorder.requestData()
      } catch {
        // Some browsers may throw if requestData isn't available in current state.
      }
      recorder.stop()
    }
    setIsRecording(false)
    setIsRecordingPaused(false)
    setHasLivePreview(false)
  }, [])

  const togglePauseRecording = useCallback(() => {
    const recorder = mediaRecorderRef.current
    if (!recorder) return

    if (recorder.state === 'recording') {
      recorder.pause()
      recordingPausedAtRef.current = Date.now()
      setIsRecordingPaused(true)
      return
    }

    if (recorder.state === 'paused') {
      if (recordingPausedAtRef.current != null) {
        accumulatedPausedMsRef.current += Date.now() - recordingPausedAtRef.current
      }
      recordingPausedAtRef.current = null
      recorder.resume()
      setIsRecordingPaused(false)
    }
  }, [])

  const startRecording = useCallback(async () => {
    try {
      if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
        setError('Recording is not supported in this browser.')
        return
      }

      setError(null)
      setFile(null)
      if (recordingPreviewUrl) {
        URL.revokeObjectURL(recordingPreviewUrl)
        setRecordingPreviewUrl(null)
      }

      const isVideo = recordingMode === 'video'
      const constraints = isVideo
        ? {
            audio: true,
            video: {
              facingMode: 'user',
              width: { ideal: 1280 },
              height: { ideal: 720 },
            },
          }
        : { audio: true }
      const stream = await navigator.mediaDevices.getUserMedia(constraints)
      mediaStreamRef.current = stream

      const mimeTypes = isVideo ? VIDEO_MIME_TYPES : AUDIO_MIME_TYPES
      const mimeType = mimeTypes.find((type) => MediaRecorder.isTypeSupported(type))
      const recorderOptions = mimeType
        ? {
            mimeType,
            audioBitsPerSecond: 128_000,
            ...(isVideo ? { videoBitsPerSecond: 1_500_000 } : {}),
          }
        : {
            audioBitsPerSecond: 128_000,
            ...(isVideo ? { videoBitsPerSecond: 1_500_000 } : {}),
          }
      const recorder = new MediaRecorder(stream, recorderOptions)
      const effectiveMimeType = recorder.mimeType || mimeType || (isVideo ? 'video/webm' : 'audio/webm')
      recordingStartedAtRef.current = Date.now()
      recordingPausedAtRef.current = null
      accumulatedPausedMsRef.current = 0
      setRecordingElapsedSeconds(0)
      setIsRecordingPaused(false)
      recordingChunksRef.current = []
      recorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          recordingChunksRef.current.push(event.data)
        }
      }

      recorder.onstop = () => {
        mediaStreamRef.current?.getTracks().forEach((track) => track.stop())
        mediaStreamRef.current = null
        setHasLivePreview(false)

        const recordedBlob = new Blob(recordingChunksRef.current, { type: effectiveMimeType })
        const extension = getRecordedFileExtension(effectiveMimeType)
        const prefix = isVideo ? 'voxly-video-recording' : 'voxly-audio-recording'
        const recordedFileName = `${prefix}-${Date.now()}.${extension}`
        const recordedFile = new File([recordedBlob], recordedFileName, { type: effectiveMimeType })
        const previewUrl = URL.createObjectURL(recordedBlob)
        setFile(recordedFile)
        setRecordingPreviewUrl(previewUrl)
        setError(null)
      }

      mediaRecorderRef.current = recorder
      recorder.start()
      setIsRecording(true)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to start recording')
      mediaStreamRef.current?.getTracks().forEach((track) => track.stop())
      mediaStreamRef.current = null
      setIsRecording(false)
      setHasLivePreview(false)
    }
  }, [recordingPreviewUrl, recordingMode])

  const toggleRecording = useCallback(() => {
    if (isRecording) {
      stopRecording()
      return
    }

    void startRecording()
  }, [isRecording, startRecording, stopRecording])

  async function handleCreateSession(e: React.FormEvent) {
    e.preventDefault()
    if (!title.trim()) return
    setError(null)
    setStep('upload')
  }

  async function handleUpload() {
    if (!accessToken || !title.trim() || !file) return

    setIsSubmitting(true)
    setError(null)
    setStep('processing')
    setUploadProgress(0)

    try {
      const session = await api.createSessionWithChunkedMedia(
        accessToken,
        { title: title.trim(), sessionType: type, language },
        file,
        setUploadProgress,
      )

      if (slideFile) {
        await api.uploadSessionSlides(accessToken, session.id, slideFile)
      }

      await api.requestAnalysis(accessToken, session.id)
      setUploadProgress(100)

      navigate(`/sessions/${session.id}`, { replace: true })
    } catch (err) {
      setStep('upload')
      if (err instanceof ApiClientError && err.status === 401) {
        if (AUTH_BYPASS_ENABLED) {
          setError('We could not verify your access right now. Please try again.')
          return
        }
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

  function formatRecordingTime(totalSeconds: number) {
    const mins = Math.floor(totalSeconds / 60)
    const secs = totalSeconds % 60
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }

  return (
    <div className="px-4 pb-6 pt-28 sm:px-6 sm:pb-8 lg:px-8 lg:pb-12">
      <div className="mx-auto w-full max-w-2xl space-y-6">
        <AppHeader
          rightSlot={
            <Link to="/sessions">
              <Button variant="ghost">
                <ArrowLeft size={18} />
                Back
              </Button>
            </Link>
          }
        />

        <Card>
          <div className="mb-6">
            <h1 className="display-font text-2xl font-semibold text-foreground">
              {step === 'details' && 'New Practice Session'}
              {step === 'upload' && 'Add Your Recording'}
              {step === 'processing' && 'Almost done...'}
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {step === 'details' && 'Set up your practice session details'}
              {step === 'upload' && 'Record in your browser or upload an audio/video file'}
              {step === 'processing' && 'Uploading your recording and preparing your feedback'}
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
              {/* Recording mode toggle */}
              <div className="flex rounded-xl border border-border overflow-hidden">
                <button
                  type="button"
                  onClick={() => { if (!isRecording) setRecordingMode('audio') }}
                  disabled={isRecording}
                  className={`flex flex-1 items-center justify-center gap-2 py-2.5 text-sm font-medium transition-colors ${
                    recordingMode === 'audio'
                      ? 'bg-primary text-primary-foreground'
                      : 'text-muted-foreground hover:bg-muted'
                  }`}
                >
                  <Mic2 size={16} /> Audio only
                </button>
                <button
                  type="button"
                  onClick={() => { if (!isRecording) setRecordingMode('video') }}
                  disabled={isRecording}
                  className={`flex flex-1 items-center justify-center gap-2 py-2.5 text-sm font-medium transition-colors ${
                    recordingMode === 'video'
                      ? 'bg-primary text-primary-foreground'
                      : 'text-muted-foreground hover:bg-muted'
                  }`}
                >
                  <Video size={16} /> Video (posture analysis)
                </button>
              </div>

              {recordingMode === 'video' && (
                <div className="overflow-hidden rounded-xl border border-border bg-black">
                  <video
                    ref={videoPreviewRef}
                    className={`w-full bg-black object-cover ${hasLivePreview ? 'block' : 'hidden'}`}
                    style={{ maxHeight: 260, minHeight: hasLivePreview ? 220 : 0, ...MIRRORED_VIDEO_STYLE }}
                    playsInline
                    autoPlay
                    muted
                    aria-label="Live camera preview"
                  />
                  {!hasLivePreview && (
                    <div
                      className="flex items-center justify-center text-sm text-white/70"
                      style={{ height: 220 }}
                    >
                      Camera preview will appear here when recording starts.
                    </div>
                  )}
                </div>
              )}

              {isRecording && (
                <Card className="border-primary/20 bg-primary/5">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-[0.16em] text-primary/80">
                        Live recording
                      </p>
                      <p className="mt-1 font-mono text-2xl text-foreground">
                        {formatRecordingTime(recordingElapsedSeconds)}
                      </p>
                    </div>
                    <span className={`rounded-full px-3 py-1 text-xs font-medium ${
                      isRecordingPaused ? 'bg-amber-100 text-amber-700' : 'bg-rose-100 text-rose-700'
                    }`}>
                      {isRecordingPaused ? 'Paused' : 'Recording'}
                    </span>
                  </div>
                </Card>
              )}

              <div className="grid gap-3 sm:grid-cols-3">
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
                  ) : recordingMode === 'video' ? (
                    <>
                      <Video size={18} />
                      Record Video
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
                  variant="secondary"
                  onClick={togglePauseRecording}
                  disabled={!isRecording || isSubmitting}
                  className="w-full"
                >
                  {isRecordingPaused ? (
                    <>
                      <Play size={18} />
                      Resume
                    </>
                  ) : (
                    <>
                      <Pause size={18} />
                      Pause
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
                    setRecordingElapsedSeconds(0)
                    setIsRecordingPaused(false)
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
                    <p className="text-sm font-medium text-foreground">
                      {file.type.startsWith('video/') ? 'Recorded video ready' : 'Recorded audio ready'}
                    </p>
                    {file.type.startsWith('video/') ? (
                      <video controls src={recordingPreviewUrl} className="w-full rounded-lg" style={MIRRORED_VIDEO_STYLE} />
                    ) : (
                      <audio controls src={recordingPreviewUrl} className="w-full" />
                    )}
                    <p className="text-xs text-muted-foreground">{file.name} · {formatFileSize(file.size)}</p>
                    <a
                      href={recordingPreviewUrl}
                      download={file.name}
                      className="text-sm text-primary hover:underline"
                    >
                      Download recording
                    </a>
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
                    <p className="font-semibold text-foreground">Drag and drop your file here</p>
                    <p className="mt-1 text-sm text-muted-foreground">
                      or click to browse (audio or video, max 500MB)
                    </p>
                  </div>
                )}
              </div>

              <div className="rounded-xl border border-border bg-muted/10 p-4">
                <div className="mb-3 flex items-center gap-2">
                  <FileText className="text-primary" size={18} />
                  <p className="font-semibold text-foreground">Slides</p>
                </div>
                <label className="block">
                  <Input
                    type="file"
                    accept=".pdf,.ppt,.pptx,application/pdf,application/vnd.ms-powerpoint,application/vnd.openxmlformats-officedocument.presentationml.presentation"
                    onChange={handleSlideSelect}
                  />
                </label>
                {slideFile ? (
                  <div className="mt-3 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2">
                    <p className="text-sm font-medium text-emerald-800">
                      {slideFile.name} · {formatFileSize(slideFile.size)}
                    </p>
                    <button
                      type="button"
                      className="text-sm font-semibold text-emerald-700 hover:underline"
                      onClick={() => setSlideFile(null)}
                    >
                      Remove
                    </button>
                  </div>
                ) : (
                  <p className="mt-2 text-sm text-muted-foreground">
                    Optional PDF, PPT, or PPTX deck for session context.
                  </p>
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
                    Upload and Analyze
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
                This can take a minute...
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
