CREATE TABLE notification (
    id UUID NOT NULL,
    recipient_member_id UUID NOT NULL REFERENCES member_account(id),
    type VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT notification_pk PRIMARY KEY (id)
);
CREATE INDEX notification_recipient_ix ON notification (recipient_member_id, created_at DESC, id DESC);

INSERT INTO notification (id, recipient_member_id, type, title, body)
VALUES ('0198f342-13d7-7000-8000-000000000501', '00000000-0000-4000-8000-000000000201', 'SYSTEM', 'TownPet에 오신 것을 환영합니다', '분실·발견 소식과 동네 모임을 확인해 보세요.');
