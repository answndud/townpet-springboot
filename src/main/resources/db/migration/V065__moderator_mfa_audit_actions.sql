ALTER TABLE identity_auth_audit
    DROP CONSTRAINT identity_auth_audit_action_ck;

ALTER TABLE identity_auth_audit
    ADD CONSTRAINT identity_auth_audit_action_ck
        CHECK (action IN (
            'PASSWORD_RESET',
            'EMAIL_VERIFIED',
            'MFA_ENROLLMENT_STARTED',
            'MFA_ENROLLED',
            'MFA_VERIFIED',
            'MFA_RECOVERY_USED'
        ));
