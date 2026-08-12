ALTER TABLE publication
    ADD COLUMN animal_interest_code VARCHAR(40);

ALTER TABLE publication
    ADD CONSTRAINT publication_animal_interest_fk FOREIGN KEY (animal_interest_code)
        REFERENCES animal_interest_option (code) ON DELETE RESTRICT;

CREATE INDEX publication_animal_interest_feed_ix
    ON publication (animal_interest_code, lifecycle, scope, created_at DESC, id DESC);
