CREATE TABLE IF NOT EXISTS neighborhood (
    id UUID NOT NULL,
    slug VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT neighborhood_pk PRIMARY KEY (id),
    CONSTRAINT neighborhood_slug_uk UNIQUE (slug)
);

CREATE TABLE IF NOT EXISTS member_account (
    id UUID NOT NULL,
    email CITEXT NOT NULL,
    nickname VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT member_account_pk PRIMARY KEY (id),
    CONSTRAINT member_account_email_uk UNIQUE (email),
    CONSTRAINT member_account_nickname_ck CHECK (char_length(nickname) BETWEEN 2 AND 40)
);

CREATE TABLE IF NOT EXISTS identity_credential (
    id UUID NOT NULL,
    member_id UUID NOT NULL,
    email CITEXT NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT identity_credential_pk PRIMARY KEY (id),
    CONSTRAINT identity_credential_member_fk FOREIGN KEY (member_id)
        REFERENCES member_account (id) ON DELETE CASCADE,
    CONSTRAINT identity_credential_email_uk UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS member_profile (
    member_id UUID NOT NULL,
    bio VARCHAR(500),
    neighborhood_id UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT member_profile_pk PRIMARY KEY (member_id),
    CONSTRAINT member_profile_member_fk FOREIGN KEY (member_id)
        REFERENCES member_account (id) ON DELETE CASCADE,
    CONSTRAINT member_profile_neighborhood_fk FOREIGN KEY (neighborhood_id)
        REFERENCES neighborhood (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS member_pet (
    id UUID NOT NULL,
    member_id UUID NOT NULL,
    name VARCHAR(40) NOT NULL,
    species VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT member_pet_pk PRIMARY KEY (id),
    CONSTRAINT member_pet_member_fk FOREIGN KEY (member_id)
        REFERENCES member_account (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS identity_credential_member_ix ON identity_credential (member_id);
CREATE INDEX IF NOT EXISTS member_profile_neighborhood_ix ON member_profile (neighborhood_id);

INSERT INTO neighborhood (id, slug, name)
VALUES
    ('00000000-0000-4000-8000-000000000101', 'seoul-mapogu', '서울 마포구'),
    ('00000000-0000-4000-8000-000000000102', 'seoul-seongdonggu', '서울 성동구')
ON CONFLICT (slug) DO NOTHING;
