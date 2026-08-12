-- TownPet local-only demo fixture.
-- This file is intentionally synthetic and may be rerun after the local volume is recreated.
BEGIN;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM member_account WHERE email = 'demo-member-1@townpet.local')
     OR NOT EXISTS (SELECT 1 FROM member_account WHERE email = 'demo-member-2@townpet.local')
     OR NOT EXISTS (SELECT 1 FROM member_account WHERE email = 'demo-member-3@townpet.local')
     OR NOT EXISTS (SELECT 1 FROM member_account WHERE email = 'demo-moderator@townpet.local') THEN
    RAISE EXCEPTION 'Run the TownPet application once so Flyway V003 creates demo identities';
  END IF;
END
$$;

-- Keep the moderator credential reproducible without changing an applied Flyway migration.
UPDATE identity_credential
SET password_hash = '$2a$12$8BUhBJsoI/6RnbSYgip/aekAWA1Th6zu0mzAKtLi0rNlkCFJutqOy',
    role = 'MODERATOR',
    enabled = TRUE
WHERE email = 'demo-moderator@townpet.local';

-- Remove only rows owned by this fixture so rerunning the script is safe for other local data.
DELETE FROM care_feedback WHERE id IN ('00000000-0000-4000-8000-00000000f701');
DELETE FROM care_assignment WHERE id IN ('00000000-0000-4000-8000-00000000f601');
DELETE FROM care_application WHERE id IN ('00000000-0000-4000-8000-00000000f501');
DELETE FROM care_request WHERE id IN ('00000000-0000-4000-8000-00000000f401', '00000000-0000-4000-8000-00000000f402');
DELETE FROM volunteer_application WHERE id IN ('00000000-0000-4000-8000-00000000f301');
DELETE FROM volunteer_opportunity WHERE id IN ('00000000-0000-4000-8000-00000000f201', '00000000-0000-4000-8000-00000000f202');
DELETE FROM hospital_review WHERE id IN ('00000000-0000-4000-8000-00000000f101', '00000000-0000-4000-8000-00000000f102');
DELETE FROM adoption_listing WHERE id IN ('00000000-0000-4000-8000-00000000e201', '00000000-0000-4000-8000-00000000e202');
DELETE FROM gathering_participant WHERE id IN ('00000000-0000-4000-8000-00000000e101');
DELETE FROM gathering WHERE id IN ('00000000-0000-4000-8000-00000000d201', '00000000-0000-4000-8000-00000000d202');
DELETE FROM lost_found_location_access_audit WHERE id IN ('00000000-0000-4000-8000-00000000c101');
DELETE FROM lost_found_sighting_report WHERE id IN ('00000000-0000-4000-8000-00000000b101', '00000000-0000-4000-8000-00000000b102');
DELETE FROM lost_found_alert_status_history WHERE id IN ('00000000-0000-4000-8000-00000000a101');
DELETE FROM lost_found_alert WHERE id IN ('00000000-0000-4000-8000-000000009101', '00000000-0000-4000-8000-000000009102');
DELETE FROM market_listing_status_history WHERE id IN ('00000000-0000-4000-8000-000000008101');
DELETE FROM market_listing WHERE id IN ('00000000-0000-4000-8000-000000007101', '00000000-0000-4000-8000-000000007102');
-- The two fixture publications own their comments/reactions/bookmarks. Remove
-- all interactions for them so browser-created test rows do not break a rerun.
DELETE FROM engagement_comment WHERE publication_id IN ('00000000-0000-4000-8000-000000003101', '00000000-0000-4000-8000-000000003102');
DELETE FROM engagement_reaction WHERE publication_id IN ('00000000-0000-4000-8000-000000003101', '00000000-0000-4000-8000-000000003102');
DELETE FROM engagement_bookmark WHERE publication_id IN ('00000000-0000-4000-8000-000000003101', '00000000-0000-4000-8000-000000003102');
DELETE FROM engagement_comment WHERE id IN ('00000000-0000-4000-8000-000000006103', '00000000-0000-4000-8000-000000006102', '00000000-0000-4000-8000-000000006101');
DELETE FROM engagement_reaction WHERE id IN ('00000000-0000-4000-8000-000000005101', '00000000-0000-4000-8000-000000005102');
DELETE FROM engagement_bookmark WHERE id IN ('00000000-0000-4000-8000-000000004101');
DELETE FROM publication_metric WHERE publication_id IN ('00000000-0000-4000-8000-000000003101', '00000000-0000-4000-8000-000000003102');
DELETE FROM publication WHERE id IN ('00000000-0000-4000-8000-000000003101', '00000000-0000-4000-8000-000000003102');
DELETE FROM member_animal_interest WHERE member_id = '00000000-0000-4000-8000-000000000201';

