CREATE VIEW townpet_public_feed_item AS
SELECT
    p.id AS source_id,
    'PUBLICATION'::VARCHAR(40) AS item_kind,
    p.type AS item_type,
    p.title,
    p.body AS summary,
    p.scope,
    p.author_member_id,
    p.neighborhood_id,
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
    'GLOBAL',
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
    'GLOBAL',
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
    'GLOBAL',
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
    'GLOBAL',
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
    'GLOBAL',
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
    'GLOBAL',
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
    'GLOBAL',
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
    'GLOBAL',
    NULL::UUID,
    NULL::UUID,
    NULL::VARCHAR(40),
    r.kind,
    r.updated_at,
    r.updated_at,
    '/guides/' || r.id::TEXT
FROM local_resource r;

CREATE INDEX IF NOT EXISTS market_listing_public_feed_ix
    ON market_listing (created_at DESC, id DESC)
    WHERE status IN ('AVAILABLE', 'RESERVED');

CREATE INDEX IF NOT EXISTS adoption_listing_public_feed_ix
    ON adoption_listing (created_at DESC, id DESC)
    WHERE status IN ('OPEN', 'RESERVED');

CREATE INDEX IF NOT EXISTS lost_found_alert_public_feed_ix
    ON lost_found_alert (created_at DESC, id DESC)
    WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS hospital_review_public_feed_ix
    ON hospital_review (created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS gathering_public_feed_ix
    ON gathering (created_at DESC, id DESC)
    WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS care_request_public_feed_ix
    ON care_request (created_at DESC, id DESC)
    WHERE status = 'OPEN';

CREATE INDEX IF NOT EXISTS volunteer_public_feed_ix
    ON volunteer_opportunity (created_at DESC, id DESC)
    WHERE status IN ('OPEN', 'FULL');
