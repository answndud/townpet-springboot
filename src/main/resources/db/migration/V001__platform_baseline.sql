DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_extension WHERE extname = 'postgis') THEN
        RAISE EXCEPTION 'PostGIS must be provisioned by the database bootstrap administrator';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_extension WHERE extname = 'citext') THEN
        RAISE EXCEPTION 'citext must be provisioned by the database bootstrap administrator';
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS spring_session (
    primary_id CHAR(36) NOT NULL,
    session_id CHAR(36) NOT NULL,
    creation_time BIGINT NOT NULL,
    last_access_time BIGINT NOT NULL,
    max_inactive_interval INT NOT NULL,
    expiry_time BIGINT NOT NULL,
    principal_name VARCHAR(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS spring_session_ix1 ON spring_session (session_id);
CREATE INDEX IF NOT EXISTS spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX IF NOT EXISTS spring_session_ix3 ON spring_session (principal_name);

CREATE TABLE IF NOT EXISTS spring_session_attributes (
    session_primary_id CHAR(36) NOT NULL,
    attribute_name VARCHAR(200) NOT NULL,
    attribute_bytes BYTEA NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id)
        REFERENCES spring_session (primary_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS event_publication (
    id UUID NOT NULL,
    completion_date TIMESTAMPTZ,
    event_type VARCHAR(512) NOT NULL,
    listener_id VARCHAR(512) NOT NULL,
    publication_date TIMESTAMPTZ NOT NULL,
    serialized_event TEXT NOT NULL,
    status VARCHAR(32),
    completion_attempts INT NOT NULL DEFAULT 0,
    last_resubmission_date TIMESTAMPTZ,
    CONSTRAINT event_publication_pk PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS event_publication_completion_idx
    ON event_publication (completion_date);

CREATE INDEX IF NOT EXISTS event_publication_serialized_event_idx
    ON event_publication (event_type, listener_id, publication_date);

COMMENT ON TABLE event_publication IS
    'Spring Modulith JDBC event publication registry; durable source for at-least-once delivery.';
