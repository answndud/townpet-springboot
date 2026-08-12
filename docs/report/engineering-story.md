# Engineering Story

TownPet을 단순히 Spring Boot 문법으로 옮기는 대신, 기존 49개 page·55개 API의 사용자 경험을 유지하면서 backend write ownership을 Java로 이전하고 있다. 아래에는 구현 순서 전체가 아니라 설계가 실제로 달라진 순간만 남긴다.

## 1. 기능보다 재현 가능한 실행 경계를 먼저 만들었다

빈 저장소에서 Java 25 toolchain, Gradle Wrapper, Spring Boot·Modulith, 정적 분석과 test task를 먼저 구성했다. 이는 성능 문제를 겪고 도입한 것이 아니라 팀이 없어도 local과 CI의 출발점을 같게 만들기 위한 사전 결정이었다.

실행 과정에서는 Error Prone Kotlin DSL 오용, Testcontainers module version 불일치, Modulith `EventSerializer` 누락, Java 25와 formatter 비호환을 실제 build 실패로 확인했다. dependency를 무작정 낮추지 않고 각 책임에 맞는 plugin 설정·Jackson event module·formatter 버전으로 수정했다.

- 근거: `85c7725`, `./gradlew clean check`
- 배운 점: 사전 설계와 구현 중 발견한 문제를 구분해야 과장 없는 기술 서사가 된다.

## 2. Database schema owner를 Flyway 하나로 고정했다

초기에는 Modulith 자동 초기화가 `event_publication`을 먼저 생성해, Flyway가 history 없는 non-empty schema를 거부했다. 동시에 application role은 PostGIS extension을 만들 권한이 없어 startup이 실패했다.

extension 설치는 bootstrap admin, versioned table 변경은 Flyway, runtime DML은 application role로 분리했다. Hibernate는 운영 schema 생성자가 아니라 mapping 검증자로 제한했다. H2 context test와 실제 PostgreSQL Testcontainers migration test의 의미도 분리했다.

- 근거: `16e54aa`, Flyway V001~V003, `DatabaseBaselineTest`
- trade-off: local 구성 요소는 늘었지만 schema drift와 과도한 DB 권한을 build 단계에서 드러낼 수 있다.

## 3. 17개 모듈을 만들되 물리적 분산은 하지 않았다

기능이 쌓이기 전에 package를 write ownership 기준으로 나누고 Spring Modulith와 ArchUnit으로 cycle, entity·repository·web DTO 누출을 검사했다. Gradle multi-project나 microservice로 분리하지 않아 혼자 개발하는 비용을 억제했다.

초기에는 OpenAPI 생성 경계를 검토했지만, 실제 frontend는 작은 수동 API client를 사용하고 생성 transport가 기능 개발 속도에 기여하지 않는다는 점을 확인했다. 별도 계약 파일과 generator를 제거하고 controller·request/response DTO를 HTTP 계약의 직접 근거로 남겼다.

- 근거: `a2d5096`, `4fd3776`, module/contract tests
- 되돌릴 조건: 독립 배포·팀 소유권·실측 부하가 생길 때만 물리 분리를 검토한다.

## 4. UI를 다시 디자인하지 않고 parity 측정 기반을 만들었다

Next.js server runtime을 제거하면서 React 19·Vite·React Router로 logo, header, palette, CTA와 responsive shell을 먼저 옮겼다. 49개 page와 55개 API를 기억으로 추적하지 않도록 inventory를 고정하고 UUID·timestamp·signed URL만 allowlist 방식으로 정규화한다.

backend, frontend, integration smoke, browser E2E를 계층으로 나눴다. smoke profile에서 H2가 runtime classpath에 없어 실패한 뒤 `developmentOnly`로 옮겨 production artifact와 test 편의를 분리했다.

- 근거: `a6c0607`, `8b9c910`, `d69dd81`, parity matrix와 CI workflow
- 배운 점: “새 화면이 열린다”보다 누락·환경 차이·의미 차이를 자동으로 찾는 기준이 먼저다.

## 5. 인증은 작은 사용자 여정으로 연결한 뒤 hardening했다

첫 slice에서 BCrypt credential, Spring Session JDBC, Cookie CSRF, 현재 회원·동네·프로필과 React login을 연결했다. 테스트에서 session 생성 전 `changeSessionId()` 호출 오류와 비인증 요청의 403/401 불일치를 발견해 실제 request lifecycle에 맞게 수정했다.

다음 단계에서 profile과 반려동물 목록을 한 transaction으로 저장하고 logout 뒤 같은 session이 401이 되는지 검증했다. CSRF cookie가 test 실행 순서에 따라 누락되는 현상은 token endpoint가 cookie 계약을 명시적으로 응답하도록 고쳤다. 공개 showcase는 실제 signup 대신 합성 MEMBER 3개와 MODERATOR 1개를 hash-only migration으로 만들고 운영 prefix를 role로 제한했다.

비밀번호 reset에서 전체 session revoke를 구현하며 더 근본적인 누락을 발견했다. `spring-session-jdbc` library와 session table만 있었고 Boot 4의 JDBC session auto-configuration starter가 없어 기존 테스트는 servlet memory session을 사용하고 있었다. 의존성을 `spring-boot-starter-session-jdbc`로 바꾸고 테스트도 `MockHttpSession` 직접 전달 대신 실제 `SESSION` cookie와 JDBC repository를 확인하도록 수정했다. Reset token은 SHA-256 hash·1시간 만료·1회 사용·optimistic version으로 저장하고, 성공 시 password 변경·audit·모든 JDBC session 삭제를 한 transaction 경계에서 수행한다.

