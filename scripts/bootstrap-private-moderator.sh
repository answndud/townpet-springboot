#!/usr/bin/env sh
set -eu

# The schema currently has MEMBER/MODERATOR roles only. This creates the
# private operational moderator used for portfolio administration; it never
# prints or accepts a plaintext password.
: "${TOWNPET_PSQL_URL:?set TOWNPET_PSQL_URL}"
: "${TOWNPET_DB_USERNAME:?set TOWNPET_DB_USERNAME}"
: "${TOWNPET_DB_PASSWORD:?set TOWNPET_DB_PASSWORD}"
: "${TOWNPET_PRIVATE_MODERATOR_EMAIL:?set TOWNPET_PRIVATE_MODERATOR_EMAIL}"
: "${TOWNPET_PRIVATE_MODERATOR_NICKNAME:?set TOWNPET_PRIVATE_MODERATOR_NICKNAME}"
: "${TOWNPET_PRIVATE_MODERATOR_PASSWORD_HASH:?set a bcrypt TOWNPET_PRIVATE_MODERATOR_PASSWORD_HASH (never plaintext)}"
: "${TOWNPET_PRIVATE_MODERATOR_CONFIRM:?set TOWNPET_PRIVATE_MODERATOR_CONFIRM=CREATE_PRIVATE_MODERATOR}"

if [ "$TOWNPET_PRIVATE_MODERATOR_CONFIRM" != "CREATE_PRIVATE_MODERATOR" ]; then
  echo "refusing private moderator bootstrap: confirmation missing" >&2
  exit 1
fi

export PGPASSWORD="$TOWNPET_DB_PASSWORD"
psql "$TOWNPET_PSQL_URL" -U "$TOWNPET_DB_USERNAME" -v ON_ERROR_STOP=1 \
  -v moderator_email="$TOWNPET_PRIVATE_MODERATOR_EMAIL" \
  -v moderator_nickname="$TOWNPET_PRIVATE_MODERATOR_NICKNAME" \
  -v moderator_password_hash="$TOWNPET_PRIVATE_MODERATOR_PASSWORD_HASH" <<'SQL'
BEGIN;
SELECT count(*) AS existing_moderator
FROM member_account
WHERE email = :'moderator_email'\gset
\if :existing_moderator
  \echo 'refusing private moderator bootstrap: email already exists'
  ROLLBACK;
  \quit 1
\endif

WITH new_member AS (
  INSERT INTO member_account (id, email, nickname)
  VALUES (gen_random_uuid(), :'moderator_email', :'moderator_nickname')
  RETURNING id, email
)
INSERT INTO identity_credential
  (id, member_id, email, password_hash, role, enabled, lifecycle_locked, email_verified_at)
SELECT gen_random_uuid(), id, email, :'moderator_password_hash', 'MODERATOR', TRUE, TRUE, CURRENT_TIMESTAMP
FROM new_member;
COMMIT;
SQL

echo "private moderator bootstrap completed"