-- Two posts in the community feed. These are the records used to test comments,
-- replies, likes and bookmarks from the browser.
INSERT INTO publication (id, author_member_id, type, scope, neighborhood_id, animal_interest_code, title, body, lifecycle, created_at, updated_at, version)
VALUES
  ('00000000-0000-4000-8000-000000003101', '00000000-0000-4000-8000-000000000201', 'FREE_BOARD', 'GLOBAL', NULL, 'DOG', '망원 산책 초보자를 위한 저녁 코스', '해 질 무렵 걷기 좋은 구간과 물을 챙길 때의 팁을 정리했습니다. 처음 함께 걷는 분들도 편하게 의견을 남겨 주세요.', 'ACTIVE', '2026-08-10T10:00:00+09:00', '2026-08-10T10:00:00+09:00', 0),
  ('00000000-0000-4000-8000-000000003102', '00000000-0000-4000-8000-000000000202', 'FREE_BOARD', 'GLOBAL', NULL, NULL, '반려동물과 이사할 때 먼저 확인한 것들', '이동장, 동물병원 기록, 새 동네 산책로를 준비한 순서를 공유합니다. 다른 보호자님의 체크리스트도 궁금해요.', 'ACTIVE', '2026-08-09T09:30:00+09:00', '2026-08-09T09:30:00+09:00', 0);

INSERT INTO member_animal_interest (id, member_id, interest_code, created_at)
VALUES
  ('00000000-0000-4000-8000-00000000a201', '00000000-0000-4000-8000-000000000201', 'DOG', '2026-08-08T00:00:00Z'),
  ('00000000-0000-4000-8000-00000000a202', '00000000-0000-4000-8000-000000000201', 'CAT', '2026-08-08T00:00:00Z');

INSERT INTO engagement_comment (id, publication_id, author_member_id, parent_comment_id, body, lifecycle, created_at, updated_at, version)
VALUES
  ('00000000-0000-4000-8000-000000006101', '00000000-0000-4000-8000-000000003101', '00000000-0000-4000-8000-000000000202', NULL, '저도 같은 코스로 걸어 봤는데 저녁 7시 이후가 가장 여유로웠어요.', 'ACTIVE', '2026-08-10T11:00:00+09:00', '2026-08-10T11:00:00+09:00', 0),
  ('00000000-0000-4000-8000-000000006102', '00000000-0000-4000-8000-000000003101', '00000000-0000-4000-8000-000000000201', NULL, '좋은 정보 감사합니다. 다음 모임 공지에도 링크할게요.', 'ACTIVE', '2026-08-10T11:30:00+09:00', '2026-08-10T11:30:00+09:00', 0),
  ('00000000-0000-4000-8000-000000006103', '00000000-0000-4000-8000-000000003101', '00000000-0000-4000-8000-000000000203', '00000000-0000-4000-8000-000000006101', '다음에는 물그릇 놓인 장소도 함께 알려주시면 좋겠어요.', 'ACTIVE', '2026-08-10T12:00:00+09:00', '2026-08-10T12:00:00+09:00', 0);

INSERT INTO engagement_reaction (id, publication_id, author_member_id, type, created_at)
VALUES
  ('00000000-0000-4000-8000-000000005101', '00000000-0000-4000-8000-000000003101', '00000000-0000-4000-8000-000000000202', 'LIKE', '2026-08-10T11:05:00+09:00'),
  ('00000000-0000-4000-8000-000000005102', '00000000-0000-4000-8000-000000003101', '00000000-0000-4000-8000-000000000203', 'LIKE', '2026-08-10T11:06:00+09:00');

INSERT INTO engagement_bookmark (id, publication_id, member_id, created_at)
VALUES ('00000000-0000-4000-8000-000000004101', '00000000-0000-4000-8000-000000003101', '00000000-0000-4000-8000-000000000203', '2026-08-10T11:07:00+09:00');

-- Community-adjacent boards.
INSERT INTO market_listing (id, owner_member_id, kind, status, title, description, price_krw, created_at, updated_at, version)
VALUES
  ('00000000-0000-4000-8000-000000007101', '00000000-0000-4000-8000-000000000201', 'SELL', 'AVAILABLE', '소형 이동장 판매합니다', '깨끗하게 사용한 소형 이동장입니다. 망원역 근처 직거래를 희망합니다.', 25000, '2026-08-08T09:00:00+09:00', '2026-08-08T09:00:00+09:00', 0),
  ('00000000-0000-4000-8000-000000007102', '00000000-0000-4000-8000-000000000203', 'SHARE', 'AVAILABLE', '강아지 계단 나눔', '잠깐 사용한 원목 계단을 필요한 이웃에게 나눔합니다.', NULL, '2026-08-07T08:00:00+09:00', '2026-08-07T08:00:00+09:00', 0);

