#!/usr/bin/env sh
set -eu

# Run once, before exposing the production web container. This intentionally
# refuses a database that already contains a non-demo member.
: "${TOWNPET_PSQL_URL:?set TOWNPET_PSQL_URL to a PostgreSQL connection URL}"
: "${TOWNPET_DB_USERNAME:?set TOWNPET_DB_USERNAME}"
: "${TOWNPET_DB_PASSWORD:?set TOWNPET_DB_PASSWORD}"
: "${TOWNPET_PRODUCTION_SANITIZE_CONFIRM:?set TOWNPET_PRODUCTION_SANITIZE_CONFIRM=REMOVE_DEMO}"
SANITIZE_APPLY="${TOWNPET_PRODUCTION_SANITIZE_APPLY:-NO}"
if [ "$SANITIZE_APPLY" = "YES" ]; then
  PSQL_APPLY=true
else
  PSQL_APPLY=false
fi

if [ "$TOWNPET_PRODUCTION_SANITIZE_CONFIRM" != "REMOVE_DEMO" ]; then
  echo "refusing production sanitize: set TOWNPET_PRODUCTION_SANITIZE_CONFIRM=REMOVE_DEMO" >&2
  exit 1
fi

export PGPASSWORD="$TOWNPET_DB_PASSWORD"
psql "$TOWNPET_PSQL_URL" -U "$TOWNPET_DB_USERNAME" -v ON_ERROR_STOP=1 -v sanitize_apply="$PSQL_APPLY" <<'SQL'
BEGIN;
CREATE TEMP TABLE demo_members(id uuid PRIMARY KEY) ON COMMIT DROP;
INSERT INTO demo_members(id) VALUES
  ('00000000-0000-4000-8000-000000000201'),
  ('00000000-0000-4000-8000-000000000202'),
  ('00000000-0000-4000-8000-000000000203'),
  ('00000000-0000-4000-8000-000000000204');

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM member_account m
    WHERE m.id NOT IN (SELECT id FROM demo_members)
  ) THEN
    RAISE EXCEPTION 'production sanitize refuses a database containing non-demo members';
  END IF;
END $$;

DELETE FROM care_feedback WHERE author_member_id IN (SELECT id FROM demo_members);
DELETE FROM care_assignment WHERE caregiver_member_id IN (SELECT id FROM demo_members);
DELETE FROM care_application WHERE applicant_member_id IN (SELECT id FROM demo_members);
DELETE FROM care_request WHERE requester_member_id IN (SELECT id FROM demo_members);
DELETE FROM volunteer_application WHERE applicant_member_id IN (SELECT id FROM demo_members);
DELETE FROM volunteer_opportunity WHERE publisher_member_id IN (SELECT id FROM demo_members);
DELETE FROM hospital_review WHERE author_member_id IN (SELECT id FROM demo_members);
DELETE FROM adoption_listing WHERE publisher_member_id IN (SELECT id FROM demo_members);
DELETE FROM upload_asset WHERE owner_member_id IN (SELECT id FROM demo_members);
DELETE FROM engagement_comment WHERE author_member_id IN (SELECT id FROM demo_members);
DELETE FROM engagement_reaction WHERE author_member_id IN (SELECT id FROM demo_members);
DELETE FROM engagement_bookmark WHERE member_id IN (SELECT id FROM demo_members);
DELETE FROM notification WHERE recipient_member_id IN (SELECT id FROM demo_members);
DELETE FROM trust_report WHERE reporter_member_id IN (SELECT id FROM demo_members);
DELETE FROM correction_request WHERE member_id IN (SELECT id FROM demo_members);
DELETE FROM moderation_action WHERE actor_member_id IN (SELECT id FROM demo_members) OR target_member_id IN (SELECT id FROM demo_members);
DELETE FROM relationship_follow WHERE follower_member_id IN (SELECT id FROM demo_members) OR followed_member_id IN (SELECT id FROM demo_members);
DELETE FROM relationship_block WHERE blocker_member_id IN (SELECT id FROM demo_members) OR blocked_member_id IN (SELECT id FROM demo_members);
DELETE FROM gathering_participant WHERE member_id IN (SELECT id FROM demo_members);
DELETE FROM gathering WHERE host_member_id IN (SELECT id FROM demo_members);
DELETE FROM market_listing_status_history WHERE actor_member_id IN (SELECT id FROM demo_members);
DELETE FROM market_listing WHERE owner_member_id IN (SELECT id FROM demo_members);
DELETE FROM lost_found_location_access_audit WHERE viewer_member_id IN (SELECT id FROM demo_members);
DELETE FROM lost_found_alert_status_history WHERE actor_member_id IN (SELECT id FROM demo_members);
DELETE FROM lost_found_sighting_report WHERE reporter_member_id IN (SELECT id FROM demo_members);
DELETE FROM lost_found_alert WHERE reporter_member_id IN (SELECT id FROM demo_members);
DELETE FROM publication_metric WHERE publication_id IN (SELECT id FROM publication WHERE author_member_id IN (SELECT id FROM demo_members));
DELETE FROM publication WHERE author_member_id IN (SELECT id FROM demo_members);
DELETE FROM search_event;
DELETE FROM acquisition_event;
DELETE FROM web_vital_metric WHERE route LIKE '/demo/%';
DELETE FROM spring_session WHERE principal_name IN (SELECT id::text FROM demo_members);
UPDATE policy_document SET updated_by = NULL WHERE updated_by IN (SELECT id FROM demo_members);
UPDATE moderator_case SET resolved_by = NULL WHERE resolved_by IN (SELECT id FROM demo_members);
DELETE FROM identity_credential WHERE member_id IN (SELECT id FROM demo_members);
DELETE FROM member_account WHERE id IN (SELECT id FROM demo_members);

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM member_account WHERE email LIKE '%@townpet.local') THEN
    RAISE EXCEPTION 'production sanitize verification failed: demo credential remains';
  END IF;
END $$;

-- Default is a dry-run. Set TOWNPET_PRODUCTION_SANITIZE_APPLY=YES to commit.
\if :sanitize_apply
  COMMIT;
\else
  ROLLBACK;
\endif
SQL

echo "production demo sanitize verification completed"
