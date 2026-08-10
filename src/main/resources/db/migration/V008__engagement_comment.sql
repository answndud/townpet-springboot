CREATE TABLE engagement_comment (
    id UUID NOT NULL,
    publication_id UUID NOT NULL,
    author_member_id UUID NOT NULL,
    body VARCHAR(5000) NOT NULL,
    lifecycle VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT engagement_comment_pk PRIMARY KEY (id),
    CONSTRAINT engagement_comment_publication_fk FOREIGN KEY (publication_id)
        REFERENCES publication (id) ON DELETE RESTRICT,
    CONSTRAINT engagement_comment_author_fk FOREIGN KEY (author_member_id)
        REFERENCES member_account (id) ON DELETE RESTRICT,
    CONSTRAINT engagement_comment_body_ck CHECK (char_length(btrim(body)) BETWEEN 1 AND 5000),
    CONSTRAINT engagement_comment_lifecycle_ck CHECK (lifecycle IN ('ACTIVE', 'DELETED')),
    CONSTRAINT engagement_comment_updated_at_ck CHECK (updated_at >= created_at)
);

CREATE INDEX engagement_comment_publication_created_ix
    ON engagement_comment (publication_id, lifecycle, created_at ASC, id ASC);
