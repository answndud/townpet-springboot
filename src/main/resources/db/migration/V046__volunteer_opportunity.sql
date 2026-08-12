CREATE TABLE volunteer_opportunity (
 id UUID PRIMARY KEY, publisher_member_id UUID NOT NULL REFERENCES member_account(id), title VARCHAR(120) NOT NULL,
 description VARCHAR(5000) NOT NULL, organization VARCHAR(160) NOT NULL, location VARCHAR(200) NOT NULL,
 starts_at TIMESTAMPTZ NOT NULL, capacity INT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 version BIGINT NOT NULL DEFAULT 0, CONSTRAINT volunteer_capacity_ck CHECK (capacity BETWEEN 1 AND 100),
 CONSTRAINT volunteer_status_ck CHECK (status IN ('OPEN','FULL','CLOSED'))
);
CREATE TABLE volunteer_application (
 id UUID PRIMARY KEY, opportunity_id UUID NOT NULL REFERENCES volunteer_opportunity(id) ON DELETE CASCADE,
 applicant_member_id UUID NOT NULL REFERENCES member_account(id), message VARCHAR(1000) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT volunteer_application_uq UNIQUE(opportunity_id, applicant_member_id)
);
