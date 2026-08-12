# 동물 커뮤니티 개편

## 목표

기존의 `게시판 종류 + 관심 동물 전역 필터`를 `동물 커뮤니티 + 커뮤니티 내부 게시판` 구조로 전환한다. 회원의 관심 동물은 콘텐츠를 숨기는 필터가 아니라 자주 방문할 커뮤니티의 빠른 이동 설정이다.

## 현재 구현 계약

- 화면: `/animals/:animalCode`와 `/animals/:animalCode/:boardCode`
- 게시판: `all`, `free`, `questions`, `adoption`, `lost-found`, `hospital-reviews`, `gatherings`, `marketplace`, `care`, `volunteer`, `showcase`, `product-reviews`
- 조회 API: `GET /api/v1/communities/{animalCode}/feed?board={boardCode}`
- 콘텐츠 분류: `content_animal_community(content_kind, content_id, animal_code)` 다중 분류 인덱스
- 기존 `/api/v1/feed`와 도메인 상세 URL은 호환 계층으로 유지

## 결정

- 동물별 화면은 하나의 parameterized React page로 구성한다.
- 도메인별 원본 데이터 소유권은 유지하고, catalog가 동물 커뮤니티 인덱스를 소유하며 discovery가 읽기 projection을 제공한다.
- 분류가 없는 콘텐츠는 개별 동물 커뮤니티에 자동 노출하지 않는다. `/animals/all`에서만 확인할 수 있다.
- 기존 `animal_interest_code`는 호환을 위해 유지하고, 새 콘텐츠는 `animalCommunityCodes`를 통해 다중 분류한다.
- 기존 구조화 도메인 생성·상태·권한 흐름은 재사용하고 생성 시 동물 context만 전달한다.

## 검증 증거

- `./gradlew spotlessApply test --tests 'com.townpet.common.web.GlobalProblemHttpTest' --tests 'com.townpet.publication.PublicationControllerTest' --max-workers=2` — 다중 태그·동물/게시판 경계·안정적인 400 계약 통과
- `(cd frontend && corepack pnpm typecheck)`
- `(cd frontend && corepack pnpm test)` — 10 files, 32 tests passed
- `(cd frontend && corepack pnpm build)` — Vite production build and bundle budget passed
- `./gradlew check migrationTest --max-workers=2` — `BUILD SUCCESSFUL in 6m 14s`; 기능·모듈 경계·query plan·Flyway 검증 통과
- 기존 `localhost:5173` 서버의 깨끗한 브라우저 탭에서 `/animals/dog/questions` shell, 12개 내부 게시판, 게시판 메뉴 선택 후 닫힘, 관심 동물 메뉴와 강아지 커뮤니티 이동을 확인했고 현재 console warn/error는 없었다.

서버를 새로 시작하지 말라는 운영 조건 때문에 `scripts/frontend-backend-smoke.sh`는 실행하지 않았다. 기존 Vite 프로세스는 유지했고, 현재 backend 프로세스가 없어 live 화면의 feed 요청은 연결 오류 상태다. API와 PostgreSQL 통합 동작은 위 MockMvc/Testcontainers gate로 검증했다.

## 구현 감사에서 보완한 경계

- 지원하지 않는 `animalCommunityCodes`는 500이 아니라 `VALIDATION_FAILED` 400 Problem Details로 응답한다. 프로젝트 전역 오류 처리기가 Spring 기본 처리기보다 먼저 적용되도록 우선순위도 고정했다.
- 게시글과 거래 수정 응답에 전체 동물 태그를 포함하고, 수정 화면이 다중 태그를 보존하거나 명시적으로 모두 해제할 수 있게 했다. 목록 응답의 태그 조회는 batch로 처리해 N+1을 피했다.
- 익명 `scope=LOCAL` 조회는 항상 빈 결과가 되도록 명시했고, 관심 동물을 모두 해제한 상태를 전체 선택으로 되돌리지 않는다.
- 헤더의 중복 `이웃 활동` 메뉴를 제거하고 게시판·관심 동물·프로필·알림으로 이동 경로를 정리했다. 게시판 드롭다운은 trigger와 panel 사이 포인터 이동 영역, 외부 클릭, route 이동, 방향키·Home·End·Escape를 지원한다.
- 커뮤니티 카드에서 익명 게시글을 운영팀 콘텐츠로 잘못 표시하던 문구를 `익명 이웃`으로 수정했다.
