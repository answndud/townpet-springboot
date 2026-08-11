CREATE TABLE correction_request (
    id UUID NOT NULL,
    member_id UUID NOT NULL REFERENCES member_account(id),
    title VARCHAR(120) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT correction_request_pk PRIMARY KEY (id),
    CONSTRAINT correction_request_status_ck CHECK (status IN ('OPEN', 'REVIEWED', 'REJECTED'))
);

CREATE INDEX correction_request_queue_ix ON correction_request(status, created_at);
