CREATE TABLE identity_mfa_factor (
    member_id UUID NOT NULL,
    secret_ciphertext VARCHAR(512) NOT NULL,
    enrollment_expires_at TIMESTAMPTZ NOT NULL,
    enabled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT identity_mfa_factor_pk PRIMARY KEY (member_id),
    CONSTRAINT identity_mfa_factor_member_fk FOREIGN KEY (member_id)
        REFERENCES member_account (id) ON DELETE CASCADE
);

CREATE TABLE identity_mfa_recovery_code (
    id UUID NOT NULL,
    member_id UUID NOT NULL,
    code_hash VARCHAR(100) NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT identity_mfa_recovery_code_pk PRIMARY KEY (id),
    CONSTRAINT identity_mfa_recovery_code_member_fk FOREIGN KEY (member_id)
        REFERENCES member_account (id) ON DELETE CASCADE
);

CREATE INDEX identity_mfa_recovery_member_ix
    ON identity_mfa_recovery_code (member_id, used_at);
