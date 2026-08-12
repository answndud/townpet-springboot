CREATE INDEX volunteer_opportunity_public_ix
    ON volunteer_opportunity (status, starts_at, id);

CREATE INDEX volunteer_application_opportunity_ix
    ON volunteer_application (opportunity_id, created_at, id);

CREATE INDEX care_feedback_assignment_created_ix
    ON care_feedback (assignment_id, created_at, id);

CREATE INDEX identity_auth_audit_created_ix
    ON identity_auth_audit (created_at DESC, id DESC);

CREATE INDEX moderation_action_created_ix
    ON moderation_action (created_at DESC, id DESC);

CREATE INDEX engagement_comment_author_created_ix
    ON engagement_comment (author_member_id, lifecycle, created_at DESC, id DESC);

CREATE INDEX engagement_reaction_author_created_ix
    ON engagement_reaction (author_member_id, created_at DESC, id DESC);

CREATE INDEX trust_report_queue_stable_ix
    ON trust_report (status, created_at ASC, id ASC);
