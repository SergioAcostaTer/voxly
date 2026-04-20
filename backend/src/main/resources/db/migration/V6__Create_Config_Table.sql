CREATE TABLE IF NOT EXISTS config (
    config_key VARCHAR(100) PRIMARY KEY,
    boolean_value BOOLEAN NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO config (config_key, boolean_value)
VALUES ('auth.require_email_verification', FALSE)
ON CONFLICT (config_key) DO NOTHING;
