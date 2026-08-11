ALTER TABLE lost_found_alert
    ADD COLUMN resolution_outcome VARCHAR(500),
    ADD COLUMN close_reason VARCHAR(500);

ALTER TABLE lost_found_alert
    ADD CONSTRAINT lost_found_alert_resolution_ck
    CHECK (status <> 'RESOLVED' OR char_length(btrim(resolution_outcome)) BETWEEN 1 AND 500),
    ADD CONSTRAINT lost_found_alert_close_ck
    CHECK (status <> 'CLOSED' OR char_length(btrim(close_reason)) BETWEEN 1 AND 500);
