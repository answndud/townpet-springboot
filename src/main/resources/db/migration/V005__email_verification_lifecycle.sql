ALTER TABLE identity_credential
    ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMPTZ;

UPDATE identity_credential
SET email_verified_at = CURRENT_TIMESTAMP
WHERE lifecycle_locked = TRUE;

CREATE TABLE email_verification_token (
    id UUID NOT NULL,
    member_id UUID NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT email_verification_token_pk PRIMARY KEY (id),
    CONSTRAINT email_verification_token_member_fk FOREIGN KEY (member_id)
        REFERENCES member_account (id) ON DELETE CASCADE,
    CONSTRAINT email_verification_token_hash_uk UNIQUE (token_hash),
    CONSTRAINT email_verification_token_expiry_ck CHECK (expires_at > created_at)
);

CREATE INDEX email_verification_token_member_ix
    ON email_verification_token (member_id, created_at DESC);
CREATE INDEX email_verification_token_expiry_ix
    ON email_verification_token (expires_at);

ALTER TABLE identity_auth_audit
    DROP CONSTRAINT identity_auth_audit_action_ck;
ALTER TABLE identity_auth_audit
    ADD CONSTRAINT identity_auth_audit_action_ck
        CHECK (action IN ('PASSWORD_RESET', 'EMAIL_VERIFIED'));
