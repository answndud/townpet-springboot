ALTER TABLE identity_credential
    ADD COLUMN IF NOT EXISTS lifecycle_locked BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE identity_credential
SET lifecycle_locked = TRUE
WHERE email LIKE '%@townpet.local';

CREATE TABLE password_reset_token (
    id UUID NOT NULL,
    member_id UUID NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT password_reset_token_pk PRIMARY KEY (id),
    CONSTRAINT password_reset_token_member_fk FOREIGN KEY (member_id)
        REFERENCES member_account (id) ON DELETE CASCADE,
    CONSTRAINT password_reset_token_hash_uk UNIQUE (token_hash),
    CONSTRAINT password_reset_token_expiry_ck CHECK (expires_at > created_at)
);

CREATE INDEX password_reset_token_member_ix
    ON password_reset_token (member_id, created_at DESC);
CREATE INDEX password_reset_token_expiry_ix
    ON password_reset_token (expires_at);

CREATE TABLE identity_auth_audit (
    id UUID NOT NULL,
    member_id UUID,
    action VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT identity_auth_audit_pk PRIMARY KEY (id),
    CONSTRAINT identity_auth_audit_member_fk FOREIGN KEY (member_id)
        REFERENCES member_account (id) ON DELETE SET NULL,
    CONSTRAINT identity_auth_audit_action_ck CHECK (action IN ('PASSWORD_RESET'))
);

CREATE INDEX identity_auth_audit_member_ix
    ON identity_auth_audit (member_id, created_at DESC);
