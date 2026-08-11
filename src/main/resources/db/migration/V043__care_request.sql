CREATE TABLE care_request (
    id UUID NOT NULL,
    requester_member_id UUID NOT NULL REFERENCES member_account(id) ON DELETE RESTRICT,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    location VARCHAR(200) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    reward_hint VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT care_request_pk PRIMARY KEY (id),
    CONSTRAINT care_request_time_ck CHECK (ends_at > starts_at),
    CONSTRAINT care_request_status_ck CHECK (status IN ('OPEN', 'MATCHED', 'CANCELLED', 'EXPIRED'))
);
CREATE INDEX care_request_status_starts_ix ON care_request(status, starts_at, id);
