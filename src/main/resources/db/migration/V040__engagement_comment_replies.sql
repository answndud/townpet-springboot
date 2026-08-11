ALTER TABLE engagement_comment
    ADD COLUMN parent_comment_id UUID NULL;

ALTER TABLE engagement_comment
    ADD CONSTRAINT engagement_comment_parent_fk FOREIGN KEY (parent_comment_id)
        REFERENCES engagement_comment (id) ON DELETE RESTRICT;

CREATE INDEX engagement_comment_parent_ix
    ON engagement_comment (parent_comment_id, lifecycle, created_at ASC, id ASC);
