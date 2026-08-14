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

## 8. 마지막 백엔드 감사에서 경계와 대량 처리의 숨은 결함을 닫았다

기존 통합 테스트가 정상 사용자 흐름은 확인했지만, Spring Security filter에서 바로 끝나는 401·403 응답은 MVC의 ProblemDetail 규칙을 거치지 않는다는 점을 발견했다. 인증 entry point와 access denied handler를 identity 모듈 안에 두어 기계 판독 code와 trace id를 같은 응답으로 반환하게 했다. moderator가 자기 계정을 제재할 수 있는 운영 경계와 빈 reason·무제한 bulk ID 같은 입력 경계도 함께 닫았다.

작성자 콘텐츠 lifecycle 변경은 모든 게시글을 JPA entity로 읽어 변경·저장하고 있었다. 게시글 수가 늘면 메모리와 flush 비용이 선형으로 커지고, 응답의 affected 수에도 이미 같은 상태인 row가 섞였다. 조건부 PostgreSQL bulk update로 ACTIVE↔HIDDEN row만 version과 updatedAt을 함께 갱신하고, 작성자/lifecycle index를 추가했다. 이때 entity·repository를 다른 모듈에 노출하지 않도록 Modulith named boundary를 유지했다.

마지막으로 request trace를 response header에만 남기면 장애 시 서버 로그와 연결되지 않는 문제가 있어, query string을 제외한 method/path/status/duration을 MDC trace id와 함께 기록했다. graceful shutdown timeout도 명시해 종료가 무한정 대기하지 않게 했다. 이 세 변경은 작은 파일별 작업이 아니라 보안·데이터 효율·운영 진단이라는 세 개의 감사 사이클로 묶었고, 최종 판단은 fresh backend/Docker gate 이후로 유보한다.

- 근거: `6366d41`, `dab5527`, `7de4416`, `StableSecurityProblemHandlers`, `PublicationRepository`, `RequestTraceFilter`, 관련 architecture·identity·publication test
- trade-off: bulk update는 대량 moderation에 효율적이지만 JPA entity lifecycle callback이 필요한 규칙에는 적용하지 않는다. 요청 로그는 진단 가능성을 높이는 대신 path 식별자가 포함될 수 있어 query와 민감 body는 기록하지 않는다.

## 9. 성능 개선은 인프라 추가보다 측정 루프로 닫았다

기존에 적용된 feed keyset·복합 인덱스·atomic view upsert·capacity row lock·목록 상한을 HTTP 부하로 다시 표현할 수 있도록 전용 perf DB, deterministic fixture, k6 시나리오를 만들었다. 먼저 100,000건 feed에서 인덱스 전후를 같은 조건으로 재생해 p95 67.13ms→5.01ms를 확인했고, 그 뒤 write·moderator·media·mixed·contention·30분 soak을 실행했다.

실행 중에는 k6 세션 cookie 누락으로 반복 write가 403이 되고, moderator fixture hash 불일치로 401이 되는 문제를 발견했다. 결과를 숨기지 않고 하네스와 seed를 수정한 뒤 유효한 run만 채택했다. capacity 경합은 10개 정원 불변식을 지켰지만 p95 약 1.19초의 row-lock 비용이 드러났고, 20 VU spike에서는 Docker host bridge 단일 timeout을 애플리케이션 오류와 분리했다. soak 종료 heap은 약 98MB였지만 RSS가 55→276MB까지 변해 native memory를 운영 환경에서 재측정할 후속 과제로 남겼다.

현재 public feed p95와 mixed/soak 결과에서 Redis cache 또는 Kafka broker가 해결할 병목은 입증되지 않았다. 따라서 두 인프라는 추가하지 않고 deferred로 결정했으며, 실제 DB CPU·queue backlog·eventual-consistency 요구가 생길 때 동일 fixture의 candidate-enabled 비교를 시작한다.

