CREATE TABLE local_resource (
    id UUID NOT NULL,
    kind VARCHAR(20) NOT NULL,
    title VARCHAR(160) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    content VARCHAR(10000) NOT NULL,
    source_name VARCHAR(120) NOT NULL,
    source_url VARCHAR(500),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT local_resource_pk PRIMARY KEY (id),
    CONSTRAINT local_resource_kind_ck CHECK (kind IN ('LOCAL_GUIDE', 'WELFARE', 'CARE'))
);

CREATE INDEX local_resource_kind_updated_ix ON local_resource (kind, updated_at DESC, id DESC);

INSERT INTO local_resource (id, kind, title, summary, content, source_name, source_url, updated_at) VALUES
('0198f342-13d7-7000-8000-000000000101', 'LOCAL_GUIDE', '망원 한강 산책 코스', '반려견과 걷기 좋은 3.2km 강변 코스입니다.', '망원나들목에서 출발해 한강공원 그늘 쉼터까지 이어지는 평지 코스입니다. 저녁 시간에는 자전거 이용자와 충분한 거리를 두고 걸어 주세요.', 'TownPet 운영팀', 'https://townpet.example/guides/mangwon-river', '2026-08-01T00:00:00Z'),
('0198f342-13d7-7000-8000-000000000102', 'LOCAL_GUIDE', '성수 반려동물 동반 카페 거리', '실내 동반 가능한 매장과 주차 정보를 모았습니다.', '방문 전 동반 정책과 예방접종 증빙 요구 여부를 확인하고, 혼잡 시간에는 이동가방을 준비하는 것을 권장합니다.', 'TownPet 운영팀', 'https://townpet.example/guides/seongsu-cafe', '2026-08-03T00:00:00Z'),
('0198f342-13d7-7000-8000-000000000201', 'WELFARE', '서울시 동물등록 지원 안내', '동물등록과 유실 예방 지원 제도를 한눈에 확인하세요.', '등록 대상과 신청 방법은 거주지 관할 구청 공고를 확인하세요. 지원 조건과 접수 기간은 변경될 수 있습니다.', '서울특별시', 'https://animal.seoul.go.kr', '2026-07-28T00:00:00Z'),
('0198f342-13d7-7000-8000-000000000301', 'CARE', '여름철 산책 전 체크리스트', '아스팔트 온도와 수분 보충을 먼저 확인하세요.', '손등으로 바닥 열기를 확인하고, 한낮 산책을 피하며, 짧은 산책 뒤 그늘에서 물을 주세요. 무기력·과호흡이 지속되면 즉시 병원에 문의하세요.', 'TownPet 케어 가이드', 'https://townpet.example/care/summer-walk', '2026-08-05T00:00:00Z'),
('0198f342-13d7-7000-8000-000000000302', 'CARE', '새 가족을 맞이한 첫 주', '식사·수면·배변 기록으로 환경 적응을 살펴보세요.', '처음부터 많은 자극을 주기보다 일정한 생활 리듬을 만들고, 이상 증상은 기록해 수의사 상담 때 활용하세요.', 'TownPet 케어 가이드', 'https://townpet.example/care/first-week', '2026-07-30T00:00:00Z');
