# 로컬 커뮤니티 데모 데이터

이 문서는 동물 게시판과 공통 게시판의 화면 확인용 합성 데이터를 채우는 방법과, Docker 이미지나 PostgreSQL volume을 실수로 삭제했을 때 복구하는 방법을 기록한다.

## 구성

데모 데이터는 두 fixture로 나뉜다.

| fixture | 내용 | 재실행 |
| --- | --- | --- |
| `migration/fixtures/local-demo.sql` | 기본 계정, 기존 커뮤니티 글 2개, 공통 게시판 기능 확인용 기본 항목 | 안전 |
| `migration/fixtures/local-community-demo.sql` | 12개 동물 코드 × 4개 동물 게시판 유형 = 48개 글, 글마다 댓글 3개(대댓글 1개 포함), 공통 게시판별 추가 항목 3개 | 안전 |

동물 게시판 글에는 `FREE_BOARD`, `QA_QUESTION`, `PET_SHOWCASE`, `PRODUCT_REVIEW` 유형을 각각 하나씩 넣었다. 댓글은 실제 `engagement_comment`와 댓글 API가 사용하는 publication에 연결되어 있으므로, 동물 게시판의 목록·상세·댓글·대댓글 화면을 바로 확인할 수 있다.

공통 게시판의 입양·분실·거래·병원 후기·모임·돌봄·봉사는 현재 제품 구조상 각각 전용 테이블의 구조화된 항목이다. 따라서 이 fixture는 공통 게시판에 각 목록 항목을 추가한다. 현재 댓글 테이블은 publication만 참조하므로 전용 공통 게시판 항목에는 댓글을 저장하지 않는다. 이는 fixture 누락이 아니라 현재 API/스키마 경계이며, 공통 항목 댓글 기능은 별도의 제품·스키마 작업으로 다뤄야 한다.

## 채우는 순서

Docker Desktop과 PostgreSQL이 실행 중이고, 최신 backend image가 현재 소스에서 만들어진 상태인지 먼저 확인한다.

```bash
cd /Users/alex/project/townpet-springboot

docker compose -f deploy/compose/local.yml build backend
docker compose -f deploy/compose/local.yml up -d backend
docker compose -f deploy/compose/local.yml ps
```

backend가 `healthy`가 되고 Flyway가 완료된 뒤 다음 순서로 실행한다.

```bash
./scripts/seed-local-demo.sh
./scripts/seed-local-community-demo.sh
```

두 seed script는 서비스를 새로 시작하지 않는다. 이미 실행 중인 PostgreSQL의 준비 상태와 필요한 테이블만 확인한 뒤 SQL을 주입한다. PostgreSQL volume과 기존 사용자의 데이터를 지우지 않으며, 각 fixture가 소유한 고정 UUID 범위만 먼저 삭제하므로 재실행할 수 있다.

확인용 조회:

```bash
docker compose -f deploy/compose/local.yml exec -T postgres \
  psql -Atq -U townpet_app -d townpet -c "
    SELECT 'animal_posts', count(*) FROM publication
      WHERE id >= '00000000-0000-4000-8000-200000000001'::uuid
        AND id <= '00000000-0000-4000-8000-200000000048'::uuid
    UNION ALL
    SELECT 'animal_comments', count(*) FROM engagement_comment
      WHERE publication_id >= '00000000-0000-4000-8000-200000000001'::uuid
        AND publication_id <= '00000000-0000-4000-8000-200000000048'::uuid;
  "
```

기대값은 `animal_posts|48`, `animal_comments|144`이다. 공통 게시판은 기존 `local-demo.sql`의 항목과 이번 fixture의 추가 항목을 합쳐 화면에서 확인한다.

## 이미지가 삭제되었을 때

Docker image 삭제는 PostgreSQL volume 삭제와 별개다. 소스가 남아 있으면 다음 명령으로 backend image를 다시 만들고 기존 DB에 연결할 수 있다.

```bash
cd /Users/alex/project/townpet-springboot
docker compose -f deploy/compose/local.yml build backend
docker compose -f deploy/compose/local.yml up -d backend
```

이 과정은 `townpet-postgres-data` volume을 삭제하지 않는다. 로컬 데이터까지 보존해야 할 때는 `docker compose down -v`를 실행하지 않는다.

image와 PostgreSQL volume을 모두 삭제했거나 새 환경에서 시작한다면 다음 순서를 지킨다.

```bash
docker compose -f deploy/compose/local.yml up -d postgres
docker compose -f deploy/compose/local.yml build backend
docker compose -f deploy/compose/local.yml up -d backend

./scripts/seed-local-demo.sh
./scripts/seed-local-community-demo.sh
```

새 volume에서는 backend가 Flyway를 실행해 schema와 합성 demo 계정을 만든 뒤 seed해야 한다. `local-community-demo.sql`만 먼저 실행하면 계정·animal catalog·schema가 없어서 실패하도록 방어되어 있다.

## 주의사항

- `docker compose -f deploy/compose/local.yml down -v`는 PostgreSQL·MinIO volume을 삭제한다. 데이터 초기화가 목적일 때만 사용한다.
- fixture에는 실제 사용자 개인정보나 실제 legacy 데이터가 없다.
- fixture의 고정 UUID 범위와 공통 항목 UUID는 다른 로컬 테스트 데이터와 겹치지 않도록 별도로 배정했다.
- 이미지에 fixture가 복사되어 포함되는 방식이 아니다. fixture는 repository의 migration/scripts 파일이며, image를 다시 만들거나 image가 삭제되어도 소스에서 다시 실행할 수 있다.
