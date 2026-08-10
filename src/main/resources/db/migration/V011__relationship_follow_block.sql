CREATE TABLE relationship_follow (
    id UUID NOT NULL,
    follower_member_id UUID NOT NULL,
    followed_member_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT relationship_follow_pk PRIMARY KEY (id),
    CONSTRAINT relationship_follow_follower_fk FOREIGN KEY (follower_member_id)
        REFERENCES member_account (id) ON DELETE RESTRICT,
    CONSTRAINT relationship_follow_followed_fk FOREIGN KEY (followed_member_id)
        REFERENCES member_account (id) ON DELETE RESTRICT,
    CONSTRAINT relationship_follow_unique UNIQUE (follower_member_id, followed_member_id),
    CONSTRAINT relationship_follow_self_ck CHECK (follower_member_id <> followed_member_id)
);

CREATE TABLE relationship_block (
    id UUID NOT NULL,
    blocker_member_id UUID NOT NULL,
    blocked_member_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT relationship_block_pk PRIMARY KEY (id),
    CONSTRAINT relationship_block_blocker_fk FOREIGN KEY (blocker_member_id)
        REFERENCES member_account (id) ON DELETE RESTRICT,
    CONSTRAINT relationship_block_blocked_fk FOREIGN KEY (blocked_member_id)
        REFERENCES member_account (id) ON DELETE RESTRICT,
    CONSTRAINT relationship_block_unique UNIQUE (blocker_member_id, blocked_member_id),
    CONSTRAINT relationship_block_self_ck CHECK (blocker_member_id <> blocked_member_id)
);

CREATE INDEX relationship_follow_followed_ix ON relationship_follow (followed_member_id);
CREATE INDEX relationship_block_blocked_ix ON relationship_block (blocked_member_id);
