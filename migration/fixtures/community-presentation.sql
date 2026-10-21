-- Non-destructive editorial update of known synthetic fixture rows only.
-- Does not reset activity, ownership, lifecycle, or user-edited content.
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

-- Stagger the known synthetic HOT examples that were imported with one
-- identical timestamp. Exact ID/title/time guards keep this out of user posts
-- and make the editorial correction a one-time operation.
UPDATE publication AS post
SET created_at = sample.created_at,
    updated_at = sample.created_at,
    version = post.version + 1
FROM (VALUES
  ('01a0a9c0-3d67-7d49-a5b8-45d6df415e5e'::uuid, '장난감은 많이 사는 것보다 교체 주기가 중요하네요', TIMESTAMPTZ '2026-09-16 19:25:00+09:00'),
  ('01a0a9c0-3ab4-72ec-9c93-add222640561'::uuid, '발톱 깎기를 한 번에 끝내려다 서로 놀랐습니다', TIMESTAMPTZ '2026-09-16 19:08:00+09:00'),
  ('01a0a9c0-3810-74c5-9c91-1250606f11b7'::uuid, '대형 캣타워보다 창가 선반을 더 오래 씁니다', TIMESTAMPTZ '2026-09-16 18:42:00+09:00'),
  ('01a0a9c0-3565-759a-8907-d0a547fdfbab'::uuid, '허물 벗고 색이 달라진 날 사진 남겨봤습니다', TIMESTAMPTZ '2026-09-16 18:19:00+09:00'),
  ('01a0a9c0-328e-75da-a058-1c0f2f65d8d5'::uuid, '탈피 전 일주일은 집 분위기가 완전히 달라집니다', TIMESTAMPTZ '2026-09-16 17:54:00+09:00'),
  ('01a0a9c0-2d71-7c14-b218-8ed089200fbd'::uuid, '조명 위치를 바꿨더니 생활 패턴이 달라졌습니다', TIMESTAMPTZ '2026-09-16 17:28:00+09:00'),
  ('01a0a9c0-2ab3-7cd3-80db-600dfec0596f'::uuid, '온습도계 두 개를 놓았더니 숫자가 달랐던 이유', TIMESTAMPTZ '2026-09-16 16:57:00+09:00'),
  ('01a0a9c0-27fb-7542-8ab0-0dd2e86aefe2'::uuid, '가르친 적 없는 말을 매일 아침 먼저 합니다', TIMESTAMPTZ '2026-09-16 16:31:00+09:00'),
  ('01a0a9c0-2551-76f8-86fc-b7aef8de862d'::uuid, '비 오는 날 산책 포기했더니 집이 난장판이 됐습니다', TIMESTAMPTZ '2026-09-16 16:04:00+09:00'),
  ('01a0a9c0-22c5-77af-8768-faf5b39c6ddb'::uuid, '자동급식기 샀더니 밥시간이 더 시끄러워졌습니다', TIMESTAMPTZ '2026-09-16 15:38:00+09:00')
) AS sample(id, title, created_at)
WHERE post.id = sample.id
  AND post.title = sample.title
  AND post.created_at = TIMESTAMPTZ '2026-09-16 19:25:00+09:00'
  AND post.updated_at = post.created_at;
COMMIT;
