CREATE OR REPLACE FUNCTION assert_engagement_allowed(publication_id UUID, actor_member UUID)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    publication_author UUID;
BEGIN
    SELECT p.author_member_id
      INTO publication_author
      FROM publication p
     WHERE p.id = publication_id
       AND p.lifecycle = 'ACTIVE';

    IF publication_author IS NOT NULL
       AND EXISTS (
           SELECT 1
             FROM relationship_block b
            WHERE b.blocker_member_id = actor_member
              AND b.blocked_member_id = publication_author
       ) THEN
        RAISE EXCEPTION 'engagement is blocked by publication author policy'
            USING ERRCODE = '23514';
    END IF;
    RETURN;
END;
$$;

CREATE OR REPLACE FUNCTION reject_blocked_comment()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    PERFORM assert_engagement_allowed(NEW.publication_id, NEW.author_member_id);
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION reject_blocked_reaction()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    PERFORM assert_engagement_allowed(NEW.publication_id, NEW.author_member_id);
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION reject_blocked_bookmark()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    PERFORM assert_engagement_allowed(NEW.publication_id, NEW.member_id);
    RETURN NEW;
END;
$$;

CREATE TRIGGER engagement_comment_block_guard
    BEFORE INSERT ON engagement_comment
    FOR EACH ROW EXECUTE FUNCTION reject_blocked_comment();

CREATE TRIGGER engagement_reaction_block_guard
    BEFORE INSERT ON engagement_reaction
    FOR EACH ROW EXECUTE FUNCTION reject_blocked_reaction();

CREATE TRIGGER engagement_bookmark_block_guard
    BEFORE INSERT ON engagement_bookmark
    FOR EACH ROW EXECUTE FUNCTION reject_blocked_bookmark();
