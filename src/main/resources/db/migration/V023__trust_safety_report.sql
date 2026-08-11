CREATE TABLE trust_report (
    id UUID NOT NULL,
    reporter_member_id UUID NOT NULL REFERENCES member_account(id),
    target_type VARCHAR(30) NOT NULL,
    target_id UUID NOT NULL,
    reason VARCHAR(40) NOT NULL,
    detail VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT trust_report_pk PRIMARY KEY (id),
    CONSTRAINT trust_report_status_ck CHECK (status IN ('OPEN', 'REVIEWED', 'REJECTED')),
    CONSTRAINT trust_report_reason_ck CHECK (reason IN ('SPAM', 'ABUSE', 'PRIVACY', 'ILLEGAL', 'OTHER')),
    CONSTRAINT trust_report_unique UNIQUE (reporter_member_id, target_type, target_id)
);
CREATE INDEX trust_report_queue_ix ON trust_report (status, created_at);
