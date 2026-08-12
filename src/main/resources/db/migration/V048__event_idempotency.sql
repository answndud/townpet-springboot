ALTER TABLE search_event ADD COLUMN client_event_id UUID;
CREATE UNIQUE INDEX search_event_client_event_uq ON search_event(client_event_id) WHERE client_event_id IS NOT NULL;

ALTER TABLE acquisition_event ADD COLUMN client_event_id UUID;
CREATE UNIQUE INDEX acquisition_event_client_event_uq ON acquisition_event(client_event_id) WHERE client_event_id IS NOT NULL;
