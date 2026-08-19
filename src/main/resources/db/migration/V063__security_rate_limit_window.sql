CREATE TABLE security_rate_limit_window (
    bucket VARCHAR(80) NOT NULL,
    rate_key VARCHAR(255) NOT NULL,
    window_started_at TIMESTAMPTZ NOT NULL,
    request_count INTEGER NOT NULL,
    CONSTRAINT security_rate_limit_window_pk PRIMARY KEY (bucket, rate_key),
    CONSTRAINT security_rate_limit_window_count_ck CHECK (request_count >= 1)
);

CREATE INDEX security_rate_limit_window_started_ix
    ON security_rate_limit_window (window_started_at);