INSERT INTO market_listing_status_history (id, listing_id, actor_member_id, from_status, to_status, changed_at)
VALUES ('00000000-0000-4000-8000-000000008101', '00000000-0000-4000-8000-000000007101', '00000000-0000-4000-8000-000000000201', 'AVAILABLE', 'AVAILABLE', '2026-08-08T09:00:00+09:00');

INSERT INTO lost_found_alert (id, reporter_member_id, kind, status, title, description, last_seen_at, approx_location, created_at, updated_at, version)
VALUES
  ('00000000-0000-4000-8000-000000009101', '00000000-0000-4000-8000-000000000202', 'LOST', 'ACTIVE', '성산동에서 보라색 목줄 강아지를 찾습니다', '보라색 목줄을 착용한 소형견입니다. 낯선 사람을 경계하니 발견하면 가까이 쫓지 말고 제보해 주세요.', '2026-08-10T07:30:00+09:00', ST_SetSRID(ST_MakePoint(126.91, 37.56), 4326)::geography, '2026-08-10T08:00:00+09:00', '2026-08-10T08:00:00+09:00', 0),
  ('00000000-0000-4000-8000-000000009102', '00000000-0000-4000-8000-000000000203', 'FOUND', 'ACTIVE', '성수 서울숲 근처 발견 고양이', '사람을 잘 따르는 고양이를 보호 중입니다. 특징을 확인할 수 있는 보호자 제보를 기다립니다.', '2026-08-09T13:00:00+09:00', ST_SetSRID(ST_MakePoint(127.04, 37.54), 4326)::geography, '2026-08-09T13:30:00+09:00', '2026-08-09T13:30:00+09:00', 0);

INSERT INTO lost_found_sighting_report (id, alert_id, reporter_member_id, seen_at, description, approx_location, visibility)
VALUES
  ('00000000-0000-4000-8000-00000000b101', '00000000-0000-4000-8000-000000009101', '00000000-0000-4000-8000-000000000203', '2026-08-10T08:40:00+09:00', '성산초등학교 뒤 골목에서 비슷한 강아지를 보았다는 제보입니다.', ST_SetSRID(ST_MakePoint(126.905, 37.558), 4326)::geography, 'PUBLIC_APPROXIMATE'),
  ('00000000-0000-4000-8000-00000000b102', '00000000-0000-4000-8000-000000009102', '00000000-0000-4000-8000-000000000201', '2026-08-09T14:00:00+09:00', '서울숲 서쪽 출입구에서 잠시 쉬고 있는 고양이를 보았습니다.', ST_SetSRID(ST_MakePoint(127.038, 37.544), 4326)::geography, 'PUBLIC_APPROXIMATE');

INSERT INTO adoption_listing (id, publisher_member_id, neighborhood_id, title, description, species, breed, status, created_at, updated_at, version)
VALUES
  ('00000000-0000-4000-8000-00000000e201', '00000000-0000-4000-8000-000000000204', '00000000-0000-4000-8000-000000000101', '차분한 성격의 믹스견 가족을 찾습니다', '사람과 산책을 좋아하는 어린 믹스견입니다. 예방접종 상담 후 신중하게 입양을 진행합니다.', 'DOG', '믹스', 'OPEN', '2026-08-06T10:00:00+09:00', '2026-08-06T10:00:00+09:00', 0),
  ('00000000-0000-4000-8000-00000000e202', '00000000-0000-4000-8000-000000000204', '00000000-0000-4000-8000-000000000102', '사람을 좋아하는 치즈태비 고양이', '실내 생활에 적응한 고양이입니다. 현재 건강 상태와 생활 습관을 상담할 수 있습니다.', 'CAT', '치즈태비', 'OPEN', '2026-08-05T10:00:00+09:00', '2026-08-05T10:00:00+09:00', 0);

INSERT INTO gathering (id, host_member_id, title, description, location, starts_at, capacity, status, created_at, version)
VALUES
  ('00000000-0000-4000-8000-00000000d201', '00000000-0000-4000-8000-000000000201', '망원 한강 저녁 산책 데모', '천천히 걷고 반려생활 팁을 나누는 소규모 산책입니다.', '망원나들목 앞', '2026-08-20T10:00:00+09:00', 8, 'ACTIVE', '2026-08-08T10:00:00+09:00', 0),
  ('00000000-0000-4000-8000-00000000d202', '00000000-0000-4000-8000-000000000202', '초보 보호자 질문 모임 데모', '처음 반려동물과 사는 분들이 질문을 나누는 자리입니다.', '성수 커뮤니티룸', '2026-08-24T05:00:00+09:00', 12, 'ACTIVE', '2026-08-07T10:00:00+09:00', 0);

INSERT INTO gathering_participant (id, gathering_id, member_id, joined_at)
VALUES ('00000000-0000-4000-8000-00000000e101', '00000000-0000-4000-8000-00000000d201', '00000000-0000-4000-8000-000000000202', '2026-08-09T10:00:00+09:00');

