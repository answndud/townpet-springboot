CREATE TABLE engagement_reaction (
    id UUID NOT NULL,
    publication_id UUID NOT NULL,
    author_member_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT engagement_reaction_pk PRIMARY KEY (id),
    CONSTRAINT engagement_reaction_publication_fk FOREIGN KEY (publication_id)
        REFERENCES publication (id) ON DELETE RESTRICT,
    CONSTRAINT engagement_reaction_author_fk FOREIGN KEY (author_member_id)
        REFERENCES member_account (id) ON DELETE RESTRICT,
    CONSTRAINT engagement_reaction_type_ck CHECK (type IN ('LIKE')),
    CONSTRAINT engagement_reaction_unique UNIQUE (publication_id, author_member_id, type)
);

CREATE INDEX engagement_reaction_publication_type_ix
    ON engagement_reaction (publication_id, type);
