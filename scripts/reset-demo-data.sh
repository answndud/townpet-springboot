#!/usr/bin/env sh
set -eu

: "${TOWNPET_DEMO_RESET_CONFIRM:?set TOWNPET_DEMO_RESET_CONFIRM=YES to reset only demo-owned content}"
if [ "$TOWNPET_DEMO_RESET_CONFIRM" != "YES" ]; then
  echo "refusing demo reset: set TOWNPET_DEMO_RESET_CONFIRM=YES" >&2
  exit 1
fi

: "${TOWNPET_PSQL_URL:?set TOWNPET_PSQL_URL to a PostgreSQL connection URL}"
: "${TOWNPET_DB_USERNAME:?set TOWNPET_DB_USERNAME}"
: "${TOWNPET_DB_PASSWORD:?set TOWNPET_DB_PASSWORD}"

export PGPASSWORD="$TOWNPET_DB_PASSWORD"
psql "$TOWNPET_PSQL_URL" -U "$TOWNPET_DB_USERNAME" -v ON_ERROR_STOP=1 <<'SQL'
BEGIN;
CREATE TEMP TABLE demo_members(id uuid PRIMARY KEY) ON COMMIT DROP;
INSERT INTO demo_members(id) VALUES
  ('00000000-0000-4000-8000-000000000201'),
  ('00000000-0000-4000-8000-000000000202'),
  ('00000000-0000-4000-8000-000000000203'),
  ('00000000-0000-4000-8000-000000000204');

DELETE FROM care_feedback WHERE author_member_id IN (SELECT id FROM demo_members);
DELETE FROM care_assignment WHERE caregiver_member_id IN (SELECT id FROM demo_members);
DELETE FROM care_application WHERE applicant_member_id IN (SELECT id FROM demo_members);
DELETE FROM care_request WHERE requester_member_id IN (SELECT id FROM demo_members);
DELETE FROM volunteer_application WHERE applicant_member_id IN (SELECT id FROM demo_members);
DELETE FROM volunteer_opportunity WHERE publisher_member_id IN (SELECT id FROM demo_members);
DELETE FROM hospital_review WHERE author_member_id IN (SELECT id FROM demo_members);
DELETE FROM upload_asset WHERE owner_member_id IN (SELECT id FROM demo_members);
DELETE FROM engagement_comment WHERE author_member_id IN (SELECT id FROM demo_members);
DELETE FROM engagement_reaction WHERE author_member_id IN (SELECT id FROM demo_members);
DELETE FROM engagement_bookmark WHERE member_id IN (SELECT id FROM demo_members);
DELETE FROM notification WHERE recipient_member_id IN (SELECT id FROM demo_members);
DELETE FROM trust_report WHERE reporter_member_id IN (SELECT id FROM demo_members);
DELETE FROM correction_request WHERE member_id IN (SELECT id FROM demo_members);
DELETE FROM moderation_action WHERE actor_member_id IN (SELECT id FROM demo_members);
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
-- Anonymous search/acquisition telemetry is not demo-owned. Keep it during a
-- local demo reset so the reset cannot erase unrelated analytics evidence.
DELETE FROM web_vital_metric WHERE route LIKE '/demo/%';
DELETE FROM spring_session WHERE principal_name IN (SELECT id::text FROM demo_members);
COMMIT;
SQL
echo "demo-owned content reset completed; identity accounts and schema were preserved"
