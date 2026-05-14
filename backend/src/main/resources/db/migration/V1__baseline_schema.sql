CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(256) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(256) NOT NULL,
    professional_role VARCHAR(100),
    coaching_focus VARCHAR(500),
    is_active BOOLEAN NOT NULL,
    email_verified BOOLEAN NOT NULL,
    email_verification_token VARCHAR(100),
    email_verification_token_expiry TIMESTAMP WITH TIME ZONE,
    password_reset_token VARCHAR(100),
    password_reset_token_expiry TIMESTAMP WITH TIME ZONE,
    failed_login_attempts INTEGER NOT NULL,
    lockout_end TIMESTAMP WITH TIME ZONE,
    two_factor_enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_name VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS user_refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(512) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_revoked BOOLEAN NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_token VARCHAR(512)
);

CREATE INDEX IF NOT EXISTS idx_refresh_token ON user_refresh_tokens(token);

CREATE TABLE IF NOT EXISTS user_two_factor_codes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code VARCHAR(6) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_used BOOLEAN NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_two_factor_codes_user_id ON user_two_factor_codes(user_id);

CREATE TABLE IF NOT EXISTS config (
    config_key VARCHAR(100) PRIMARY KEY,
    boolean_value BOOLEAN NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO config (config_key, boolean_value, updated_at)
VALUES ('email_verification_required', false, CURRENT_TIMESTAMP)
ON CONFLICT (config_key) DO NOTHING;

CREATE TABLE IF NOT EXISTS sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    session_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    media_storage_path VARCHAR(1024),
    media_original_filename VARCHAR(255),
    media_content_type VARCHAR(100),
    media_size_bytes BIGINT,
    media_duration_seconds DOUBLE PRECISION,
    slide_storage_path VARCHAR(1024),
    slide_original_filename VARCHAR(255),
    slide_content_type VARCHAR(100),
    slide_size_bytes BIGINT,
    evaluation_id UUID,
    language VARCHAR(10),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_status ON sessions(status);

CREATE TABLE IF NOT EXISTS evaluations (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL UNIQUE REFERENCES sessions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    session_type VARCHAR(50),
    transcription_text TEXT,
    transcription_json TEXT,
    transcription_words_json TEXT,
    transcription_raw_json TEXT,
    duration_seconds DOUBLE PRECISION,
    detected_language VARCHAR(10),
    words_per_minute INTEGER,
    total_words INTEGER,
    filler_word_count INTEGER,
    pause_count INTEGER,
    clarity_score DOUBLE PRECISION,
    metrics_json TEXT,
    feedback_json TEXT,
    overall_summary TEXT,
    strengths_json TEXT,
    improvements_json TEXT,
    posture_score DOUBLE PRECISION,
    posture_grade VARCHAR(10),
    posture_gesture_summaries_json TEXT,
    posture_timeline_json TEXT,
    posture_penalty_breakdown_json TEXT,
    posture_recommendations_json TEXT,
    error_message TEXT,
    processing_started_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_evaluations_user_id ON evaluations(user_id);
CREATE INDEX IF NOT EXISTS idx_evaluations_status ON evaluations(status);
