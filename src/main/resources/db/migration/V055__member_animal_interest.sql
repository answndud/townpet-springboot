CREATE TABLE animal_interest_option (
    code VARCHAR(40) NOT NULL,
    group_label VARCHAR(80) NOT NULL,
    label VARCHAR(80) NOT NULL,
    sort_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT animal_interest_option_pk PRIMARY KEY (code),
    CONSTRAINT animal_interest_option_sort_ck CHECK (sort_order > 0),
    CONSTRAINT animal_interest_option_label_ck CHECK (char_length(btrim(label)) BETWEEN 1 AND 80)
);

CREATE TABLE member_animal_interest (
    id UUID NOT NULL,
    member_id UUID NOT NULL,
    interest_code VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT member_animal_interest_pk PRIMARY KEY (id),
    CONSTRAINT member_animal_interest_member_fk FOREIGN KEY (member_id)
        REFERENCES member_account (id) ON DELETE CASCADE,
    CONSTRAINT member_animal_interest_option_fk FOREIGN KEY (interest_code)
        REFERENCES animal_interest_option (code) ON DELETE RESTRICT,
    CONSTRAINT member_animal_interest_uk UNIQUE (member_id, interest_code)
);

CREATE INDEX member_animal_interest_member_ix
    ON member_animal_interest (member_id, interest_code);

INSERT INTO animal_interest_option (code, group_label, label, sort_order)
VALUES
    ('DOG', '강아지 & 고양이', '강아지', 10),
    ('CAT', '강아지 & 고양이', '고양이', 20),
    ('PARROT', '조류', '앵무새', 30),
    ('BIRD', '조류', '조류', 40),
    ('TURTLE', '파충류 & 양서류', '거북', 50),
    ('LIZARD', '파충류 & 양서류', '도마뱀', 60),
    ('SNAKE', '파충류 & 양서류', '뱀', 70),
    ('AMPHIBIAN', '파충류 & 양서류', '양서류', 80),
    ('REPTILE', '파충류 & 양서류', '파충류', 90),
    ('SMALL_ANIMAL', '소동물', '소동물', 100),
    ('AQUARIUM_FISH', '어류 / 수조', '어류·수조', 110),
    ('ARTHROPOD_INSECT', '기타', '절지류·곤충', 120);
