ALTER TABLE market_listing DROP CONSTRAINT market_listing_kind_ck;
ALTER TABLE market_listing ADD CONSTRAINT market_listing_kind_ck
    CHECK (kind IN ('SELL', 'RENT', 'SHARE', 'GROUP_BUY'));

ALTER TABLE market_listing DROP CONSTRAINT market_listing_price_ck;
ALTER TABLE market_listing ADD CONSTRAINT market_listing_price_ck CHECK (
    (kind = 'SHARE' AND price_krw IS NULL)
    OR (kind IN ('SELL', 'RENT', 'GROUP_BUY') AND price_krw IS NOT NULL AND price_krw >= 0)
);
