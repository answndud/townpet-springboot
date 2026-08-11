CREATE TABLE lost_found_sighting_report (
    id UUID NOT NULL,
    alert_id UUID NOT NULL,
    reporter_member_id UUID NOT NULL,
    seen_at TIMESTAMPTZ NOT NULL,
    description VARCHAR(2000) NOT NULL,
    approx_location geography(Point, 4326) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT lost_found_sighting_pk PRIMARY KEY (id),
    CONSTRAINT lost_found_sighting_alert_fk FOREIGN KEY (alert_id)
        REFERENCES lost_found_alert(id),
    CONSTRAINT lost_found_sighting_reporter_fk FOREIGN KEY (reporter_member_id)
        REFERENCES member_account(id),
    CONSTRAINT lost_found_sighting_description_ck
        CHECK (char_length(btrim(description)) BETWEEN 1 AND 2000)
);

CREATE INDEX lost_found_sighting_alert_ix
    ON lost_found_sighting_report (alert_id, seen_at DESC, id DESC);
