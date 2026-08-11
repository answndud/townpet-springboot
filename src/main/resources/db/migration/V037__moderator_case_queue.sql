CREATE TABLE moderator_case (
    id UUID NOT NULL,
    case_type VARCHAR(40) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id UUID,
    subject VARCHAR(200) NOT NULL,
    detail VARCHAR(4000),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,
    resolved_by UUID REFERENCES member_account(id),
    CONSTRAINT moderator_case_pk PRIMARY KEY (id),
    CONSTRAINT moderator_case_type_ck CHECK (case_type IN ('CARE_FEEDBACK', 'HOSPITAL_REVIEW', 'DIRECT_MODERATION')),
    CONSTRAINT moderator_case_status_ck CHECK (status IN ('OPEN', 'REVIEWED', 'DISMISSED'))
);

CREATE INDEX moderator_case_queue_ix ON moderator_case(case_type, status, created_at DESC);
