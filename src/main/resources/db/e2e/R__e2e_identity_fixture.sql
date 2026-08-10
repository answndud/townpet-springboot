INSERT INTO member_account (id, email, nickname)
VALUES
    ('00000000-0000-4000-8000-000000000301', 'e2e-member-desktop@townpet.local', 'e2e-desktop'),
    ('00000000-0000-4000-8000-000000000302', 'e2e-member-mobile@townpet.local', 'e2e-mobile'),
    ('00000000-0000-4000-8000-000000000303', 'e2e-verify-desktop@townpet.local', 'verify-desktop'),
    ('00000000-0000-4000-8000-000000000304', 'e2e-verify-mobile@townpet.local', 'verify-mobile')
ON CONFLICT (id) DO NOTHING;

INSERT INTO identity_credential (
    id, member_id, email, password_hash, role, lifecycle_locked, email_verified_at
)
VALUES
    (
        '00000000-0000-4000-8000-000000000311',
        '00000000-0000-4000-8000-000000000301',
        'e2e-member-desktop@townpet.local',
        '$2y$12$5F8A2cx5oPRWxpnvcF6RWeAdXKBjwSqdJ3u2OAwL2un7701NqmpKW',
        'MEMBER', FALSE, CURRENT_TIMESTAMP
    ),
    (
        '00000000-0000-4000-8000-000000000312',
        '00000000-0000-4000-8000-000000000302',
        'e2e-member-mobile@townpet.local',
        '$2y$12$5F8A2cx5oPRWxpnvcF6RWeAdXKBjwSqdJ3u2OAwL2un7701NqmpKW',
        'MEMBER', FALSE, CURRENT_TIMESTAMP
    ),
    (
        '00000000-0000-4000-8000-000000000313',
        '00000000-0000-4000-8000-000000000303',
        'e2e-verify-desktop@townpet.local',
        '$2y$12$5F8A2cx5oPRWxpnvcF6RWeAdXKBjwSqdJ3u2OAwL2un7701NqmpKW',
        'MEMBER', FALSE, NULL
    ),
    (
        '00000000-0000-4000-8000-000000000314',
        '00000000-0000-4000-8000-000000000304',
        'e2e-verify-mobile@townpet.local',
        '$2y$12$5F8A2cx5oPRWxpnvcF6RWeAdXKBjwSqdJ3u2OAwL2un7701NqmpKW',
        'MEMBER', FALSE, NULL
    )
ON CONFLICT (id) DO NOTHING;