- 근거: [`docs/performance/results/2026-08-12-public-feed-index.md`](../performance/results/2026-08-12-public-feed-index.md), [`docs/performance/results/2026-08-12-s0-s2-baseline.md`](../performance/results/2026-08-12-s0-s2-baseline.md), [`docs/performance/results/2026-08-12-s3-s8-workloads.md`](../performance/results/2026-08-12-s3-s8-workloads.md), [`docs/performance/results/2026-08-13-phase2-capacity-diagnostics.md`](../performance/results/2026-08-13-phase2-capacity-diagnostics.md), [`docs/performance/results/2026-08-13-phase3-capacity-query-shape.md`](../performance/results/2026-08-13-phase3-capacity-query-shape.md), [`docs/performance/results/2026-08-13-phase4-feed-redis-evaluation.md`](../performance/results/2026-08-13-phase4-feed-redis-evaluation.md)
- trade-off: local Docker bridge와 JVM RSS는 운영 네트워크·메모리의 대체 증거가 아니므로 배포 전 VPS에서 spike/soak을 재실행한다.

## 10. 기능을 CRUD가 아니라 상태와 원장 중심으로 확장했다

도메인 기능을 추가할 때 모든 기능을 공통 `Content` 모델이나 기술별 service로 합치지 않았다. Publication은 lifecycle과 visibility를 소유하고, Comment·Reaction·Bookmark는 서로 다른 원장과 멱등성 규칙을 가진다. Lost & Found는 공개 근사 위치와 보호된 정확 위치를 분리하고, Care는 Request → Application → Assignment → Feedback의 상태 전이를 분리했다. Marketplace와 Volunteer도 결제나 단순 게시글로 축소하지 않고 상태·capacity·ownership 규칙을 각 module이 소유하게 했다.

이 선택으로 화면마다 임의의 상태 문자열과 권한 조건을 복제하지 않고, 각 변경이 어느 aggregate와 DB constraint를 통과해야 하는지 설명할 수 있게 됐다. 결제는 여전히 범위 밖이지만, 이메일 전송과 production object storage는 이제 배포 전 구현 대상으로 재분류했다.

- 근거: `src/main/java/com/townpet/{publication,engagement,lostfound,care,marketplace,welfare}/`, V007~V049, `docs/PRD.md`, `docs/TRD.md`
- trade-off: module과 상태 모델이 단순 CRUD보다 복잡하지만, 권한·동시성·복구 규칙을 한 곳에 두고 테스트할 수 있다.

## 13. 배포 전 필수 기능을 기술 유행과 분리해 재분류했다

production profile을 다시 읽으며 `UnavailableAccountTokenDelivery`와 `UnavailableObjectStorage`가 단순한 deferred 기능이 아니라 공개 환경에서 실제 요청을 503으로 끝낼 수 있는 경계임을 확인했다. SMTP는 공개 signup을 켜기 위한 것이 아니라 이메일 인증·비밀번호 복구라는 기본 계정 기능을 완성하기 위해 Spring Mail과 PostgreSQL event publication으로 도입한다. event payload에는 raw token을 넣지 않고 AES-GCM으로 암호화한 값만 저장하며, local/test는 동기 capture listener로 기존 검증 속성을 유지하고 production은 commit 이후 재시도 listener를 사용한다.

반대로 Redis·Kafka는 성능 결과에서 해결할 병목이 입증되지 않아 계속 보류한다. public demo 계정도 showcase를 풍부하게 보이게 한다는 이유만으로 유지하지 않고, migration에 들어간 synthetic row가 web 노출 전에 남지 않도록 guarded sanitize와 private moderator bootstrap을 배치 전제로 추가했다. 이 재분류로 “기술을 많이 사용했다”가 아니라 “production에서 실패할 기능은 먼저 완성하고 운영 복잡도는 증거가 있을 때만 늘린다”는 설명이 가능해졌다.

