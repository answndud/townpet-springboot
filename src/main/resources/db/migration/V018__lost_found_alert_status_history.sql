ALTER TABLE lost_found_alert
    ADD COLUMN reopen_reason VARCHAR(500);

CREATE TABLE lost_found_alert_status_history (
    id UUID NOT NULL,
    alert_id UUID NOT NULL,
    actor_member_id UUID NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT lost_found_alert_history_pk PRIMARY KEY (id),
    CONSTRAINT lost_found_alert_history_alert_fk FOREIGN KEY (alert_id)
        REFERENCES lost_found_alert(id),
    CONSTRAINT lost_found_alert_history_actor_fk FOREIGN KEY (actor_member_id)
        REFERENCES member_account(id),
    CONSTRAINT lost_found_alert_history_from_ck
        CHECK (from_status IS NULL OR from_status IN ('ACTIVE', 'RESOLVED', 'CLOSED')),
    CONSTRAINT lost_found_alert_history_to_ck
        CHECK (to_status IN ('ACTIVE', 'RESOLVED', 'CLOSED'))
);

CREATE INDEX lost_found_alert_history_alert_ix
    ON lost_found_alert_status_history (alert_id, changed_at DESC);
