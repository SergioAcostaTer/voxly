# VoxLy Transcription Feature - Implementation Guide

## Overview

The transcription feature enables users to upload video recordings of presentations and automatically generate transcriptions using OpenAI's Whisper API. This guide covers setup, deployment, and API usage.

## Architecture

### Components

1. **WhisperService** (`infrastructure/evaluation/WhisperService.java`)
   - Handles OpenAI Whisper API integration
   - Configurable model selection and language
   - Error handling and response parsing

2. **TranscriptionService** (`application/evaluation/TranscriptionService.java`)
   - Orchestrates the transcription workflow
   - Manages async processing
   - Tracks transcription status

3. **AudioExtractionService** (`infrastructure/evaluation/AudioExtractionService.java`)
   - Extracts audio from video files using FFmpeg
   - Calculates audio duration
   - Cleans up temporary files

4. **Transcription Entity** (`domain/evaluation/Transcription.java`)
   - JPA entity for storing transcription records
   - Tracks status, metadata, and errors

## Setup Instructions

### 1. Environment Variables

Add the following to your `.env` file:

```env
OPENAI_API_KEY=sk-...your-api-key...
FFMPEG_PATH=/usr/bin/ffmpeg    # Linux/Mac
# or on Windows:
FFMPEG_PATH=C:/ffmpeg/bin/ffmpeg.exe
```

### 2. Install FFmpeg

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get install ffmpeg
```

**Mac (Homebrew):**
```bash
brew install ffmpeg
```

**Windows:**
- Download from: https://ffmpeg.org/download.html
- Extract and add to PATH, or set `FFMPEG_PATH` environment variable

### 3. Database Migration

The project now uses Flyway as the single source of truth for schema changes.
JPA runs with `ddl-auto: validate` only.

Migrations live in `src/main/resources/db/migration` and are applied automatically at startup.

If manual migration is needed:
```sql
CREATE TABLE transcriptions (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    original_text TEXT,
    word_count INTEGER,
    duration_seconds INTEGER,
    language VARCHAR(10),
    status VARCHAR(20),
    error_message VARCHAR(1000),
    created_at TIMESTAMP,
    completed_at TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES sessions(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### 4. Configuration

Edit `application.yml`:

```yaml
openai:
  apiKey: ${OPENAI_API_KEY}
  whisper:
    model: whisper-1
    language: en
  gpt:
    model: gpt-3.5-turbo

storage:
  temp-dir: /tmp/voxly-temp
  ffmpeg:
    path: ${FFMPEG_PATH:ffmpeg}
    audio-format: wav
    audio-samplerate: 16000

spring:
  servlet:
    multipart:
      max-file-size: 500MB
      max-request-size: 500MB
```

## API Endpoints

### Request Transcription

**Endpoint:** `POST /v1/evaluations/{sessionId}/transcribe`

**Authentication:** Bearer token required

**Request:**
```bash
curl -X POST \
  -H "Authorization: Bearer your_token" \
  -F "file=@video.mp4" \
  http://localhost:8080/v1/evaluations/{sessionId}/transcribe
```

**Response (202 Accepted):**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "PENDING",
    "originalText": null,
    "durationSeconds": null,
    "wordCount": null,
    "language": "en",
    "errorMessage": null
  },
  "timestamp": "2024-01-01T12:00:00Z"
}
```

**Status Codes:**
- `202 Accepted` - Transcription request accepted
- `400 Bad Request` - Invalid file or missing parameters
- `401 Unauthorized` - Invalid or missing token
- `413 Payload Too Large` - File exceeds 500MB limit
- `500 Internal Server Error` - Server error

### Get Transcription

**Endpoint:** `GET /v1/evaluations/{sessionId}/transcription`

**Authentication:** Bearer token required

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "COMPLETED",
    "originalText": "Hello, today I want to talk about...",
    "durationSeconds": 1200,
    "wordCount": 2450,
    "language": "en",
    "errorMessage": null
  },
  "timestamp": "2024-01-01T12:05:00Z"
}
```

**Status Values:**
- `PENDING` - Waiting to be processed
- `PROCESSING` - Currently transcribing
- `COMPLETED` - Successfully transcribed
- `FAILED` - Transcription failed

## Workflow

