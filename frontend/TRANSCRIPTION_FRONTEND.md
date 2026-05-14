# VoxLy Frontend - Transcription Feature Integration

## Overview

The frontend transcription component allows users to upload video recordings and view their transcriptions. This guide covers integration, usage, and customization.

## Components

### TranscriptionUploadComponent

Located at: `src/components/TranscriptionUploadComponent.tsx`

**Features:**
- Drag-and-drop video upload
- File validation (type and size)
- Real-time status polling
- Displays transcription results
- Metadata display (duration, word count, language)

## Integration

### 1. Add to Your Page

```typescript
import { TranscriptionUploadComponent } from '@/components/TranscriptionUploadComponent';

export const SessionDetailPage: React.FC = () => {
  const { sessionId } = useParams();
  
  return (
    <div className="space-y-6">
      <h1>Session Details</h1>
      <TranscriptionUploadComponent />
    </div>
  );
};
```

### 2. Environment Setup

Add to `.env`:
```env
VITE_API_URL=http://localhost:8080/api
```

### 3. Authentication

The component uses the `useAuth` hook to get the authentication token:

```typescript
const { token } = useAuth();
```

Ensure your auth context provides the token.

## Component Props

Currently, the component derives parameters from the route:
- `sessionId` - From URL params (`:sessionId`)
- `token` - From auth context

**Future enhancement:** Accept props for more flexibility

```typescript
interface TranscriptionUploadComponentProps {
  sessionId: string;
  onTranscriptionComplete?: (transcription: TranscriptionResponse) => void;
  maxFileSize?: number; // in bytes
  pollInterval?: number; // in ms
}
```

## API Integration

### Endpoints Used

1. **Upload Transcription Request**
   ```
   POST /v1/evaluations/{sessionId}/transcribe
   ```

2. **Get Transcription Status**
   ```
   GET /v1/evaluations/{sessionId}/transcription
   ```

### Error Handling

The component handles various error scenarios:

```typescript
// File too large (413)
if (response.status === 413) {
  setError('File exceeds maximum size of 500MB');
}

// API errors
const errorData = await response.json();
setError(errorData.errors?.[0]?.message || 'Upload failed');

// Network errors
catch (err) {
  setError(err instanceof Error ? err.message : 'Upload failed');
}
```

## Status Polling

The component automatically polls for transcription status:

```typescript
// Poll every 3 seconds
const pollInterval = setInterval(async () => {
  // Check status endpoint
}, 3000);

// Stop polling when completed or failed
if (data.data.status === 'COMPLETED' || data.data.status === 'FAILED') {
  setPollingActive(false);
}
```

**To customize polling interval:**
```typescript
// Change the interval in the useEffect dependency
const pollInterval = setInterval(async () => {
  // ...
}, 5000); // Change to 5 seconds
```

## UI/UX Features

### File Upload Area

- Drag-and-drop support
- Visual feedback for selected file
- File size display
- Allowed formats hint

### Status Indicators

```
PENDING    → Gray badge
PROCESSING → Blue badge with spinner
COMPLETED  → Green badge
FAILED     → Red badge
```

### Results Display

When transcription is complete:
- Duration in MM:SS format
- Word count with thousands separator
- Language code (e.g., EN, ES, FR)
- Full transcription text in scrollable box

## Customization

### Change Polling Interval

```typescript
// In useEffect hook
const pollInterval = setInterval(async () => {
  // ...
}, 5000); // Change from 3000 to 5000ms
```

### Modify File Size Limit

```typescript
// Update validation
if (selectedFile.size > 1024 * 1024 * 1024) { // 1GB instead of 500MB
  setError('File exceeds 1GB limit');
  return;
}
```

### Add More Supported Formats

```typescript
const validTypes = [
  'video/mp4',
  'video/quicktime',
  'video/x-msvideo',
  'video/webm',
  'video/mpeg', // Add MPEG
];
```

### Style Customization

The component uses Tailwind CSS classes. Modify to match your design:

```typescript
// Change status badge colors
className={`px-3 py-1 rounded text-sm font-medium ${
  transcription.status === 'COMPLETED'
    ? 'bg-green-100 text-green-800' // Customize here
    : // ...
}`}
```

## Usage Examples

### Basic Integration

```typescript
<TranscriptionUploadComponent />
```

### With Callback Handler (Future Enhancement)

```typescript
const handleTranscriptionComplete = (transcription) => {
  console.log('Transcription complete:', transcription);
  // Save to database
  // Update UI
  // Send notification
};

<TranscriptionUploadComponent 
  onTranscriptionComplete={handleTranscriptionComplete}
/>
```

### In a Session Detail Page

