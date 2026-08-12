-- Deterministic synthetic fixture. Run only against the dedicated performance DB.
\set ON_ERROR_STOP on
\if :{?scale}
\else
\set scale 2000
\endif

BEGIN;

DELETE FROM trust_report WHERE detail = 'performance-fixture';
DELETE FROM volunteer_opportunity WHERE title LIKE 'perf-opportunity-%';
DELETE FROM publication WHERE title LIKE 'perf-publication-%';

INSERT INTO publication (
    id, author_member_id, type, scope, neighborhood_id, title, body,
    lifecycle, created_at, updated_at, version
)
SELECT
    md5('perf-publication-' || i)::uuid,
    CASE WHEN i % 3 = 0
         THEN '00000000-0000-4000-8000-000000000202'::uuid
         ELSE '00000000-0000-4000-8000-000000000201'::uuid END,
    'FREE_BOARD', 'GLOBAL', NULL,
    'perf-publication-' || i,
    'Deterministic performance fixture publication ' || i,
    CASE WHEN i % 29 = 0 THEN 'DELETED' ELSE 'ACTIVE' END,
    CURRENT_TIMESTAMP - (i || ' seconds')::interval,
    CURRENT_TIMESTAMP - (i || ' seconds')::interval,
    0
FROM generate_series(1, :scale) AS series(i);

INSERT INTO volunteer_opportunity (
    id, publisher_member_id, title, description, organization, location,
    starts_at, capacity, status, created_at, updated_at, version
)
SELECT
    md5('perf-opportunity-' || i)::uuid,
    '00000000-0000-4000-8000-000000000204'::uuid,
    'perf-opportunity-' || i,
    'Deterministic performance fixture opportunity ' || i,
    'TownPet performance fixture',
    'Seoul',
    CURRENT_TIMESTAMP + (i || ' minutes')::interval,
    10,
    CASE WHEN i % 7 = 0 THEN 'FULL' ELSE 'OPEN' END,
    CURRENT_TIMESTAMP - (i || ' seconds')::interval,
    CURRENT_TIMESTAMP - (i || ' seconds')::interval,
    0
FROM generate_series(1, GREATEST(100, :scale / 10)) AS series(i);

INSERT INTO trust_report (
    id, reporter_member_id, target_type, target_id, reason, detail, status, created_at
)
SELECT
    md5('perf-report-' || i)::uuid,
    '00000000-0000-4000-8000-000000000203'::uuid,
    'PUBLICATION',
    md5('perf-publication-' || i)::uuid,
    'SPAM',
    'performance-fixture',
    CASE WHEN i % 5 = 0 THEN 'REVIEWED' ELSE 'OPEN' END,
    CURRENT_TIMESTAMP - (i || ' seconds')::interval
FROM generate_series(1, GREATEST(100, :scale / 10)) AS series(i);

COMMIT;

ANALYZE publication;
ANALYZE volunteer_opportunity;
ANALYZE trust_report;
