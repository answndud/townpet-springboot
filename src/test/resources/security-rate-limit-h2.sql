CREATE TABLE IF NOT EXISTS security_rate_limit_window (
    bucket VARCHAR(80) NOT NULL,
    rate_key VARCHAR(255) NOT NULL,
    window_started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    request_count INTEGER NOT NULL,
    PRIMARY KEY (bucket, rate_key),
    CHECK (request_count >= 1)
);

CREATE INDEX IF NOT EXISTS security_rate_limit_window_started_ix
    ON security_rate_limit_window (window_started_at);
