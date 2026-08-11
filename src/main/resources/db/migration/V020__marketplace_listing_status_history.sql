CREATE TABLE market_listing_status_history (
    id UUID NOT NULL,
    listing_id UUID NOT NULL,
    actor_member_id UUID NOT NULL,
    from_status VARCHAR(20) NOT NULL,
    to_status VARCHAR(20) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT market_listing_history_pk PRIMARY KEY (id),
    CONSTRAINT market_listing_history_listing_fk FOREIGN KEY (listing_id)
        REFERENCES market_listing(id),
    CONSTRAINT market_listing_history_actor_fk FOREIGN KEY (actor_member_id)
        REFERENCES member_account(id)
);

CREATE INDEX market_listing_history_listing_ix
    ON market_listing_status_history (listing_id, changed_at DESC);
