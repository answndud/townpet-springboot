CREATE INDEX publication_author_lifecycle_ix
    ON publication (author_member_id, lifecycle);
