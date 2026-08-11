CREATE TABLE moderation_action (
    id UUID NOT NULL,
    actor_member_id UUID NOT NULL REFERENCES member_account(id),
    target_member_id UUID REFERENCES member_account(id),
    target_type VARCHAR(30) NOT NULL,
    target_id UUID,
    action VARCHAR(40) NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT moderation_action_pk PRIMARY KEY (id)
);

CREATE INDEX moderation_action_target_ix
    ON moderation_action (target_type, target_id, created_at DESC);
