ALTER TABLE lost_found_sighting_report
    ADD COLUMN exact_location geography(Point, 4326),
    ADD COLUMN visibility VARCHAR(30) NOT NULL DEFAULT 'PUBLIC_APPROXIMATE';

ALTER TABLE lost_found_sighting_report
    ADD CONSTRAINT lost_found_sighting_visibility_ck
    CHECK (visibility IN ('PUBLIC_APPROXIMATE', 'OWNER_ONLY_EXACT'));

CREATE TABLE lost_found_location_access_audit (
    id UUID NOT NULL,
    sighting_id UUID NOT NULL,
    viewer_member_id UUID NOT NULL,
    accessed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT lost_found_location_audit_pk PRIMARY KEY (id),
    CONSTRAINT lost_found_location_audit_sighting_fk FOREIGN KEY (sighting_id)
        REFERENCES lost_found_sighting_report(id),
    CONSTRAINT lost_found_location_audit_viewer_fk FOREIGN KEY (viewer_member_id)
        REFERENCES member_account(id)
);
