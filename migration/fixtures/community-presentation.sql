-- Non-destructive editorial update of known synthetic fixture rows only.
-- Does not reset activity, ownership, lifecycle, dates, or real/user-edited content.
BEGIN;
UPDATE publication
SET title = CASE animal_interest_code
  WHEN 'DOG' THEN '강아지' WHEN 'CAT' THEN '고양이' WHEN 'PARROT' THEN '앵무새'
  WHEN 'BIRD' THEN '조류' WHEN 'TURTLE' THEN '거북' WHEN 'LIZARD' THEN '도마뱀'
  WHEN 'SNAKE' THEN '뱀' WHEN 'AMPHIBIAN' THEN '양서류' WHEN 'REPTILE' THEN '파충류'
  WHEN 'SMALL_ANIMAL' THEN '소동물' WHEN 'AQUARIUM_FISH' THEN '어류'
  ELSE '반려동물' END || CASE type
  WHEN 'FREE_BOARD' THEN ' 보호자의 하루 루틴을 공유합니다'
  WHEN 'QA_QUESTION' THEN ' 먹이와 활동 시간을 어떻게 기록하시나요?'
  WHEN 'PET_SHOWCASE' THEN '에게 마련해 준 새로운 공간'
  ELSE ' 용품을 고를 때 확인한 세 가지' END,
  updated_at = CURRENT_TIMESTAMP, version = version + 1
WHERE id BETWEEN '00000000-0000-4000-8000-200000000001'::uuid AND '00000000-0000-4000-8000-200000000048'::uuid
  AND author_member_id IN ('00000000-0000-4000-8000-000000000201','00000000-0000-4000-8000-000000000202','00000000-0000-4000-8000-000000000203','00000000-0000-4000-8000-000000000204')
  AND title LIKE '% 데모 %';
UPDATE market_listing
SET description = '분해 가능한 캣타워입니다. 사용감이 있으며 직거래 가능합니다.', updated_at = CURRENT_TIMESTAMP, version = version + 1
WHERE id = '00000000-0000-4000-8000-000000009101'::uuid
  AND owner_member_id = '00000000-0000-4000-8000-000000000201'::uuid
  AND description = '분해 가능한 캣타워입니다. 사용감은 사진과 동일하며 직거래 가능합니다.';
COMMIT;
