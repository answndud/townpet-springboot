-- TownPet local community demo fixture.
-- Synthetic, deterministic, and safe to rerun. It is separate from local-demo.sql
-- so a recreated image/volume can be restored without editing the base fixture.
BEGIN;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM member_account WHERE email = 'demo-member-1@townpet.local')
     OR NOT EXISTS (SELECT 1 FROM animal_interest_option WHERE code = 'DOG') THEN
    RAISE EXCEPTION 'Run the TownPet application and local-demo.sql first';
  END IF;
END
$$;

DELETE FROM engagement_comment
WHERE publication_id IN (
  SELECT id FROM publication
  WHERE id >= '00000000-0000-4000-8000-200000000001'::uuid
    AND id <= '00000000-0000-4000-8000-200000000048'::uuid
);
DELETE FROM engagement_reaction
WHERE publication_id IN (
  SELECT id FROM publication
  WHERE id >= '00000000-0000-4000-8000-200000000001'::uuid
    AND id <= '00000000-0000-4000-8000-200000000048'::uuid
);
DELETE FROM engagement_bookmark
WHERE publication_id IN (
  SELECT id FROM publication
  WHERE id >= '00000000-0000-4000-8000-200000000001'::uuid
    AND id <= '00000000-0000-4000-8000-200000000048'::uuid
);
DELETE FROM publication_metric
WHERE publication_id IN (
  SELECT id FROM publication
  WHERE id >= '00000000-0000-4000-8000-200000000001'::uuid
    AND id <= '00000000-0000-4000-8000-200000000048'::uuid
);
-- Local-only voter accounts provide enough distinct authors to exercise the
-- LIKE uniqueness constraint at the 10/20 recommendation thresholds.
DELETE FROM member_account
WHERE email LIKE 'demo-voter-%@townpet.local';
DELETE FROM content_animal_community
WHERE content_kind = 'PUBLICATION'
  AND content_id >= '00000000-0000-4000-8000-200000000001'::uuid
  AND content_id <= '00000000-0000-4000-8000-200000000048'::uuid;
DELETE FROM publication
WHERE id >= '00000000-0000-4000-8000-200000000001'::uuid
  AND id <= '00000000-0000-4000-8000-200000000048'::uuid;

INSERT INTO member_account (id, email, nickname)
SELECT
  md5('townpet-local-voter-' || voter_no::TEXT)::UUID,
  'demo-voter-' || lpad(voter_no::TEXT, 2, '0') || '@townpet.local',
  'demo-voter-' || lpad(voter_no::TEXT, 2, '0')
FROM generate_series(1, 48) AS voter_no
ON CONFLICT (id) DO UPDATE
SET email = EXCLUDED.email,
    nickname = EXCLUDED.nickname;

DO $$
DECLARE
  animal_codes TEXT[] := ARRAY['DOG','CAT','PARROT','BIRD','TURTLE','LIZARD','SNAKE','AMPHIBIAN','REPTILE','SMALL_ANIMAL','AQUARIUM_FISH','ARTHROPOD_INSECT'];
  animal_labels TEXT[] := ARRAY['강아지','고양이','앵무새','새','거북이','도마뱀','뱀','양서류','파충류','소동물','관상어','절지동물·곤충'];
  post_types TEXT[] := ARRAY['FREE_BOARD','QA_QUESTION','PET_SHOWCASE','PRODUCT_REVIEW'];
  type_labels TEXT[] := ARRAY['자유게시판','질문','사진 자랑','용품 후기'];
  post_no INTEGER;
  animal_no INTEGER;
  type_no INTEGER;
  publication_id UUID;
  first_comment_id UUID;
  comment_id UUID;
  author_id UUID;
  comment_no INTEGER;
