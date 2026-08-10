CREATE TABLE engagement_bookmark (
    id UUID NOT NULL,
    publication_id UUID NOT NULL,
    member_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT engagement_bookmark_pk PRIMARY KEY (id),
    CONSTRAINT engagement_bookmark_publication_fk FOREIGN KEY (publication_id)
        REFERENCES publication (id) ON DELETE RESTRICT,
    CONSTRAINT engagement_bookmark_member_fk FOREIGN KEY (member_id)
        REFERENCES member_account (id) ON DELETE RESTRICT,
    CONSTRAINT engagement_bookmark_unique UNIQUE (publication_id, member_id)
);

CREATE INDEX engagement_bookmark_member_created_ix
    ON engagement_bookmark (member_id, created_at DESC, publication_id DESC);
