CREATE TABLE market_listing (
    id UUID NOT NULL,
    owner_member_id UUID NOT NULL,
    kind VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    title VARCHAR(120) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    price_krw BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT market_listing_pk PRIMARY KEY (id),
    CONSTRAINT market_listing_owner_fk FOREIGN KEY (owner_member_id)
        REFERENCES member_account(id),
    CONSTRAINT market_listing_kind_ck CHECK (kind IN ('SELL', 'RENT', 'SHARE')),
    CONSTRAINT market_listing_status_ck CHECK (status IN ('AVAILABLE', 'RESERVED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT market_listing_title_ck CHECK (char_length(btrim(title)) BETWEEN 1 AND 120),
    CONSTRAINT market_listing_description_ck CHECK (char_length(btrim(description)) BETWEEN 1 AND 5000),
    CONSTRAINT market_listing_price_ck CHECK (
        (kind = 'SHARE' AND price_krw IS NULL)
        OR (kind IN ('SELL', 'RENT') AND price_krw IS NOT NULL AND price_krw >= 0)
    )
);

CREATE INDEX market_listing_public_ix
    ON market_listing (status, created_at DESC, id DESC);