BEGIN
  FOR animal_no IN 1..array_length(animal_codes, 1) LOOP
    FOR type_no IN 1..array_length(post_types, 1) LOOP
      post_no := ((animal_no - 1) * 4) + type_no;
      publication_id := ('00000000-0000-4000-8000-' || lpad((200000000000 + post_no)::TEXT, 12, '0'))::UUID;
      author_id := CASE ((post_no - 1) % 4)
        WHEN 0 THEN '00000000-0000-4000-8000-000000000201'::UUID
        WHEN 1 THEN '00000000-0000-4000-8000-000000000202'::UUID
        WHEN 2 THEN '00000000-0000-4000-8000-000000000203'::UUID
        ELSE '00000000-0000-4000-8000-000000000204'::UUID
      END;

      INSERT INTO publication (
        id, author_member_id, type, animal_interest_code,
        title, body, lifecycle, created_at, updated_at, version
      )
      VALUES (
        publication_id, author_id, post_types[type_no], animal_codes[animal_no],
        animal_labels[animal_no] || ' ' || type_labels[type_no] || ' 데모 ' || lpad(post_no::TEXT, 2, '0'),
        CASE post_types[type_no]
          WHEN 'FREE_BOARD' THEN animal_labels[animal_no] || ' 가족이 함께 참고할 산책·생활 팁을 정리했습니다. 처음 시작하는 보호자도 편하게 경험을 나눠 주세요.'
          WHEN 'QA_QUESTION' THEN animal_labels[animal_no] || '와 함께 지내며 생긴 질문입니다. 비슷한 경험이 있다면 준비물과 주의할 점을 알려 주세요.'
          WHEN 'PET_SHOWCASE' THEN '우리 집 ' || animal_labels[animal_no] || '의 최근 모습입니다. 건강하게 지내는 루틴과 좋아하는 놀이를 공유해요.'
          ELSE animal_labels[animal_no] || ' 관련 용품을 직접 사용해 본 후기입니다. 장점과 아쉬운 점을 함께 기록해 둡니다.'
        END,
        'ACTIVE',
        ('2026-08-11T' || lpad((6 + ((post_no - 1) % 12))::TEXT, 2, '0') || ':00:00+09:00')::TIMESTAMPTZ,
        ('2026-08-11T' || lpad((6 + ((post_no - 1) % 12))::TEXT, 2, '0') || ':00:00+09:00')::TIMESTAMPTZ,
        0
      );

      INSERT INTO content_animal_community (content_kind, content_id, animal_code)
      VALUES ('PUBLICATION', publication_id, animal_codes[animal_no])
      ON CONFLICT DO NOTHING;

      FOR comment_no IN 1..8 LOOP
        comment_id := ('00000000-0000-4000-8000-' || lpad((400000000000 + ((post_no - 1) * 8) + comment_no)::TEXT, 12, '0'))::UUID;
        IF comment_no = 1 THEN
          first_comment_id := comment_id;
        END IF;
        author_id := CASE ((post_no + comment_no) % 4)
          WHEN 0 THEN '00000000-0000-4000-8000-000000000201'::UUID
          WHEN 1 THEN '00000000-0000-4000-8000-000000000202'::UUID
          WHEN 2 THEN '00000000-0000-4000-8000-000000000203'::UUID
          ELSE '00000000-0000-4000-8000-000000000204'::UUID
        END;
        INSERT INTO engagement_comment (
          id, publication_id, author_member_id, parent_comment_id, body,
          lifecycle, created_at, updated_at, version
        )
        VALUES (
          comment_id, publication_id, author_id,
          CASE WHEN comment_no IN (3, 4) THEN first_comment_id ELSE NULL END,
          CASE WHEN comment_no IN (3, 4)
            THEN '첫 댓글에 공감해요. 저도 다음에 시도해 보고 결과를 공유할게요.'
            WHEN type_no = 1
            THEN '정리해 주셔서 감사합니다. 실제로 적용해 본 뒤 다시 알려 드릴게요.'
            ELSE '저희 집에서도 비슷한 경험이 있었어요. 천천히 관찰하면 도움이 되더라고요.'
          END,
          'ACTIVE',
          ('2026-08-12T' || lpad((8 + comment_no)::TEXT, 2, '0') || ':00:00+09:00')::TIMESTAMPTZ,
          ('2026-08-12T' || lpad((8 + comment_no)::TEXT, 2, '0') || ':00:00+09:00')::TIMESTAMPTZ,
          0
        );
      END LOOP;
    END LOOP;
  END LOOP;
END
$$;

-- Seed visible popular/HOT examples for the local feed. All 48 animal posts
-- receive 48 down to 1 LIKEs, so both thresholds have enough rows to exercise
-- scrolling and pagination while the boundary remains visible.
DO $$
DECLARE
  hot_post_no INTEGER;
  reaction_no INTEGER;
  publication_id UUID;
  reaction_id UUID;
  recommendation_count INTEGER;
