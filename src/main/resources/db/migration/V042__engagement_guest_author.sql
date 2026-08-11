ALTER TABLE engagement_comment
    ALTER COLUMN author_member_id DROP NOT NULL,
    ADD COLUMN guest_author_id UUID NULL;

ALTER TABLE engagement_comment
    ADD CONSTRAINT engagement_comment_guest_author_fk FOREIGN KEY (guest_author_id)
        REFERENCES guest_author (id) ON DELETE RESTRICT,
    ADD CONSTRAINT engagement_comment_single_author_ck CHECK (
        (author_member_id IS NOT NULL AND guest_author_id IS NULL)
        OR (author_member_id IS NULL AND guest_author_id IS NOT NULL)
    );
