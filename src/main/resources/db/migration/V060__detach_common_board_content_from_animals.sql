-- Common boards are intentionally shared across animal families. Remove the
-- transitional tags written by the first animal-community projection.
DELETE FROM content_animal_community
WHERE content_kind IN (
    'ADOPTION',
    'LOST_FOUND',
    'HOSPITAL_REVIEW',
    'GATHERING',
    'MARKETPLACE',
    'CARE_REQUEST',
    'VOLUNTEER'
);
