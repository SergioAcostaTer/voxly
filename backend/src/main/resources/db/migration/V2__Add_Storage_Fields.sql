-- Add storage-related columns to sessions table
ALTER TABLE sessions ADD COLUMN video_url VARCHAR(500);
ALTER TABLE sessions ADD COLUMN video_storage_key VARCHAR(500);
ALTER TABLE sessions ADD COLUMN slides_url VARCHAR(500);
ALTER TABLE sessions ADD COLUMN slides_storage_key VARCHAR(500);
ALTER TABLE sessions ADD COLUMN video_upload_status VARCHAR(50) DEFAULT 'PENDING';
ALTER TABLE sessions ADD COLUMN slides_upload_status VARCHAR(50) DEFAULT 'PENDING';
ALTER TABLE sessions ADD COLUMN uploaded_at TIMESTAMP;

-- Create indexes
CREATE INDEX idx_sessions_video_status ON sessions(video_upload_status);
CREATE INDEX idx_sessions_slides_status ON sessions(slides_upload_status);
