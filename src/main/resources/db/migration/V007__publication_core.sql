CREATE TABLE publication (
    id UUID NOT NULL,
    author_member_id UUID NOT NULL,
    type VARCHAR(40) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    neighborhood_id UUID,
    title VARCHAR(120) NOT NULL,
    body VARCHAR(20000) NOT NULL,
    lifecycle VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT publication_pk PRIMARY KEY (id),
    CONSTRAINT publication_author_fk FOREIGN KEY (author_member_id)
        REFERENCES member_account (id) ON DELETE RESTRICT,
    CONSTRAINT publication_neighborhood_fk FOREIGN KEY (neighborhood_id)
        REFERENCES neighborhood (id) ON DELETE RESTRICT,
    CONSTRAINT publication_type_ck CHECK (type IN ('FREE_BOARD')),
    CONSTRAINT publication_scope_ck CHECK (scope IN ('LOCAL', 'GLOBAL')),
    CONSTRAINT publication_scope_neighborhood_ck CHECK (
        (scope = 'GLOBAL' AND neighborhood_id IS NULL)
        OR (scope = 'LOCAL' AND neighborhood_id IS NOT NULL)
    ),
    CONSTRAINT publication_title_ck CHECK (char_length(btrim(title)) BETWEEN 1 AND 120),
    CONSTRAINT publication_body_ck CHECK (char_length(btrim(body)) BETWEEN 1 AND 20000),
    CONSTRAINT publication_lifecycle_ck CHECK (lifecycle IN ('ACTIVE', 'DELETED')),
    CONSTRAINT publication_updated_at_ck CHECK (updated_at >= created_at)
);

CREATE INDEX publication_author_created_ix
    ON publication (author_member_id, created_at DESC, id DESC);
CREATE INDEX publication_local_created_ix
    ON publication (neighborhood_id, lifecycle, created_at DESC, id DESC)
    WHERE scope = 'LOCAL';