- 근거: `PLAN.md`, `ADR.md`의 ADR-0013·0014·0022·0023·0029·0030, `scripts/sanitize-production-demo.sh`, `scripts/bootstrap-private-moderator.sh`, `AccountTokenCipher`, `AccountTokenDeliveryListener`
- 검증: identity controller 23개 테스트와 unavailable delivery rollback 테스트, `./gradlew compileJava spotlessApply`

## 11. 운영 가능한 오류와 대량 처리 경계를 별도 감사했다

정상 흐름이 통과한 뒤에도 운영 실패를 별도로 점검했다. Spring Security filter에서 끝나는 401·403이 MVC ProblemDetail과 다른 형식으로 반환되던 문제는 entry point와 access denied handler를 추가해 trace ID와 machine-readable code를 통일했다. 작성자 콘텐츠 lifecycle 일괄 변경은 entity 전체 로딩 대신 조건부 PostgreSQL bulk update로 바꾸고, 실제로 변경된 row만 version과 `updatedAt`을 갱신했다. 요청 로그에는 query string과 민감 body를 넣지 않으면서 MDC trace ID·method·path·status·duration을 남겼고, graceful shutdown timeout도 설정했다.

이 사건들은 기능 하나를 추가한 기록이 아니라, 정상 요청만 확인하면 놓치는 보안·메모리·장애 진단 문제를 production 경계에서 다시 본 사례다. bulk update는 JPA lifecycle callback이 필요한 규칙에는 사용하지 않는다는 제한도 함께 남겼다.

- 근거: `StableSecurityProblemHandlers`, `PublicationRepository`, `RequestTraceFilter`, `application.yml`, `docs/performance/results/2026-08-12-s3-s8-workloads.md`
- 검증: backend security/integration tests, bulk update 관련 테스트, `./scripts/frontend-backend-smoke.sh`, 성능 결과 문서

## 12. 면접 답변은 사건·근거·한계로 압축한다

이 프로젝트의 핵심 서사는 “Spring 기술을 처음부터 모두 넣었다”가 아니다. Legacy parity를 유지해야 하는 상황에서 실행 경계와 module ownership을 먼저 고정하고, 인증·권한·동시성·대량 처리에서 실제 실패를 재현한 뒤 경계를 코드와 DB로 내렸다. 성능은 query/index/transaction을 먼저 측정하고, Redis·Kafka는 병목과 consistency 비용이 입증되지 않아 보류했다.

각 사건은 30초 답변에서는 상황과 결과만, 2분 답변에서는 선택과 검증까지, deep-dive에서는 trade-off와 재현 명령까지 확장한다. 아직 VPS, production object storage, 외부 email provider를 검증하지 않았다는 한계를 먼저 밝히는 것이 이 프로젝트의 신뢰성을 높인다.

## 14. 목록 탐색은 offset이 아니라 cursor 계약으로 통일했다

기존 feed는 keyset cursor를 사용하고 있었지만, 화면마다 `더 보기`와 누적 목록을 다르게 구현해 URL로 위치를 공유하거나 새로고침 후 복원하기 어려웠다. HOT 목록은 추천 수 순위가 별도라 일반 feed cursor 계약을 그대로 재사용할 수도 없었다.

일반 feed는 `(created_at, id)` 경계를 유지하고, HOT은 `(recommendation_count, created_at, id)`를 versioned URL-safe cursor로 확장했다. 두 조회 모두 `limit + 1`건으로 `hasNext`를 계산하고, 프론트는 필터 조합별 cursor chain을 메모리에 보관해 `page=N` 직접 접근도 앞 페이지 경계를 순서대로 확보한다. 페이지를 이동할 때는 누적하지 않고 현재 페이지로 교체해 번호와 목록 순위가 어긋나지 않게 했다.

