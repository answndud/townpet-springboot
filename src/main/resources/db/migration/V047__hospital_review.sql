CREATE TABLE hospital_review (
 id UUID PRIMARY KEY, author_member_id UUID NOT NULL REFERENCES member_account(id), hospital_name VARCHAR(160) NOT NULL,
 address VARCHAR(240) NOT NULL, rating INT NOT NULL, body VARCHAR(5000) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 version BIGINT NOT NULL DEFAULT 0, CONSTRAINT hospital_review_rating_ck CHECK (rating BETWEEN 1 AND 5),
 CONSTRAINT hospital_review_author_place_uq UNIQUE(author_member_id, hospital_name, address)
);
CREATE INDEX hospital_review_place_ix ON hospital_review(hospital_name, created_at DESC, id);
