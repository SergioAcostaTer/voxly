import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { api } from '../lib/api';
import { Button } from '../ui/Button';
import { Card } from '../ui/Card';

interface TranscriptionResponse {
  id: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  originalText: string | null;
  durationSeconds: number | null;
  wordCount: number | null;
  language: string;
  errorMessage: string | null;
}

export const TranscriptionUploadComponent: React.FC = () => {
  const { sessionId } = useParams<{ sessionId: string }>();
  const { accessToken } = useAuth();
  
  const [file, setFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [transcription, setTranscription] = useState<TranscriptionResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pollingActive, setPollingActive] = useState(false);

  // Stream transcription status
  useEffect(() => {
    if (!pollingActive || !sessionId || !accessToken) return;

    const abortController = new AbortController();

    void api.streamSessionEvents(
      accessToken,
      sessionId,
      async ({ event }) => {
        if (event === 'status' || event === 'connected') {
          try {
            const data = await api.getTranscription(accessToken, sessionId);
            setTranscription(data);

            if (data.status === 'COMPLETED' || data.status === 'FAILED') {
              setPollingActive(false);
            }
          } catch (err) {
            console.error('Error streaming transcription:', err);
          }
        }
      },
      abortController.signal,
    ).catch((err) => {
      if (!abortController.signal.aborted) {
        console.error('Error opening transcription stream:', err);
      }
    });

    return () => abortController.abort();
  }, [pollingActive, sessionId, accessToken]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const selectedFile = e.target.files[0];

      // Validate file size (500MB)
      if (selectedFile.size > 500 * 1024 * 1024) {
        setError('File exceeds 500MB limit');
        return;
      }

      // Validate file type
      const validTypes = ['video/mp4', 'video/quicktime', 'video/x-msvideo', 'video/webm'];
      if (!validTypes.includes(selectedFile.type)) {
        setError('Invalid video format. Allowed: MP4, MOV, AVI, WebM');
        return;
      }

      setFile(selectedFile);
      setError(null);
    }
  };

  const handleUpload = async () => {
    if (!file || !sessionId || !accessToken) {
      setError('Missing required information');
      return;
    }

    setIsUploading(true);
    setError(null);

    try {
      const data = await api.requestTranscription(accessToken, sessionId, file);
      setTranscription(data);
      setFile(null);
      setPollingActive(true); // Start polling for updates
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload failed');
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="space-y-6">
      <Card>
        <div className="space-y-4">
          <h3 className="text-lg font-semibold">Video Transcription</h3>

          {error && (
            <div className="p-4 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
              {error}
            </div>
          )}

          {!transcription || transcription.status === 'FAILED' ? (
            <div className="space-y-4">
              <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center">
                <input
                  type="file"
                  accept="video/mp4,video/quicktime,video/x-msvideo,video/webm"
                  onChange={handleFileChange}
                  disabled={isUploading}
                  className="hidden"
                  id="video-input"
                />
                <label htmlFor="video-input" className="cursor-pointer">
                  <div className="text-gray-600">
                    {file ? (
                      <>
                        <p className="font-medium">{file.name}</p>
                        <p className="text-sm">
                          {(file.size / (1024 * 1024)).toFixed(2)} MB
                        </p>
                      </>
                    ) : (
                      <>
                        <p>Drag and drop your video here</p>
                        <p className="text-sm">or click to select</p>
                        <p className="text-xs text-gray-500 mt-2">
                          Max 500MB • MP4, MOV, AVI, WebM
                        </p>
                      </>
                    )}
                  </div>
                </label>
              </div>

              <Button
                onClick={handleUpload}
                disabled={!file || isUploading}
                className="w-full"
              >
                {isUploading ? 'Uploading...' : 'Upload for Transcription'}
              </Button>
            </div>
          ) : (
            <div className="space-y-4">
              <div className="flex items-center justify-between p-4 bg-gray-50 rounded">
                <span className="text-sm font-medium">Status:</span>
                <span className={`px-3 py-1 rounded text-sm font-medium ${
                  transcription.status === 'COMPLETED'
                    ? 'bg-green-100 text-green-800'
                    : transcription.status === 'PROCESSING'
                    ? 'bg-blue-100 text-blue-800'
                    : 'bg-gray-100 text-gray-800'
                }`}>
                  {transcription.status}
                </span>
              </div>

              {transcription.status === 'COMPLETED' && transcription.originalText && (
                <>
                  <div className="grid grid-cols-3 gap-4">
                    <div className="p-3 bg-blue-50 rounded">
                      <p className="text-xs text-gray-600">Duration</p>
                      <p className="text-lg font-semibold">
                        {transcription.durationSeconds
                          ? `${Math.floor(transcription.durationSeconds / 60)}:${String(
                              transcription.durationSeconds % 60
                            ).padStart(2, '0')}`
                          : 'N/A'}
                      </p>
                    </div>
                    <div className="p-3 bg-green-50 rounded">
                      <p className="text-xs text-gray-600">Words</p>
                      <p className="text-lg font-semibold">
                        {transcription.wordCount?.toLocaleString() || 'N/A'}
                      </p>
                    </div>
                    <div className="p-3 bg-purple-50 rounded">
                      <p className="text-xs text-gray-600">Language</p>
                      <p className="text-lg font-semibold">
                        {transcription.language.toUpperCase()}
                      </p>
                    </div>
                  </div>

                  <div className="p-4 bg-gray-50 rounded max-h-64 overflow-y-auto">
                    <p className="text-sm text-gray-700 whitespace-pre-wrap">
                      {transcription.originalText}
                    </p>
                  </div>

                  <Button
                    onClick={() => {
                      setTranscription(null);
                      setFile(null);
                    }}
                    variant="secondary"
                    className="w-full"
                  >
                    Upload Another Video
                  </Button>
                </>
              )}

              {(transcription.status === 'PENDING' || transcription.status === 'PROCESSING') && (
                <div className="p-4 bg-blue-50 rounded">
                  <div className="flex items-center">
                    <div className="animate-spin h-5 w-5 text-blue-600 mr-3"></div>
                    <span className="text-sm text-blue-700">
                      Processing video... This may take a few minutes
                    </span>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </Card>
    </div>
  );
};

export default TranscriptionUploadComponent;
