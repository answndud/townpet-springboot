# TownPet shell·관심 동물 구현 계획

> 이 문서는 기존 `PLAN.md`와 독립된 기능 계획이다. 프로젝트의 전역 실행 순서나 성능 계획을 변경하지 않는다.

## 현재 상태

구현 완료: 전역 shell·게시판 메뉴·관심 동물 저장/필터·공개 통합 feed projection·합성 fixture·대표 Chromium parity 검증.
루트 `PLAN.md`는 이 작업에서 변경하지 않는다.

## 목표

원본 TownPet처럼 `/`·`/feed/guest`·게시물 상세·도메인 목록·프로필 등 어느 화면에서도 같은 헤더 구조를 보여준다. 비회원·회원·운영자 모두 `게시판`, `관심 동물`, `내 프로필`, `로그인`의 의미 있는 이동을 사용할 수 있고, 관심 동물 선택은 저장되어 공개 feed 필터에 반영된다.

## 1. 전역 shell과 게시판 메뉴 — 완료

대상 파일:

- `frontend/src/App.tsx`
- `frontend/src/styles.css`
- `frontend/src/App.test.tsx`
- `frontend/e2e/parity-shell.spec.ts`
- `frontend/e2e/FeedFlows` 관련 테스트

구현 내용:

- route별로 헤더가 달라지지 않도록 공통 헤더 계약을 고정한다.
- 비회원도 `게시판`, `관심 동물`, `내 프로필`, `로그인`을 본다.
- `내 프로필`은 비회원이면 로그인 후 원래 URL로 돌아가고, 회원이면 `/profile`로 이동한다.
- 운영자 전용 `운영 콘솔`은 유지하되 공개 메뉴를 제거하지 않는다.
- 게시판 메뉴에 원본의 전체 유형을 연결한다.

  - 전체
  - 자유게시판
  - 질문/답변
  - 병원 후기
  - 실종/목격 제보
  - 동네 모임
  - 중고·공동구매
  - 돌봄 요청
  - 유기동물 입양
  - 보호소 봉사 모집
  - 용품 후기
  - 반려동물 자랑

- 메뉴의 hover, click, Escape, 바깥 클릭, 키보드 이동과 모바일 breakpoint를 회귀 테스트한다.

완료 조건:

- `/`, `/feed/guest`, `/posts/:id`, `/marketplace`, `/profile`에서 같은 핵심 헤더가 보인다.
- 인증 상태와 무관하게 공개 이동이 사라지지 않는다.
- 메뉴 링크와 query filter가 새로고침 후에도 유지된다.

## 2. 관심 동물 backend vertical slice — 완료

대상 파일:

- `src/main/resources/db/migration/V055__member_animal_interest.sql`
- `src/main/java/com/townpet/catalog/`
- `src/main/java/com/townpet/member/`
- `src/test/java/com/townpet/identity/IdentityMemberControllerTest.java`

구현 내용:

- catalog에 원본의 그룹·코드·라벨·정렬 순서를 추가한다.

  - 강아지, 고양이
  - 앵무새, 조류
  - 거북, 도마뱀, 뱀, 양서류, 파충류
  - 소동물
  - 어류·수조
  - 절지류·곤충

- member 소유 join table에 선택된 관심 동물 code를 저장한다.
- FK, unique, 유효 code 검증을 DB와 application 양쪽에 둔다.
- 다음 API를 추가한다.

  - `GET /api/v1/catalog/animal-interests`
  - `GET /api/v1/members/me/preferences/animal-interests`
  - `PUT /api/v1/members/me/preferences/animal-interests`

- 전체 선택 교체 방식으로 멱등성을 보장한다.
- 빈 선택, 알 수 없는 code, 타인 member 접근, 권한 없는 변경을 테스트한다.

완료 조건:

- 회원이 자신의 관심 동물만 조회·저장할 수 있다.
- 동일 요청을 반복해도 중복 선택이 생기지 않는다.
- migration 재실행과 새 local volume에서 catalog가 결정적으로 생성된다.

## 3. 관심 동물 header panel — 완료

대상 파일:

- `frontend/src/features/member/AnimalInterestMenu.tsx`
- `frontend/src/api/client.ts`
- `frontend/src/App.tsx`
- `frontend/src/App.test.tsx`
- `frontend/src/FeedFlows.test.tsx`

구현 내용:

- 원본처럼 그룹별 checkbox를 제공한다.
- `전체 선택`, `전체 해제`, `저장`, 저장 중, 저장 성공, 저장 실패 상태를 제공한다.
- 비회원은 scoped local storage를 사용하고, 회원은 Spring API를 source of truth로 사용한다.
- 로그인 전 비회원 선택을 로그인 후 회원 설정과 어떻게 합칠지 명시하고 테스트한다.
- 저장된 선택을 feed 요청의 동물 filter 계약에 연결한다.
- 특정 동물로 분류되지 않은 일반 글은 정책상 기본 노출 여부를 명확히 한다.

완료 조건:

- 어느 공개 route에서도 `관심 동물` panel을 열 수 있다.
- 선택·저장·새로고침 후 상태가 유지된다.
- 저장된 관심값이 feed API 요청과 화면 결과에 반영된다.

## 4. 공개 feed의 TownPet 유형 parity — 완료

대상 파일:

- `src/main/java/com/townpet/discovery/`
- `src/main/java/com/townpet/publication/api/PublicationFeed.java`
- `frontend/src/api/client.ts`
- `frontend/src/features/publication/PublicationFeedPage.tsx`
- 관련 controller·frontend flow tests

구현 내용:

- `FREE_BOARD` 하나로 고정된 feed 응답을 discriminated feed item으로 확장한다.
- discovery read projection에서 publication과 marketplace, lostfound, welfare, localguide, gathering, care, hospital review, volunteer의 공개 필드를 조합한다.
- module 간 JPA entity association은 만들지 않고 read projection으로 연결한다.
- 유형별 label, summary, 동물 정보, 상태, 작성자/profile link를 반환한다.
- cursor, 게시 유형 filter, 관심 동물 filter를 함께 지원한다.

완료 조건:

- 원본처럼 여러 게시 유형이 공개 feed에 섞여 보인다.
- 모든 항목이 `자유게시판`으로 표시되지 않는다.
- 관심 동물과 게시 유형 filter가 URL·API·화면에서 일관된다.

## 5. fixture와 검증 — 진행 완료

대상 파일:

- `migration/fixtures/local-demo.sql`
- `frontend/e2e/parity-shell.spec.ts`
- `frontend/e2e/publication-parity.spec.ts`
- `docs/parity/matrix.yaml`
- `docs/parity/shell.md`

검증 순서:

```bash
./gradlew test --tests '*IdentityMemberControllerTest'
./gradlew migrationTest
(cd frontend && corepack pnpm test -- App.test.tsx FeedFlows.test.tsx)
(cd frontend && corepack pnpm typecheck)
(cd frontend && corepack pnpm build)
./scripts/frontend-backend-smoke.sh
(cd frontend && corepack pnpm test:e2e -- parity-shell.spec.ts publication-parity.spec.ts)
```

fixture에는 각 게시 유형, 관심 동물 선택, 빈 결과, cursor 다음 페이지를 재현할 합성 데이터만 추가한다. 실제 Legacy 개인정보는 사용하지 않는다. 통합 fixture는 실제 local PostgreSQL에 재주입했고, 공개 feed의 유형별 상세 링크를 Chromium 한 worker로 확인했다.
