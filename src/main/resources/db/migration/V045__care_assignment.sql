CREATE TABLE care_assignment (
    id UUID NOT NULL,
    request_id UUID NOT NULL REFERENCES care_request(id) ON DELETE CASCADE,
    caregiver_member_id UUID NOT NULL REFERENCES member_account(id) ON DELETE RESTRICT,
    status VARCHAR(30) NOT NULL DEFAULT 'MATCHED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT care_assignment_pk PRIMARY KEY (id),
    CONSTRAINT care_assignment_request_uk UNIQUE (request_id),
    CONSTRAINT care_assignment_status_ck CHECK (status IN ('MATCHED','IN_PROGRESS','COMPLETED','CANCELLED_BY_REQUESTER','CANCELLED_BY_CAREGIVER','ABORTED'))
);
CREATE TABLE care_feedback (
    id UUID NOT NULL,
    assignment_id UUID NOT NULL REFERENCES care_assignment(id) ON DELETE CASCADE,
    author_member_id UUID NOT NULL REFERENCES member_account(id) ON DELETE RESTRICT,
    body VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT care_feedback_pk PRIMARY KEY (id),
    CONSTRAINT care_feedback_author_uk UNIQUE (assignment_id, author_member_id)
);
