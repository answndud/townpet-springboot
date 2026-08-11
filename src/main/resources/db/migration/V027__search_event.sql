CREATE TABLE search_event (
    id UUID NOT NULL,
    query_hash CHAR(64) NOT NULL,
    route VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT search_event_pk PRIMARY KEY (id)
);
CREATE INDEX search_event_created_ix ON search_event (created_at DESC);
