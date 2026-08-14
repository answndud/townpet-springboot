-- Publication posts are public community content. Remove the legacy
-- GLOBAL/LOCAL visibility model while retaining neighborhood metadata for
-- aggregates such as adoption listings.

DROP VIEW IF EXISTS townpet_community_feed_item;
DROP VIEW IF EXISTS townpet_public_feed_item;

DROP INDEX IF EXISTS publication_global_feed_cursor_ix;
DROP INDEX IF EXISTS publication_animal_interest_feed_ix;
DROP INDEX IF EXISTS publication_local_created_ix;

ALTER TABLE publication
    DROP CONSTRAINT IF EXISTS publication_scope_neighborhood_ck,
    DROP CONSTRAINT IF EXISTS publication_scope_ck,
    DROP CONSTRAINT IF EXISTS publication_neighborhood_fk;

ALTER TABLE publication
    DROP COLUMN IF EXISTS scope,
    DROP COLUMN IF EXISTS neighborhood_id;

CREATE INDEX publication_feed_cursor_ix
    ON publication (lifecycle, created_at DESC, id DESC);

CREATE INDEX publication_animal_interest_feed_ix
    ON publication (animal_interest_code, lifecycle, created_at DESC, id DESC);

CREATE VIEW townpet_public_feed_item AS
SELECT
    p.id AS source_id,
    'PUBLICATION'::VARCHAR(40) AS item_kind,
    p.type AS item_type,
    p.title,
    p.body AS summary,
    p.author_member_id,
    NULL::UUID AS neighborhood_id,
    p.animal_interest_code,
    p.lifecycle AS status,
    p.created_at,
    p.updated_at,
    '/posts/' || p.id::TEXT AS target_path
FROM publication p
WHERE p.lifecycle = 'ACTIVE'

UNION ALL

SELECT
    m.id,
    'MARKETPLACE',
    'MARKETPLACE',
    m.title,
    m.description,
    m.owner_member_id,
    NULL::UUID,
    NULL::VARCHAR(40),
    m.status,
    m.created_at,
    m.updated_at,
    '/marketplace/' || m.id::TEXT
FROM market_listing m
WHERE m.status IN ('AVAILABLE', 'RESERVED')

UNION ALL

SELECT
    a.id,
    'ADOPTION',
    'ADOPTION',
    a.title,
    a.description,
    a.publisher_member_id,
    a.neighborhood_id,
    CASE
        WHEN UPPER(a.species) LIKE 'DOG%' THEN 'DOG'
        WHEN UPPER(a.species) LIKE 'CAT%' THEN 'CAT'
        ELSE 'OTHER'
    END,
    a.status,
    a.created_at,
    a.updated_at,
    '/adoptions/' || a.id::TEXT
FROM adoption_listing a
WHERE a.status IN ('OPEN', 'RESERVED')

UNION ALL

SELECT
    l.id,
    'LOST_FOUND',
    'LOST_FOUND',
    l.title,
    l.description,
    l.reporter_member_id,
    NULL::UUID,
    NULL::VARCHAR(40),
    l.status,
    l.created_at,
    l.updated_at,
    '/lost-found/' || l.id::TEXT
FROM lost_found_alert l
WHERE l.status = 'ACTIVE'

UNION ALL

SELECT
    h.id,
    'HOSPITAL_REVIEW',
    'HOSPITAL_REVIEW',
    h.hospital_name,
    h.body,
    h.author_member_id,
    NULL::UUID,
    NULL::VARCHAR(40),
    'ACTIVE',
    h.created_at,
    h.updated_at,
    '/hospital-reviews'
FROM hospital_review h

UNION ALL

SELECT
    g.id,
    'GATHERING',
    'GATHERING',
    g.title,
    g.description,
    g.host_member_id,
    NULL::UUID,
    NULL::VARCHAR(40),
    g.status,
    g.created_at,
    g.created_at,
    '/gatherings/' || g.id::TEXT
FROM gathering g
WHERE g.status = 'ACTIVE'

UNION ALL

SELECT
    c.id,
    'CARE_REQUEST',
    'CARE_REQUEST',
    c.title,
    c.description,
    c.requester_member_id,
    NULL::UUID,
    NULL::VARCHAR(40),
    c.status,
    c.created_at,
    c.updated_at,
    '/care/' || c.id::TEXT
FROM care_request c
WHERE c.status = 'OPEN'

UNION ALL

SELECT
    v.id,
    'VOLUNTEER',
    'VOLUNTEER',
    v.title,
    v.description,
    v.publisher_member_id,
    NULL::UUID,
    NULL::VARCHAR(40),
    v.status,
    v.created_at,
    v.updated_at,
    '/volunteer'
FROM volunteer_opportunity v
WHERE v.status IN ('OPEN', 'FULL')

UNION ALL

SELECT
    r.id,
    'RESOURCE',
    r.kind,
    r.title,
    r.summary,
    NULL::UUID,
    NULL::UUID,
    NULL::VARCHAR(40),
    r.kind,
    r.updated_at,
    r.updated_at,
    '/guides/' || r.id::TEXT
FROM local_resource r;

CREATE VIEW townpet_community_feed_item AS
SELECT
    f.source_id,
    f.item_kind,
    f.item_type,
    f.title,
    f.summary,
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
