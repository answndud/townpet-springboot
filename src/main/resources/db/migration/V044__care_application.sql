CREATE TABLE care_application (
    id UUID NOT NULL,
    request_id UUID NOT NULL REFERENCES care_request(id) ON DELETE CASCADE,
    applicant_member_id UUID NOT NULL REFERENCES member_account(id) ON DELETE RESTRICT,
    message VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT care_application_pk PRIMARY KEY (id),
    CONSTRAINT care_application_unique_applicant UNIQUE (request_id, applicant_member_id),
    CONSTRAINT care_application_status_ck CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'WITHDRAWN'))
);
CREATE INDEX care_application_request_status_ix ON care_application(request_id, status, created_at, id);
