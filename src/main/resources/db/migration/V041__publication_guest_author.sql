ALTER TABLE publication
    ALTER COLUMN author_member_id DROP NOT NULL,
    ADD COLUMN guest_author_id UUID NULL;

ALTER TABLE publication
    ADD CONSTRAINT publication_guest_author_fk FOREIGN KEY (guest_author_id)
        REFERENCES guest_author (id) ON DELETE RESTRICT,
    ADD CONSTRAINT publication_single_author_ck CHECK (
        (author_member_id IS NOT NULL AND guest_author_id IS NULL)
        OR (author_member_id IS NULL AND guest_author_id IS NOT NULL)
    );

CREATE INDEX publication_guest_author_ix ON publication (guest_author_id, lifecycle, created_at DESC);
