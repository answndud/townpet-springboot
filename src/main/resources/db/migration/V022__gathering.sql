CREATE TABLE gathering (
    id UUID NOT NULL,
    host_member_id UUID NOT NULL REFERENCES member_account(id),
    title VARCHAR(160) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    location VARCHAR(200) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    capacity INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT gathering_pk PRIMARY KEY (id),
    CONSTRAINT gathering_capacity_ck CHECK (capacity BETWEEN 2 AND 100),
    CONSTRAINT gathering_status_ck CHECK (status IN ('ACTIVE', 'CANCELLED'))
);
CREATE TABLE gathering_participant (
    id UUID NOT NULL,
    gathering_id UUID NOT NULL REFERENCES gathering(id),
    member_id UUID NOT NULL REFERENCES member_account(id),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT gathering_participant_pk PRIMARY KEY (id),
    CONSTRAINT gathering_participant_uq UNIQUE (gathering_id, member_id)
);
CREATE INDEX gathering_public_ix ON gathering (status, starts_at, id);
CREATE INDEX gathering_participant_count_ix ON gathering_participant (gathering_id);

INSERT INTO gathering (id, host_member_id, title, description, location, starts_at, capacity)
VALUES ('0198f342-13d7-7000-8000-000000000401', '00000000-0000-4000-8000-000000000201', '망원 한강 저녁 산책', '천천히 걷고 반려생활 팁을 나누는 소규모 산책입니다.', '망원나들목 앞', '2026-08-20T10:00:00Z', 8),
       ('0198f342-13d7-7000-8000-000000000402', '00000000-0000-4000-8000-000000000201', '초보 보호자 Q&A 모임', '처음 반려동물과 사는 분들이 질문을 나누는 자리입니다.', '성수 커뮤니티룸', '2026-08-24T05:00:00Z', 12);
