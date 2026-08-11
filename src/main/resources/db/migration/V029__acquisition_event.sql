CREATE TABLE acquisition_event (
    id UUID NOT NULL,
    event_name VARCHAR(80) NOT NULL,
    route VARCHAR(200) NOT NULL,
    anonymous_key_hash VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT acquisition_event_pk PRIMARY KEY (id)
);
CREATE INDEX acquisition_event_created_ix ON acquisition_event (event_name, created_at DESC);
