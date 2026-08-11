CREATE TABLE lost_found_alert (
    id UUID NOT NULL,
    reporter_member_id UUID NOT NULL,
    kind VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    title VARCHAR(120) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    approx_location geography(Point, 4326) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT lost_found_alert_pk PRIMARY KEY (id),
    CONSTRAINT lost_found_alert_reporter_fk FOREIGN KEY (reporter_member_id)
        REFERENCES member_account(id),
    CONSTRAINT lost_found_alert_kind_ck CHECK (kind IN ('LOST', 'FOUND')),
    CONSTRAINT lost_found_alert_status_ck CHECK (status IN ('ACTIVE', 'RESOLVED', 'CLOSED')),
    CONSTRAINT lost_found_alert_title_ck CHECK (char_length(btrim(title)) BETWEEN 1 AND 120),
    CONSTRAINT lost_found_alert_description_ck CHECK (char_length(btrim(description)) BETWEEN 1 AND 5000)
);

CREATE INDEX lost_found_alert_active_ix
    ON lost_found_alert (status, last_seen_at DESC);
CREATE INDEX lost_found_alert_location_ix
    ON lost_found_alert USING GIST (approx_location);