BEGIN
  FOR hot_post_no IN 1..48 LOOP
    publication_id := ('00000000-0000-4000-8000-' || lpad((200000000000 + hot_post_no)::TEXT, 12, '0'))::UUID;
    recommendation_count := 49 - hot_post_no;
    FOR reaction_no IN 1..recommendation_count LOOP
      reaction_id := ('00000000-0000-4000-8000-' || lpad((500000000000 + ((hot_post_no - 1) * 48) + reaction_no)::TEXT, 12, '0'))::UUID;
      INSERT INTO engagement_reaction (id, publication_id, author_member_id, type, created_at)
      VALUES (
        reaction_id,
        publication_id,
        md5('townpet-local-voter-' || reaction_no::TEXT)::UUID,
        'LIKE',
        ('2026-08-12T' || lpad((10 + ((hot_post_no - 1) % 12))::TEXT, 2, '0') || ':' || lpad(reaction_no::TEXT, 2, '0') || ':00+09:00')::TIMESTAMPTZ
      );
    END LOOP;
  END LOOP;
END
$$;

DELETE FROM market_listing WHERE id IN (
  '00000000-0000-4000-8000-000000009101',
  '00000000-0000-4000-8000-000000009102',
  '00000000-0000-4000-8000-000000009103'
);
INSERT INTO market_listing (id, owner_member_id, kind, status, title, description, price_krw, created_at, updated_at, version)
VALUES
  ('00000000-0000-4000-8000-000000009101', '00000000-0000-4000-8000-000000000201', 'SELL', 'AVAILABLE', '캣타워 상태 좋은 제품', '분해 가능한 캣타워입니다. 사용감은 사진과 동일하며 직거래 가능합니다.', 35000, '2026-08-11T09:00:00+09:00', '2026-08-11T09:00:00+09:00', 0),
  ('00000000-0000-4000-8000-000000009102', '00000000-0000-4000-8000-000000000202', 'RENT', 'AVAILABLE', '대형 이동장 단기 대여', '병원 방문이나 이사 때 필요한 분께 일주일 단위로 대여합니다.', 10000, '2026-08-10T09:00:00+09:00', '2026-08-10T09:00:00+09:00', 0),
  ('00000000-0000-4000-8000-000000009103', '00000000-0000-4000-8000-000000000203', 'SHARE', 'AVAILABLE', '미개봉 사료 샘플 나눔', '알레르기 확인용으로 소분한 샘플을 필요한 가족에게 나눔합니다.', NULL, '2026-08-09T09:00:00+09:00', '2026-08-09T09:00:00+09:00', 0);

DELETE FROM adoption_listing WHERE id IN (
  '00000000-0000-4000-8000-000000009201',
  '00000000-0000-4000-8000-000000009202',
  '00000000-0000-4000-8000-000000009203'
);
INSERT INTO adoption_listing (id, publisher_member_id, neighborhood_id, title, description, species, breed, status, created_at, updated_at, version)
VALUES
  ('00000000-0000-4000-8000-000000009201', '00000000-0000-4000-8000-000000000204', '00000000-0000-4000-8000-000000000101', '활발한 토끼 가족을 찾습니다', '사람 손을 좋아하고 건강 검진을 마친 토끼입니다. 생활 환경을 충분히 상담합니다.', 'SMALL_ANIMAL', '네덜란드 드워프', 'OPEN', '2026-08-11T08:00:00+09:00', '2026-08-11T08:00:00+09:00', 0),
  ('00000000-0000-4000-8000-000000009202', '00000000-0000-4000-8000-000000000201', '00000000-0000-4000-8000-000000000102', '조용한 앵무새 입양 상담', '기본 훈련이 된 앵무새입니다. 충분한 사육 환경을 확인한 뒤 진행합니다.', 'PARROT', '코뉴어', 'OPEN', '2026-08-10T08:00:00+09:00', '2026-08-10T08:00:00+09:00', 0),
  ('00000000-0000-4000-8000-000000009203', '00000000-0000-4000-8000-000000000202', '00000000-0000-4000-8000-000000000101', '건강한 금붕어 분양 안내', '수조 환경과 환수 주기를 확인할 수 있는 분께 분양합니다.', 'AQUARIUM_FISH', '금붕어', 'OPEN', '2026-08-09T08:00:00+09:00', '2026-08-09T08:00:00+09:00', 0);