- 근거: [`../pagination-plan.md`](../pagination-plan.md), `PublicationFeed`, `BestFeedController`, `useCursorPagination`, `CursorPagination`, `PublicationControllerTest`
- 검증: `./gradlew test --tests com.townpet.publication.PublicationControllerTest`, `corepack pnpm typecheck`, frontend 34 tests와 build budget
- trade-off·한계: cursor 기반은 전체 페이지 수와 마지막 번호를 미리 알 수 없다. HOT 추천 수가 페이지 요청 사이에 바뀌면 snapshot이 아니므로, 완전한 순위 snapshot이나 서명 cursor는 실제 운영 요구가 생길 때 별도 결정한다.

## 15. 브라우저 gate에서 모바일 feed의 실제 렌더링 결함을 찾았다

cursor pagination을 연결한 뒤 unit/integration 테스트와 desktop 화면은 통과했지만, 전체 Chromium desktop/mobile E2E에서 mobile synthetic feed 항목만 보이지 않는 실패가 남았다. DOM에는 제목이 있었지만 링크의 bounding box 너비가 `0px`였다. 좁은 화면에서 chip 영역을 고정한 flex layout이 남은 제목 영역을 모두 소비한 것이 원인이었다.

모바일 breakpoint에서 feed item을 grid로 전환하고 chip을 줄바꿈하도록 수정했다. 기존 화면 assertion과 pagination mock도 현재 `HOT 글`·`전체글`·`page.totalPages` 계약에 맞추고, desktop/mobile visual baseline을 재생성했다. 그 뒤 54개 전체 E2E가 통과했다.

- 근거: `frontend/src/styles.css`, `frontend/e2e/public-search-parity.spec.ts`, `frontend/e2e/desktop-visual.spec.ts`, `frontend/e2e/desktop-visual.spec.ts-snapshots/`
- 검증: `corepack pnpm test:e2e` 54개 통과, frontend typecheck/Vitest/build와 backend `clean check migrationTest` 통과
- trade-off: visual snapshot은 화면 계약 변경을 빠르게 감지하지만 baseline 갱신만으로 결함을 숨길 수 있다. 이번에는 snapshot을 갱신하기 전에 DOM geometry를 확인해 실제 CSS 문제와 단순 기준선 차이를 분리했다.

## 16. 커뮤니티 확산을 막던 publication 지역 공개 범위를 제거했다

초기 parity 모델은 publication마다 `GLOBAL`·`LOCAL`과 publication 전용 neighborhood를 저장했다. 그 결과 글 작성 화면에는 공개 범위 선택이 생겼고, 로그인 여부·대표 동네·feed query가 같은 글의 노출을 다르게 만들었다. 커뮤니티 전체 클릭과 검색을 우선하기로 결정하면서 이 구분은 제품 가치보다 계약·인덱스·테스트 비용을 키우는 제약이 됐다.

V062에서 적용된 publication 제약·컬럼·scope 인덱스를 제거하고, projection view를 active lifecycle 중심으로 재생성했다. JPA entity와 service, jOOQ feed, controller DTO에서 scope와 publication neighborhood를 삭제했으며, 로그인 viewer에게만 block 정책을 남겼다. 입양·지역 가이드의 자체 neighborhood와 guest step-up 보안 scope는 별도 도메인이어서 보존했다. React 작성·수정 화면과 feed 탭·chip·query도 함께 제거하고, fixture와 요청 mock을 새 계약으로 바꿨다.

- 근거: `V062__remove_publication_scope.sql`, `PublicationEntity`, `PublicationFeed`, `CommunityFeed`, `frontend/src/api/client.ts`, `PLAN.md`
- 검증: `./gradlew compileJava`, `./gradlew compileTestJava`, frontend `tsc --noEmit`, Vitest 37개 통과. Testcontainers 기반 전체 migration/integration gate는 Docker container log wait 실패와 build image의 디스크 부족으로 완료하지 못했으며, 이 환경 한계를 배포 완료로 포장하지 않는다.
- trade-off·한계: publication 작성은 단순해졌지만 neighborhood 기반 개인화 feed는 사라졌다. 실제 지역별 discovery가 필요해지면 publication visibility를 되살리기보다 별도 지역 탐색 기능과 새 ADR을 검토한다.