INSERT INTO volunteer_opportunity (id, publisher_member_id, title, description, organization, location, starts_at, capacity, status, created_at, updated_at, version)
VALUES
  ('00000000-0000-4000-8000-00000000f201', '00000000-0000-4000-8000-000000000204', '보호소 산책 봉사 데모', '주말 오전 보호소 동물의 산책을 돕습니다.', '마포 반려동물 보호소', '서울 마포구', '2026-08-22T01:00:00+09:00', 10, 'OPEN', '2026-08-06T09:00:00+09:00', '2026-08-06T09:00:00+09:00', 0),
  ('00000000-0000-4000-8000-00000000f202', '00000000-0000-4000-8000-000000000204', '입양 상담 안내 봉사 데모', '입양 행사 방문자에게 기본 절차와 준비물을 안내합니다.', '성동 입양센터', '서울 성동구', '2026-08-29T02:00:00+09:00', 6, 'OPEN', '2026-08-05T09:00:00+09:00', '2026-08-05T09:00:00+09:00', 0);

INSERT INTO volunteer_application (id, opportunity_id, applicant_member_id, message, created_at)
VALUES ('00000000-0000-4000-8000-00000000f301', '00000000-0000-4000-8000-00000000f201', '00000000-0000-4000-8000-000000000203', '동물 산책 봉사 경험이 있어 일정에 맞춰 참여할 수 있습니다.', '2026-08-09T12:00:00+09:00');

INSERT INTO hospital_review (id, author_member_id, hospital_name, address, rating, body, created_at, updated_at, version)
VALUES
  ('00000000-0000-4000-8000-00000000f101', '00000000-0000-4000-8000-000000000201', '망원우리동물병원', '서울 마포구 월드컵로 10', 5, '예약 안내가 친절했고 진료 설명을 이해하기 쉽게 해 주셨어요.', '2026-08-04T08:00:00+09:00', '2026-08-04T08:00:00+09:00', 0),
  ('00000000-0000-4000-8000-00000000f102', '00000000-0000-4000-8000-000000000202', '성수24시동물병원', '서울 성동구 성수이로 20', 4, '야간에도 상담할 수 있어 안심이 됐습니다. 대기 시간은 확인이 필요해요.', '2026-08-03T08:00:00+09:00', '2026-08-03T08:00:00+09:00', 0);

INSERT INTO care_request (id, requester_member_id, title, description, location, starts_at, ends_at, reward_hint, status, created_at, updated_at, version)
VALUES
  ('00000000-0000-4000-8000-00000000f401', '00000000-0000-4000-8000-000000000201', '주말 고양이 돌봄 요청 데모', '주말 여행 동안 사료와 물을 확인해 주실 이웃을 찾습니다.', '서울 마포구 망원동', '2026-08-23T01:00:00+09:00', '2026-08-23T09:00:00+09:00', '사례 협의', 'OPEN', '2026-08-08T07:00:00+09:00', '2026-08-08T07:00:00+09:00', 0),
  ('00000000-0000-4000-8000-00000000f402', '00000000-0000-4000-8000-000000000202', '저녁 산책 동행 매칭 데모', '퇴근 전 반려견 저녁 산책을 도와주실 분과 매칭된 요청입니다.', '서울 성동구 성수동', '2026-08-21T09:00:00+09:00', '2026-08-21T10:00:00+09:00', '산책 후 간식 제공', 'MATCHED', '2026-08-07T07:00:00+09:00', '2026-08-07T07:00:00+09:00', 1);

INSERT INTO care_application (id, request_id, applicant_member_id, message, status, created_at, updated_at, version)
VALUES ('00000000-0000-4000-8000-00000000f501', '00000000-0000-4000-8000-00000000f401', '00000000-0000-4000-8000-000000000203', '반려동물 돌봄 경험이 있어 일정에 맞춰 방문할 수 있습니다.', 'PENDING', '2026-08-09T07:00:00+09:00', '2026-08-09T07:00:00+09:00', 0);

INSERT INTO care_assignment (id, request_id, caregiver_member_id, status, created_at, updated_at, version)
VALUES ('00000000-0000-4000-8000-00000000f601', '00000000-0000-4000-8000-00000000f402', '00000000-0000-4000-8000-000000000203', 'MATCHED', '2026-08-08T07:00:00+09:00', '2026-08-08T07:00:00+09:00', 0);

INSERT INTO care_feedback (id, assignment_id, author_member_id, body, created_at)
VALUES ('00000000-0000-4000-8000-00000000f701', '00000000-0000-4000-8000-00000000f601', '00000000-0000-4000-8000-000000000202', '약속한 시간에 잘 진행되고 있어요. 완료 후 후기를 남겨 보세요.', '2026-08-08T08:00:00+09:00');

COMMIT;