DELETE FROM lost_found_alert WHERE id IN (
  '00000000-0000-4000-8000-000000009301',
  '00000000-0000-4000-8000-000000009302',
  '00000000-0000-4000-8000-000000009303'
);
INSERT INTO lost_found_alert (id, reporter_member_id, kind, status, title, description, last_seen_at, approx_location, created_at, updated_at, version)
VALUES
  ('00000000-0000-4000-8000-000000009301', '00000000-0000-4000-8000-000000000201', 'LOST', 'ACTIVE', '서교동에서 초록색 앵무새를 찾습니다', '사람 목소리에 반응하는 앵무새입니다. 발견하면 조용히 사진과 위치를 제보해 주세요.', '2026-08-11T07:00:00+09:00', ST_SetSRID(ST_MakePoint(126.91, 37.55), 4326)::geography, '2026-08-11T07:30:00+09:00', '2026-08-11T07:30:00+09:00', 0),
  ('00000000-0000-4000-8000-000000009302', '00000000-0000-4000-8000-000000000202', 'FOUND', 'ACTIVE', '잠실에서 발견한 작은 거북이', '안전하게 보호 중입니다. 사진으로 특징을 확인할 보호자를 기다립니다.', '2026-08-10T06:00:00+09:00', ST_SetSRID(ST_MakePoint(127.08, 37.51), 4326)::geography, '2026-08-10T06:30:00+09:00', '2026-08-10T06:30:00+09:00', 0),
  ('00000000-0000-4000-8000-000000009303', '00000000-0000-4000-8000-000000000203', 'LOST', 'ACTIVE', '연남동에서 사라진 도마뱀 제보', '작은 도마뱀이며 꼬리 끝에 무늬가 있습니다. 발견 시 가까운 실내로 유도해 주세요.', '2026-08-09T05:00:00+09:00', ST_SetSRID(ST_MakePoint(126.92, 37.56), 4326)::geography, '2026-08-09T05:30:00+09:00', '2026-08-09T05:30:00+09:00', 0);

DELETE FROM hospital_review WHERE id IN (
  '00000000-0000-4000-8000-000000009401',
  '00000000-0000-4000-8000-000000009402',
  '00000000-0000-4000-8000-000000009403'
);
INSERT INTO hospital_review (id, author_member_id, hospital_name, address, rating, body, created_at, updated_at, version)
VALUES
  ('00000000-0000-4000-8000-000000009401', '00000000-0000-4000-8000-000000000203', '연남동물의료센터', '서울 마포구 동교로 30', 5, '검사 결과와 선택지를 차분하게 설명해 주셔서 좋았습니다.', '2026-08-11T07:00:00+09:00', '2026-08-11T07:00:00+09:00', 0),
  ('00000000-0000-4000-8000-000000009402', '00000000-0000-4000-8000-000000000204', '잠실우리동물병원', '서울 송파구 올림픽로 40', 4, '접수부터 진료까지 안내가 명확했습니다. 주차 정보를 미리 확인하세요.', '2026-08-10T07:00:00+09:00', '2026-08-10T07:00:00+09:00', 0),
  ('00000000-0000-4000-8000-000000009403', '00000000-0000-4000-8000-000000000201', '동네고양이클리닉', '서울 성동구 왕십리로 50', 4, '고양이 대기 공간이 분리되어 있어 긴장이 덜했습니다.', '2026-08-09T07:00:00+09:00', '2026-08-09T07:00:00+09:00', 0);

