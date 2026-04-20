-- Create analyses table
CREATE TABLE analyses (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL UNIQUE REFERENCES sessions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    transcription_id UUID REFERENCES transcriptions(id) ON DELETE SET NULL,
    metrics JSONB,
    feedback JSONB,
    status VARCHAR(50) DEFAULT 'PENDING',
    error_message VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- Create indexes for analyses
CREATE INDEX idx_analysis_session_id ON analyses(session_id);
CREATE INDEX idx_analysis_user_id ON analyses(user_id);
CREATE INDEX idx_analysis_status ON analyses(status);
CREATE INDEX idx_analysis_created_at ON analyses(created_at);
