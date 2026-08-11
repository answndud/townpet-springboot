CREATE TABLE guest_author (
    id UUID NOT NULL,
    public_id UUID NOT NULL,
    management_password_hash VARCHAR(100) NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT guest_author_pk PRIMARY KEY (id),
    CONSTRAINT guest_author_public_id_uk UNIQUE (public_id),
    CONSTRAINT guest_author_failed_attempts_ck CHECK (failed_attempts >= 0)
);

CREATE TABLE guest_step_up_challenge (
    id UUID NOT NULL,
    guest_author_id UUID NOT NULL REFERENCES guest_author(id) ON DELETE CASCADE,
    scope VARCHAR(80) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT guest_step_up_challenge_pk PRIMARY KEY (id),
    CONSTRAINT guest_step_up_challenge_token_uk UNIQUE (token_hash),
    CONSTRAINT guest_step_up_challenge_scope_ck CHECK (char_length(btrim(scope)) BETWEEN 1 AND 80)
);

CREATE INDEX guest_step_up_active_ix ON guest_step_up_challenge (guest_author_id, expires_at)
    WHERE used_at IS NULL;
