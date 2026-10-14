ALTER TABLE engagement_comment
    DROP CONSTRAINT engagement_comment_lifecycle_ck;

ALTER TABLE engagement_comment
    ADD CONSTRAINT engagement_comment_lifecycle_ck
        CHECK (lifecycle IN ('ACTIVE', 'DELETED', 'MODERATOR_DELETED'));
