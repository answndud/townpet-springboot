CREATE TABLE breed (
    code VARCHAR(40) NOT NULL,
    species VARCHAR(20) NOT NULL,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(500) NOT NULL,
    CONSTRAINT breed_pk PRIMARY KEY (code),
    CONSTRAINT breed_species_ck CHECK (species IN ('DOG', 'CAT', 'OTHER'))
);

INSERT INTO breed (code, species, name, description) VALUES
    ('golden-retriever', 'DOG', '골든 리트리버', '사람과 함께 활동하는 것을 좋아하는 대표적인 반려견 품종입니다.'),
    ('maltese', 'DOG', '말티즈', '작은 체구와 밝은 성격으로 많은 사랑을 받는 반려견 품종입니다.'),
    ('domestic-short-hair', 'CAT', '코리안 숏헤어', '다양한 털색과 개성을 가진 친근한 고양이 품종입니다.')
ON CONFLICT (code) DO NOTHING;
