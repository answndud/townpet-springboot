ALTER TABLE publication
    DROP CONSTRAINT IF EXISTS publication_type_ck;

ALTER TABLE publication
    ADD CONSTRAINT publication_type_ck CHECK (
        type IN ('FREE_BOARD', 'QA_QUESTION', 'PET_SHOWCASE', 'PRODUCT_REVIEW')
    );

CREATE TABLE content_animal_community (
    content_kind VARCHAR(40) NOT NULL,
    content_id UUID NOT NULL,
    animal_code VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT content_animal_community_pk PRIMARY KEY (content_kind, content_id, animal_code),
    CONSTRAINT content_animal_community_animal_fk FOREIGN KEY (animal_code)
        REFERENCES animal_interest_option (code) ON DELETE RESTRICT
);

CREATE INDEX content_animal_community_lookup_ix
    ON content_animal_community (animal_code, content_kind, created_at DESC, content_id DESC);

INSERT INTO content_animal_community (content_kind, content_id, animal_code)
SELECT 'PUBLICATION', p.id, p.animal_interest_code
FROM publication p
WHERE p.animal_interest_code IS NOT NULL
ON CONFLICT DO NOTHING;

INSERT INTO content_animal_community (content_kind, content_id, animal_code)
SELECT
    'ADOPTION',
    a.id,
    CASE
        WHEN UPPER(a.species) LIKE 'DOG%' OR UPPER(a.species) LIKE '%강아지%' THEN 'DOG'
        WHEN UPPER(a.species) LIKE 'CAT%' OR UPPER(a.species) LIKE '%고양이%' THEN 'CAT'
        WHEN UPPER(a.species) LIKE '%PARROT%' OR UPPER(a.species) LIKE '%앵무%' THEN 'PARROT'
        WHEN UPPER(a.species) LIKE '%BIRD%' OR UPPER(a.species) LIKE '%조류%' THEN 'BIRD'
        WHEN UPPER(a.species) LIKE '%TURTLE%' OR UPPER(a.species) LIKE '%거북%' THEN 'TURTLE'
        WHEN UPPER(a.species) LIKE '%LIZARD%' OR UPPER(a.species) LIKE '%도마뱀%' THEN 'LIZARD'
        WHEN UPPER(a.species) LIKE '%SNAKE%' OR UPPER(a.species) LIKE '%뱀%' THEN 'SNAKE'
        WHEN UPPER(a.species) LIKE '%AMPHIBIAN%' OR UPPER(a.species) LIKE '%양서%' THEN 'AMPHIBIAN'
        WHEN UPPER(a.species) LIKE '%REPTILE%' OR UPPER(a.species) LIKE '%파충%' THEN 'REPTILE'
        WHEN UPPER(a.species) LIKE '%FISH%' OR UPPER(a.species) LIKE '%어류%' OR UPPER(a.species) LIKE '%수조%' THEN 'AQUARIUM_FISH'
        ELSE NULL
    END
FROM adoption_listing a
WHERE CASE
        WHEN UPPER(a.species) LIKE 'DOG%' OR UPPER(a.species) LIKE '%강아지%' THEN 'DOG'
        WHEN UPPER(a.species) LIKE 'CAT%' OR UPPER(a.species) LIKE '%고양이%' THEN 'CAT'
        WHEN UPPER(a.species) LIKE '%PARROT%' OR UPPER(a.species) LIKE '%앵무%' THEN 'PARROT'
        WHEN UPPER(a.species) LIKE '%BIRD%' OR UPPER(a.species) LIKE '%조류%' THEN 'BIRD'
        WHEN UPPER(a.species) LIKE '%TURTLE%' OR UPPER(a.species) LIKE '%거북%' THEN 'TURTLE'
        WHEN UPPER(a.species) LIKE '%LIZARD%' OR UPPER(a.species) LIKE '%도마뱀%' THEN 'LIZARD'
        WHEN UPPER(a.species) LIKE '%SNAKE%' OR UPPER(a.species) LIKE '%뱀%' THEN 'SNAKE'
        WHEN UPPER(a.species) LIKE '%AMPHIBIAN%' OR UPPER(a.species) LIKE '%양서%' THEN 'AMPHIBIAN'
        WHEN UPPER(a.species) LIKE '%REPTILE%' OR UPPER(a.species) LIKE '%파충%' THEN 'REPTILE'
        WHEN UPPER(a.species) LIKE '%FISH%' OR UPPER(a.species) LIKE '%어류%' OR UPPER(a.species) LIKE '%수조%' THEN 'AQUARIUM_FISH'
        ELSE NULL
    END IS NOT NULL
ON CONFLICT DO NOTHING;
