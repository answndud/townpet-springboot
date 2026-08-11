CREATE TABLE adoption_listing (
    id UUID NOT NULL,
    publisher_member_id UUID NOT NULL REFERENCES member_account(id),
    neighborhood_id UUID REFERENCES neighborhood(id),
    title VARCHAR(120) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    species VARCHAR(30) NOT NULL,
    breed VARCHAR(80),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT adoption_listing_pk PRIMARY KEY (id),
    CONSTRAINT adoption_listing_status_ck CHECK (status IN ('OPEN', 'RESERVED', 'ADOPTED', 'CLOSED')),
    CONSTRAINT adoption_listing_title_ck CHECK (char_length(btrim(title)) BETWEEN 1 AND 120),
    CONSTRAINT adoption_listing_description_ck CHECK (char_length(btrim(description)) BETWEEN 1 AND 5000),
    CONSTRAINT adoption_listing_species_ck CHECK (char_length(btrim(species)) BETWEEN 1 AND 30)
);

CREATE INDEX adoption_listing_public_ix ON adoption_listing(status, created_at DESC, id DESC);
