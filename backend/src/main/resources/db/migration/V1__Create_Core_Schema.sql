CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(256) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(256) NOT NULL,
    is_active BOOLEAN NOT NULL,
    email_verified BOOLEAN NOT NULL,
    email_verification_token VARCHAR(100),
    email_verification_token_expiry TIMESTAMP,
    password_reset_token VARCHAR(100),
    password_reset_token_expiry TIMESTAMP,
    failed_login_attempts INTEGER NOT NULL,
    lockout_end TIMESTAMP,
    two_factor_enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    modified_at TIMESTAMP
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_name VARCHAR(50),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE user_refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    token VARCHAR(512) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN NOT NULL,
    revoked_at TIMESTAMP,
    replaced_by_token VARCHAR(512),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token ON user_refresh_tokens(token);

CREATE TABLE user_two_factor_codes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    code VARCHAR(6) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN NOT NULL,
    used_at TIMESTAMP,
    CONSTRAINT fk_two_factor_codes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    session_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    media_storage_path VARCHAR(255),
    media_original_filename VARCHAR(255),
    media_content_type VARCHAR(100),
    media_size_bytes BIGINT,
    media_duration_seconds DOUBLE PRECISION,
    evaluation_id UUID,
    created_at TIMESTAMP NOT NULL,
    modified_at TIMESTAMP,
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE evaluations (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    session_type VARCHAR(50),
    transcription_text TEXT,
    transcription_json TEXT,
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
    error_message VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_evaluations_session FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_evaluations_user FOREIGN KEY (user_id) REFERENCES users(id)
);
