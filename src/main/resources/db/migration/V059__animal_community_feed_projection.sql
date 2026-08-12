CREATE VIEW townpet_community_feed_item AS
SELECT
    f.source_id,
    f.item_kind,
    f.item_type,
    f.title,
    f.summary,
    f.scope,
    f.author_member_id,
    f.neighborhood_id,
    c.animal_code,
    f.status,
    f.created_at,
    f.updated_at,
    f.target_path
FROM townpet_public_feed_item f
JOIN content_animal_community c
  ON c.content_kind = f.item_kind
 AND c.content_id = f.source_id;

CREATE INDEX content_animal_community_feed_cursor_ix
    ON content_animal_community (animal_code, created_at DESC, content_kind ASC, content_id DESC);