```typescript
import { SessionDetailPage } from '@/pages/SessionDetailPage';
import { TranscriptionUploadComponent } from '@/components/TranscriptionUploadComponent';

export const EnhancedSessionDetailPage: React.FC = () => {
  const { sessionId } = useParams();

  return (
    <div className="grid grid-cols-3 gap-6">
      <div className="col-span-2">
        <SessionDetailPage />
      </div>
      <div className="col-span-1">
        <TranscriptionUploadComponent />
      </div>
    </div>
  );
};
```

## Error Handling

### User-Friendly Error Messages

```
"File exceeds 500MB limit"
→ User should compress or split video

"Invalid video format. Allowed: MP4, MOV, AVI, WebM"
→ User should convert video format

"File exceeds maximum size of 500MB"
→ Backend rejected file size

"Upload failed"
→ Generic error - check network connection
```

### Developer Error Messages

Check browser console for:
- Network errors
- JSON parsing errors
- Auth token issues

```typescript
catch (err) {
  console.error('Upload error:', err);
}
```

## Testing

### Manual Testing

1. **Test File Upload**
   - Upload small MP4 file (< 10MB)
   - Verify success message

2. **Test Status Polling**
   - Upload file
   - Observe status changes
   - Should see PENDING → PROCESSING → COMPLETED

3. **Test Error Handling**
   - Upload too large file
   - Upload wrong format
   - Verify error messages

4. **Test Results Display**
   - After completion
   - Verify text displays correctly
   - Check metadata display

### Unit Testing Example

```typescript
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { TranscriptionUploadComponent } from '@/components/TranscriptionUploadComponent';

describe('TranscriptionUploadComponent', () => {
  it('should display upload area initially', () => {
    render(<TranscriptionUploadComponent />);
    expect(screen.getByText(/drag and drop/i)).toBeInTheDocument();
  });

  it('should validate file size', () => {
    const { getByLabelText } = render(<TranscriptionUploadComponent />);
    const input = getByLabelText(/video-input/i) as HTMLInputElement;
    
    // Create large file
    const largeFile = new File(['x'], 'video.mp4', {
      size: 600 * 1024 * 1024,
      type: 'video/mp4',
    });
    
    fireEvent.change(input, { target: { files: [largeFile] } });
    
    expect(screen.getByText(/exceeds 500MB/i)).toBeInTheDocument();
  });
});
```

## Performance Optimization

### 1. Lazy Load Component

```typescript
import { lazy, Suspense } from 'react';

const TranscriptionUpload = lazy(() => 
  import('@/components/TranscriptionUploadComponent')
);

export const SessionPage: React.FC = () => {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <TranscriptionUpload />
    </Suspense>
  );
};
```

### 2. Memoize Component

```typescript
import { memo } from 'react';

export const TranscriptionUploadComponent = memo(() => {
  // Component code
});
```

### 3. Reduce Polling Frequency

```typescript
// For better performance, increase interval
const pollInterval = setInterval(async () => {
  // ...
}, 10000); // Check every 10 seconds instead of 3
```

## Accessibility

The component can be enhanced for better accessibility:

```typescript
<div 
  role="status"
  aria-live="polite"
  aria-label="Transcription status"
>
  {/* Status indicator */}
</div>

<input
  id="video-input"
  aria-label="Upload video file for transcription"
  aria-describedby="upload-help"
/>

<div id="upload-help">
  Maximum 500MB • Supported: MP4, MOV, AVI, WebM
</div>
```

## Future Enhancements

1. **Progress Bar**
   - Show upload progress
   - Show transcription progress

2. **Drag & Drop Zones**
   - Add visual feedback on hover
   - Show drop zone highlighting

3. **Multiple File Support**
   - Queue multiple videos
   - Show upload queue

4. **Transcription Editor**
   - Edit transcription text
   - Highlight corrections

5. **Export Options**
   - Download as TXT
   - Download as PDF with metadata

6. **Language Selection**
   - Let user specify language
   - Show auto-detected language

7. **Real-time WebSocket Updates**
   - Replace polling with WebSocket
   - Get instant updates

## Troubleshooting

### Component Not Showing

1. Check route params contain `sessionId`
2. Verify auth context provides token
3. Check browser console for errors

### Upload Fails

1. Check API endpoint URL in `.env`
2. Verify authentication token is valid
3. Check backend is running

### Status Not Updating

1. Check polling interval (default 3s)
2. Verify API response format
3. Check browser network tab

### Styling Issues

1. Ensure Tailwind CSS is configured
2. Check component className syntax
3. Verify UI component imports

## Support

For issues:
1. Check browser console
2. Check network tab (F12)
3. Verify backend is running
4. Check API response format
5. Review error messages

## API Reference

See `TRANSCRIPTION_SETUP.md` for full API documentation.

Quick reference:
- POST `/v1/evaluations/{sessionId}/transcribe`
- GET `/v1/evaluations/{sessionId}/transcription`
