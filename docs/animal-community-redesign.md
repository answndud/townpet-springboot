# 동물 커뮤니티 개편

## 목표

기존의 `게시판 종류 + 관심 동물 전역 필터`를 `동물 게시판`과 `공통게시판`의 두 축으로 전환한다. 회원의 관심 동물은 콘텐츠를 숨기는 필터가 아니라 자주 방문할 동물 게시판의 빠른 이동 설정이다. 입양·분실·병원 후기·모임·거래·돌봄·봉사는 특정 동물에 종속되지 않는 공통게시판이다.

## 현재 구현 계약

- 동물 게시판 화면: `/animals/:animalCode`와 `/animals/:animalCode/:boardCode`
- 동물 게시판: `all`, `free`, `questions`, `showcase`, `product-reviews`
- 공통게시판 화면: `/boards/:boardCode`
- 공통게시판: `all`, `adoption`, `lost-found`, `hospital-reviews`, `gatherings`, `marketplace`, `care`, `volunteer`
- 동물 게시판 조회 API: `GET /api/v1/communities/{animalCode}/feed?board={boardCode}`
- 공통게시판 조회 API: `GET /api/v1/boards/{boardCode}/feed`
- 동물 콘텐츠 분류: `content_animal_community(content_kind, content_id, animal_code)` 다중 분류 인덱스
- 기존 `/api/v1/feed`와 도메인 상세 URL은 호환 계층으로 유지

## 결정

- 동물별 화면은 하나의 parameterized React page로 구성한다.
- 헤더는 `동물 게시판`과 `공통게시판`을 별도 메뉴로 제공하며, 관심 동물 설정은 동물 게시판 빠른 이동만 관리한다.
- 도메인별 원본 데이터 소유권은 유지하고, catalog가 동물 커뮤니티 인덱스를 소유하며 discovery가 읽기 projection을 제공한다.
- 동물 게시판 콘텐츠는 `FREE_BOARD`, `QA_QUESTION`, `PET_SHOWCASE`, `PRODUCT_REVIEW`만 허용한다.
- 공통게시판 콘텐츠는 `ADOPTION`, `LOST_FOUND`, `HOSPITAL_REVIEW`, `GATHERING`, `MARKETPLACE`, `CARE_REQUEST`, `VOLUNTEER`로만 조회하며 동물 분류를 사용하지 않는다.
- 기존 `animal_interest_code`는 호환을 위해 유지하고, 새 콘텐츠는 `animalCommunityCodes`를 통해 다중 분류한다.
- 기존 구조화 도메인 생성·상태·권한 흐름은 재사용하되 공통 도메인 생성 요청에서 동물 context를 제거했다.
- 전환 기간에 생성된 공통 도메인 동물 태그는 `V060__detach_common_board_content_from_animals.sql`에서 정리한다. 이후 공통 서비스는 `AnimalCommunityTagger`를 주입받지 않는다.

## 검증 증거

- `./gradlew spotlessApply test --tests 'com.townpet.common.web.GlobalProblemHttpTest' --tests 'com.townpet.publication.PublicationControllerTest' --max-workers=2` — 다중 태그·동물/게시판 경계·안정적인 400 계약 통과
- `(cd frontend && corepack pnpm typecheck)`
- `(cd frontend && corepack pnpm test)` — 10 files, 33 tests passed
- `(cd frontend && corepack pnpm build)` — Vite production build and bundle budget passed
- `./gradlew check migrationTest --max-workers=2` — `BUILD SUCCESSFUL in 6m 14s`; 기능·모듈 경계·query plan·Flyway 검증 통과
- 기존 `localhost:5173` 서버의 깨끗한 브라우저 탭에서 `/animals/dog/questions` 동물 게시판, `/boards/marketplace` 공통게시판과 헤더 메뉴를 확인했다. 로컬 서버는 재시작하지 않았다.

서버를 새로 시작하지 말라는 운영 조건 때문에 `scripts/frontend-backend-smoke.sh`는 실행하지 않았다. 기존 Vite 프로세스는 유지했고, 현재 backend 프로세스가 없어 live 화면의 feed 요청은 연결 오류 상태다. API와 PostgreSQL 통합 동작은 위 MockMvc/Testcontainers gate로 검증했다.

## 구현 감사에서 보완한 경계

- 지원하지 않는 `animalCommunityCodes`는 500이 아니라 `VALIDATION_FAILED` 400 Problem Details로 응답한다. 프로젝트 전역 오류 처리기가 Spring 기본 처리기보다 먼저 적용되도록 우선순위도 고정했다.
- 게시글 수정 응답에는 동물 게시판 분류를 유지하고, 공통 도메인 응답에는 동물 분류를 노출하지 않는다.
- 익명 `scope=LOCAL` 조회는 항상 빈 결과가 되도록 명시했고, 관심 동물을 모두 해제한 상태를 전체 선택으로 되돌리지 않는다.
- 헤더의 중복 `이웃 활동` 메뉴를 제거하고 게시판·관심 동물·프로필·알림으로 이동 경로를 정리했다. 게시판 드롭다운은 trigger와 panel 사이 포인터 이동 영역, 외부 클릭, route 이동, 방향키·Home·End·Escape를 지원한다.
- 커뮤니티 카드에서 익명 게시글을 운영팀 콘텐츠로 잘못 표시하던 문구를 `익명 이웃`으로 수정했다.
