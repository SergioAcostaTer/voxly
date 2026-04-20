-- Create transcriptions table
CREATE TABLE transcriptions (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL UNIQUE REFERENCES sessions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    original_text TEXT,
    word_count INTEGER,
    duration_seconds INTEGER,
    language VARCHAR(10) DEFAULT 'en',
    status VARCHAR(50) DEFAULT 'PENDING',
    error_message VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- Create indexes for transcriptions
CREATE INDEX idx_transcription_session_id ON transcriptions(session_id);
CREATE INDEX idx_transcription_user_id ON transcriptions(user_id);
CREATE INDEX idx_transcription_status ON transcriptions(status);
CREATE INDEX idx_transcription_created_at ON transcriptions(created_at);