DELETE FROM gathering WHERE id IN (
  '00000000-0000-4000-8000-000000009501',
  '00000000-0000-4000-8000-000000009502',
  '00000000-0000-4000-8000-000000009503'
);
INSERT INTO gathering (id, host_member_id, title, description, location, starts_at, capacity, status, created_at, version)
VALUES
  ('00000000-0000-4000-8000-000000009501', '00000000-0000-4000-8000-000000000201', '반려동물 사진 산책 모임', '동네를 천천히 걷고 서로의 촬영 팁을 나눕니다.', '서울숲 남문', '2026-08-28T10:00:00+09:00', 10, 'ACTIVE', '2026-08-11T06:00:00+09:00', 0),
  ('00000000-0000-4000-8000-000000009502', '00000000-0000-4000-8000-000000000202', '소동물 보호자 첫 만남', '소동물 사육 환경과 병원 정보를 편하게 나눠요.', '합정 커뮤니티룸', '2026-08-30T05:00:00+09:00', 8, 'ACTIVE', '2026-08-10T06:00:00+09:00', 0),
  ('00000000-0000-4000-8000-000000009503', '00000000-0000-4000-8000-000000000203', '입양 전 준비물 체크 모임', '입양 전에 집을 어떻게 준비했는지 서로의 목록을 공유합니다.', '온라인 화상 모임', '2026-09-01T09:00:00+09:00', 20, 'ACTIVE', '2026-08-09T06:00:00+09:00', 0);

DELETE FROM care_request WHERE id IN (
  '00000000-0000-4000-8000-000000009601',
  '00000000-0000-4000-8000-000000009602',
  '00000000-0000-4000-8000-000000009603'
);
INSERT INTO care_request (id, requester_member_id, title, description, location, starts_at, ends_at, reward_hint, status, created_at, updated_at, version)
VALUES
  ('00000000-0000-4000-8000-000000009601', '00000000-0000-4000-8000-000000000203', '출장 중 파충류 급여 도움', '출장 기간 동안 정해진 시간에 급여와 온습도만 확인해 주세요.', '서울 용산구', '2026-08-25T08:00:00+09:00', '2026-08-27T09:00:00+09:00', '사례 협의', 'OPEN', '2026-08-11T05:00:00+09:00', '2026-08-11T05:00:00+09:00', 0),
  ('00000000-0000-4000-8000-000000009602', '00000000-0000-4000-8000-000000000204', '주말 새 돌봄 부탁드려요', '주말 동안 물과 모이를 확인해 주실 이웃을 찾습니다.', '서울 광진구', '2026-08-29T01:00:00+09:00', '2026-08-30T09:00:00+09:00', '간식 나눔', 'OPEN', '2026-08-10T05:00:00+09:00', '2026-08-10T05:00:00+09:00', 0),
  ('00000000-0000-4000-8000-000000009603', '00000000-0000-4000-8000-000000000201', '저녁 산책 동행 요청', '퇴근 시간에 맞춰 짧은 산책을 함께해 주실 분을 찾습니다.', '서울 마포구', '2026-08-26T09:00:00+09:00', '2026-08-26T10:00:00+09:00', '음료 제공', 'OPEN', '2026-08-09T05:00:00+09:00', '2026-08-09T05:00:00+09:00', 0);

DELETE FROM volunteer_opportunity WHERE id IN (
  '00000000-0000-4000-8000-000000009701',
  '00000000-0000-4000-8000-000000009702',
  '00000000-0000-4000-8000-000000009703'
);
INSERT INTO volunteer_opportunity (id, publisher_member_id, title, description, organization, location, starts_at, capacity, status, created_at, updated_at, version)
VALUES
  ('00000000-0000-4000-8000-000000009701', '00000000-0000-4000-8000-000000000204', '보호소 고양이 사회화 봉사', '고양이들의 놀이와 환경 정리를 돕습니다.', '마포 반려동물 보호소', '서울 마포구', '2026-08-23T01:00:00+09:00', 8, 'OPEN', '2026-08-11T04:00:00+09:00', '2026-08-11T04:00:00+09:00', 0),
  ('00000000-0000-4000-8000-000000009702', '00000000-0000-4000-8000-000000000201', '입양 행사 안내 스태프', '입양 상담 부스에서 방문자에게 준비물을 안내합니다.', '성동 입양센터', '서울 성동구', '2026-08-30T02:00:00+09:00', 10, 'OPEN', '2026-08-10T04:00:00+09:00', '2026-08-10T04:00:00+09:00', 0),
  ('00000000-0000-4000-8000-000000009703', '00000000-0000-4000-8000-000000000202', '야생동물 구조 물품 정리', '기부받은 물품을 분류하고 기록하는 봉사입니다.', '서울 야생동물센터', '서울 강서구', '2026-09-05T01:00:00+09:00', 6, 'OPEN', '2026-08-09T04:00:00+09:00', '2026-08-09T04:00:00+09:00', 0);

COMMIT;
