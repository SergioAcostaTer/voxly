-- Add language support to sessions table
ALTER TABLE sessions ADD COLUMN language VARCHAR(10) DEFAULT 'en';

-- Create index for language for faster queries
CREATE INDEX idx_sessions_language ON sessions(language);