1. **User uploads video**
   - POST to `/transcribe` endpoint
   - File is validated and saved temporarily
   - Transcription record created with status PENDING

2. **Async Processing** (runs in background)
   - Audio extracted from video using FFmpeg
   - Audio duration calculated
   - File sent to Whisper API
   - Results stored in database
   - Status updated to COMPLETED or FAILED

3. **User retrieves results**
   - GET `/transcription` endpoint
   - Returns transcription text and metadata

## Error Handling

### Common Errors

**1. FFmpeg not found**
```
Error: "FFmpeg process failed with exit code: 127"
Solution: Install FFmpeg and set FFMPEG_PATH environment variable
```

**2. OpenAI API key invalid**
```
Error: "Whisper API error: invalid api key"
Solution: Verify OPENAI_API_KEY is set correctly
```

**3. File too large**
```
Error: "File exceeds maximum size of 500MB"
Solution: Configure max-file-size in application.yml or compress video
```

**4. Audio extraction failed**
```
Error: "FFmpeg extraction failed with code: 1"
Solution: Verify video format is supported (mp4, mov, avi, webm)
```

## Performance Considerations

### Processing Time

Typical transcription times:
- 1-minute video: 5-10 seconds processing
- 10-minute video: 15-30 seconds processing
- 60-minute video: 1-2 minutes processing

Factors affecting time:
- Audio quality and format
- OpenAI API response time
- Server load

### Optimization Tips

1. **Async Processing**
   - Transcription happens in background
   - Don't wait for completion in HTTP request
   - Poll status endpoint to check progress

2. **File Optimization**
   - Compress video before upload
   - Use MP4 format (most optimized)
   - Ensure mono audio at 16kHz for faster processing

3. **Caching**
   - Store transcriptions in database
   - Avoid re-transcribing same session
   - Check existing transcription before processing

## Testing

### Manual Testing

1. **Upload video:**
```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test.mp4" \
  http://localhost:8080/v1/evaluations/550e8400-e29b-41d4-a716-446655440000/transcribe
```

2. **Check status:**
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/v1/evaluations/550e8400-e29b-41d4-a716-446655440000/transcription
```

### Unit Tests

To add tests:
```java
@Test
public void testTranscriptionService() {
    UUID sessionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    
    Transcription result = transcriptionService
        .requestTranscription(sessionId, userId, videoFile);
    
    assertEquals("PENDING", result.getStatus());
}
```

## Monitoring

### Logs to Check

```bash
# Watch transcription processing
tail -f logs/application.log | grep -i "transcription"

# Check for API errors
grep -i "whisper\|openai" logs/application.log
```

### Database Queries

Check transcription status:
```sql
SELECT * FROM transcriptions 
WHERE session_id = 'your-session-id';

SELECT status, COUNT(*) 
FROM transcriptions 
GROUP BY status;
```

## Troubleshooting

### Transcription stuck in PROCESSING

1. Check logs for errors
2. Verify FFmpeg is installed and accessible
3. Check OpenAI API quota and rate limits
4. Restart application if stuck indefinitely

### Audio extraction fails

- Verify video format is supported
- Check FFmpeg installation
- Test FFmpeg manually: `ffmpeg -i input.mp4 -vn output.wav`

### Whisper API errors

- Check API key validity
- Verify API quota hasn't been exceeded
- Check OpenAI service status
- Ensure audio file is valid WAV format

## Security Considerations

1. **File Upload Security**
   - Max file size: 500MB
   - File type validation (video formats only)
   - Temporary files cleaned up after processing

2. **API Key Security**
   - Use environment variables, not config files
   - Rotate keys regularly
   - Use separate keys for dev/prod

3. **User Authorization**
   - Verify user owns the session
   - Check token validity
   - Log all transcription requests

## Future Enhancements

1. **Batch Processing** - Process multiple files concurrently
2. **Speaker Diarization** - Identify different speakers
3. **Language Detection** - Auto-detect video language
4. **Webhook Notifications** - Notify when transcription completes
5. **Streaming Support** - Process large files in chunks
6. **Caching** - Cache transcriptions to reduce API costs

## Support

For issues or questions:
1. Check logs: `logs/application.log`
2. Verify configuration in `application.yml`
3. Test components individually
4. Check OpenAI API documentation: https://platform.openai.com/docs/guides/speech-to-text