Credentials 화면을 실제 PostgreSQL·Spring·Vite browser test로 연결하자 H2에서는 보이지 않던 `CHAR(64)` token hash와 JPA `VARCHAR(64)`, PostgreSQL `citext`와 기본 String mapping의 schema validation 차이가 차례로 드러났다. 적용된 migration을 수정하지 않고 V006에서 hash column을 정렬하고 길이 constraint를 유지했으며, email은 JPA mapping에 `citext`를 명시해 case-insensitive unique 의미를 보존했다. E2E는 운영과 분리된 임시 DB와 합성 계정을 사용하고 종료 시 volume을 제거하며, desktop·mobile 여정 뒤 JDBC session·auth audit row까지 대사한다.

- 근거: `482428d`, `c1f6155`, `461d4ad`, Flyway V004~V006, `IdentityMemberControllerTest`, `auth-browser-e2e.sh`
- 현재 한계: 실제 email provider와 transaction 이후 durable delivery·retry·bounce 처리는 아직 구현하지 않았다.

## 6. 역할 검증 누락을 실제 계정 조합으로 닫았다

권한 감사를 진행하면서 URL matcher로 보호된 관리자 API와 controller의 `@PreAuthorize`에 의존한 API가 서로 다른 결과를 내는 것을 발견했다. Method Security가 활성화되지 않아 MEMBER가 정책·운영 로그·personalization 같은 관리자 surface에 접근할 수 있었고, `viewer-shell`은 MODERATOR도 MEMBER로 표현했다.

Spring Method Security를 활성화하고 `/api/admin/**`, 신고 compatibility alias와 운영 summary의 경계를 정리했다. 일반 회원 쓰기 기능에는 `MEMBER` 전용 meta-annotation을 적용해 MODERATOR가 게시·댓글·관계·거래·돌봄·미디어 같은 일반 사용자 mutation을 실행하지 못하게 했다. 레거시 공개 프로필도 공개 반려동물 설정을 동일하게 적용하고 React Router에는 moderator route guard를 추가했다.

추가 점검에서 guest cookie가 있으면 인증된 MODERATOR도 GuestPrincipal 작성·댓글·step-up을 호출할 수 있는 우회 경로와, 공개 상세 화면에 회원 전용 버튼이 보이는 문제를 발견했다. guest write는 익명 또는 MEMBER만 허용하고, React member route와 상세 action은 MODERATOR에게 읽기 전용으로 렌더링하도록 분리했다.

이 수정은 코드 검색만으로 완료 처리하지 않고 MEMBER·MODERATOR demo session을 실제 Docker PostgreSQL backend에 로그인시켜 관리자 API가 각각 `403`·`200`인지, 일반 mutation이 MODERATOR에게 `403`인지 확인했다. `IdentityMemberControllerTest`, `ModularityTest`, frontend Vitest가 이 경계를 회귀 검증한다.

- 근거: `SecurityConfig`, `MemberOnly`, `ViewerShellController`, `IdentityMemberControllerTest`, `frontend/src/App.tsx`
- trade-off: MODERATOR는 운영 검토와 공개 읽기만 수행하며 일반 회원 콘텐츠를 만들지 않는다. 운영자가 테스트용 콘텐츠를 만들어야 한다면 별도 MEMBER demo 계정을 사용한다.

## 면접에서 강조할 핵심

“처음부터 모든 기술을 넣었다”가 아니라, 사전 제약으로 정한 선택과 실제 실패 후 수정한 선택을 구분한다. 각 답변은 상황, 선택한 경계, 재현 가능한 test, 남은 trade-off 순서로 말한다. 아직 측정하지 않은 성능이나 구현하지 않은 기능은 성과로 주장하지 않는다.

## 7. release-candidate 재감사에서 경쟁 조건을 저장소까지 내렸다

첫 release-candidate gate가 통과한 뒤에도 목록·상태·metric 코드를 다시 읽어 실제 경합을 가정했다. 조회수는 `find → save` 구조에서 첫 두 요청이 동시에 insert할 수 있었고, volunteer 신청은 `FULL` 상태를 응답에만 표현할 뿐 capacity를 원자적으로 제한하지 않았다. 병원 신고도 애플리케이션의 중복 조회만으로는 병렬 요청을 막을 수 없었다.

조회수는 PostgreSQL `INSERT ... ON CONFLICT DO UPDATE ... RETURNING`으로 바꾸고, volunteer 신청은 대상 opportunity row lock 아래에서 count·insert·`FULL` 전이를 수행했다. 병원 신고에는 open case partial unique index를 추가하고 충돌을 `409`로 수렴시켰다. 미디어 첨부는 publication row lock을 사용해 동시에 다섯 개 제한을 넘지 않도록 했고, 돌봄 수락은 request lock과 application version 증가를 함께 적용했다.

이 변경은 단순 unit test가 아니라 Testcontainers PostgreSQL의 `EXPLAIN (ANALYZE, BUFFERS)`와 160개 병렬 metric upsert fixture로 검증했다. Docker 재기동 중에는 호스트의 Docker build cache가 PostgreSQL migration을 막는 `No space left on device`를 실제로 재현했고, 프로젝트 volume을 삭제하지 않고 build cache만 정리한 뒤 Flyway `V052`, health/readiness, seed 2회 반복을 다시 확인했다.

- 근거: `c1f5c68`, `V052__moderator_case_open_flag_uniqueness.sql`, `ReleaseCandidateQueryPlanTest`, Docker compose health/seed output
- trade-off: row/advisory lock과 partial index는 단일 VPS 규모에서 충분한 일관성을 제공하지만, 실제 운영 부하·replica·queue 요구가 생기면 별도 부하 측정과 확장 설계가 필요하다.
