ALTER TABLE identity_credential
    ADD COLUMN IF NOT EXISTS role VARCHAR(30) NOT NULL DEFAULT 'MEMBER';

ALTER TABLE identity_credential
    ADD CONSTRAINT identity_credential_role_ck CHECK (role IN ('MEMBER', 'MODERATOR'));

INSERT INTO member_account (id, email, nickname)
VALUES
    ('00000000-0000-4000-8000-000000000201', 'demo-member-1@townpet.local', 'demo-member-1'),
    ('00000000-0000-4000-8000-000000000202', 'demo-member-2@townpet.local', 'demo-member-2'),
    ('00000000-0000-4000-8000-000000000203', 'demo-member-3@townpet.local', 'demo-member-3'),
    ('00000000-0000-4000-8000-000000000204', 'demo-moderator@townpet.local', 'demo-moderator')
ON CONFLICT (id) DO NOTHING;

INSERT INTO identity_credential (id, member_id, email, password_hash, role)
VALUES
    ('00000000-0000-4000-8000-000000000211', '00000000-0000-4000-8000-000000000201', 'demo-member-1@townpet.local', '$2y$12$5F8A2cx5oPRWxpnvcF6RWeAdXKBjwSqdJ3u2OAwL2un7701NqmpKW', 'MEMBER'),
    ('00000000-0000-4000-8000-000000000212', '00000000-0000-4000-8000-000000000202', 'demo-member-2@townpet.local', '$2y$12$5F8A2cx5oPRWxpnvcF6RWeAdXKBjwSqdJ3u2OAwL2un7701NqmpKW', 'MEMBER'),
    ('00000000-0000-4000-8000-000000000213', '00000000-0000-4000-8000-000000000203', 'demo-member-3@townpet.local', '$2y$12$5F8A2cx5oPRWxpnvcF6RWeAdXKBjwSqdJ3u2OAwL2un7701NqmpKW', 'MEMBER'),
    ('00000000-0000-4000-8000-000000000214', '00000000-0000-4000-8000-000000000204', 'demo-moderator@townpet.local', '$2y$12$xqMvHDCYtDxPbZ3K1kuZlOEFV4CjZ4Jwl4A1aRqfGDhxKSdE4Ct0u', 'MODERATOR')
ON CONFLICT (id) DO NOTHING;
