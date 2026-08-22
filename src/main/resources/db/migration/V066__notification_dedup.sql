ALTER TABLE notification
  ADD COLUMN event_id UUID;

UPDATE notification
SET event_id = gen_random_uuid()
WHERE event_id IS NULL;

ALTER TABLE notification
  ALTER COLUMN event_id SET NOT NULL,
  ADD CONSTRAINT notification_event_id_uk UNIQUE (event_id);
