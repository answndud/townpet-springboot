# PLAN.md

## Goal

외부 VPS·DNS·TLS·object storage를 구성하기 전, Spring Boot 백엔드를 배포 가능한 release candidate 수준으로 완성한다. 모든 유지 대상 API가 정상·오류·권한·상태 전이를 일관되게 처리하고, PostgreSQL/Flyway 기준의 데이터 불변식·트랜잭션·쿼리 효율·실행 재현성이 검증되며, 깨끗한 환경에서 재기동해도 알려진 오류가 없는 상태를 완료로 본다.

범위는 `src/main/java`, `src/main/resources`, backend migration·test·Docker 실행 경로다. React 화면 구현과 실제 공개 배포는 이 계획의 완료 조건이 아니며, 공개 배포·TLS·외부 저장소·SMTP·off-site backup·monitoring은 G9에서 별도로 다룬다.

## Current status

이전 release-candidate(`fb7241d`)를 기준으로 백엔드만 세 차례 재감사했다. 1차는 보안 오류 계약·관리자 self-target·입력 상한, 2차는 대량 moderation update의 무제한 entity load와 실제 반영 건수 오류, 3차는 request trace·duration log와 graceful shutdown 상한을 닫았다. 현재 변경은 `6366d41`, `dab5527`, `7de4416`에 묶였고 각 사이클의 인접 테스트와 최종 fresh backend gate가 통과했다. 공개 배포는 시작하지 않는다.

## Active

### 재감사 사이클 - 실제 충돌·성능·재기동 결함을 닫는다

이번 최종 감사에서는 기능을 더 잘게 쪼개지 않고 아래 세 사이클을 완료했다. 마지막 통합 gate만 남겨 둔다.

1. **보안·입력 경계** — `6366d41` 완료
   - 인증 실패와 권한 부족을 동일한 ProblemDetail 계약으로 통일하고 trace id를 보존한다.
   - 관리자 본인 제재를 차단하고 moderation reason·신고 상태·bulk ID에 서버 상한과 공백 검증을 적용한다.
   - CSP report의 빈 body가 500으로 흐르지 않도록 명시적인 413/400 경계를 둔다.
   - 검증: `GlobalProblemHttpTest`, `IdentityMemberControllerTest`.

2. **상태·대량 처리 효율** — `dab5527` 완료
   - 작성자별 콘텐츠 공개 범위 변경을 전체 entity load/save에서 조건부 bulk update로 바꾸고 version을 증가시킨다.
   - moderation bulk 응답은 요청 수가 아니라 실제 존재·변경 대상 수를 반환한다.
   - 작성자/lifecycle 복합 index와 Modulith/ArchUnit 경계를 함께 고정한다.
   - 검증: publication·identity integration test, architecture migration test.

3. **실행 관측·종료 안정성** — `7de4416` 완료
   - 모든 요청에 method/path/status/duration과 trace id를 남기되 query string은 기록하지 않는다.
   - 로그 pattern에 trace id를 연결하고 graceful shutdown phase를 20초로 제한한다.
   - 검증: `RequestTraceFilterTest`, `GlobalProblemHttpTest`.

4. **최종 통합 gate** — 완료
   - 위 사이클 이후 clean backend test, migration/performance, bootJar를 한 번만 fresh 실행한다.
   - Docker 빈 volume에서 migration→health/readiness→demo seed 2회→재기동을 확인한다.
   - 실제 실행: `./gradlew clean check migrationTest performanceTest bootJar --no-daemon` 성공(2026-08-12), `./scripts/validate-release-candidate.sh` 성공.
   - Docker runtime은 이전 V052 재기동 evidence와 Testcontainers migration/performance 검증을 함께 근거로 삼으며, 공개 배포 직전에 새 빈 volume smoke를 다시 수행한다.

1. **상태·동시성 불변식을 실제 저장소에서 고정한다.**
   - 파일: `care/`, `welfare/VolunteerService.java`, `media/MediaService.java`, `publication/PublicationMetricsController.java`, `db/migration/V052__moderator_case_open_flag_uniqueness.sql`
   - 변경: 돌봄 수락 row lock·bulk version 증가, volunteer capacity와 `FULL` 전이, 병원 신고 open 중복 unique index, publication metric atomic upsert, media publication row lock, exact location pair validation을 유지한다.
   - 검증: care/media/welfare test, PostgreSQL concurrency test, migration startup, Docker seed.
   - 완료: 경쟁 요청이 중복·초과·lost update를 만들지 않고 충돌은 409로 수렴한다.

2. **조회 성능 검증을 실제 PostgreSQL plan으로 고정한다.**
   - 파일: `build.gradle.kts`, `src/test/java/com/townpet/performance/ReleaseCandidateQueryPlanTest.java`, `docs/report/technical-notes.md`
   - 변경: `performanceTest`가 전체 테스트를 중복 실행하지 않고 query-plan/concurrent-upsert fixture만 실행하게 하며, 대표 queue index 사용을 `EXPLAIN (ANALYZE, BUFFERS)`로 확인한다.
   - 검증: `./gradlew performanceTest --no-daemon`.
   - 완료: 성능 주장이 작은 fixture의 측정 범위를 넘지 않고, index와 atomic write 근거가 재현된다.

3. **clean runtime과 전체 gate를 다시 통과시킨다.**
   - 파일: `deploy/compose/local.yml`, `scripts/seed-local-demo.sh`, backend test/build 경로
   - 변경: Docker backend를 새 image로 재기동해 Flyway `052` 적용, health/readiness, seed→seed 멱등성을 확인한다. 이후 전체 backend gate를 fresh run한다.
   - 검증: `./gradlew clean check migrationTest performanceTest --no-daemon`, `./gradlew bootJar --no-daemon`, `./scripts/validate-release-candidate.sh`, Docker health/seed.
   - 완료: 모든 새 변경이 green이고, untracked/secret/generated artifact 없이 commit 가능한 상태다.

### P1 - 백엔드 기능·계약·권한을 닫는다

#### P1.1 - HTTP 오류와 입력 계약을 하나의 정책으로 통합한다

- 파일: `src/main/java/com/townpet/common/web/`, `src/main/java/com/townpet/*/*Controller.java`, 관련 `src/test/java/com/townpet/**`
- 변경:
  - `ResponseStatusException`, `null` 반환, controller별 문자열 오류를 공통 domain/application 예외와 `ProblemDetail` 코드로 정리한다.
  - validation, 잘못된 UUID·cursor·limit·filter, 존재하지 않는 리소스, 인증 실패, 권한 부족, 상태 충돌, optimistic-lock 충돌을 각각 고정된 status/code로 매핑한다.
  - 401·403·404를 resource visibility 규칙에 맞게 구분하고, 오류 응답에 credential·정확 위치·내부 SQL·stack trace가 노출되지 않게 한다.
  - trace/correlation id 생성·전달 규칙을 정하고 모든 API 오류에서 재현 가능한 형태로 제공한다.
- 검증: 공통 `GlobalProblemHandler` 단위 테스트와 각 controller의 정상·validation·404·401·403·409 통합 테스트를 변경 module별로 실행한다.
- 완료: 동일한 오류 유형이 어느 module에서도 같은 status/code/response shape을 반환하고, 주요 endpoint의 계약 테스트가 통과한다.

#### P1.2 - Identity·RBAC·ownership·privacy 매트릭스를 backend에서 강제한다

- 파일: `src/main/java/com/townpet/identity/`, `src/main/java/com/townpet/common/security/`, `src/main/java/com/townpet/member/`, `src/main/java/com/townpet/relationship/`, `src/main/java/com/townpet/media/`, `src/main/java/com/townpet/trustsafety/`, `src/test/java/com/townpet/identity/`
- 변경:
  - anonymous, MEMBER, MODERATOR, ADMIN/운영 내부 계정의 읽기·쓰기·운영 API 매트릭스를 endpoint 목록으로 고정한다.
  - method security가 실제 application method까지 적용되는지 확인하고, controller matcher 누락·legacy alias·guest cookie/step-up 우회 경로를 모두 같은 정책으로 묶는다.
  - resource owner, staff review, blocked member, 공개 게시글·댓글·반응·반려동물 프로필, 분실동물 정확 위치·private media 규칙을 service/domain에서 검사한다.
  - session·CSRF·password/email token lifecycle의 만료·single-use·재사용·실패 잠금·로그아웃 동작을 명확히 한다.
- 검증: 각 역할의 허용/거부 쌍을 `MockMvc` 또는 Testcontainers PostgreSQL 통합 테스트로 작성하고, staff가 일반 회원/guest write API를 우회할 수 없는지 회귀 테스트한다.
- 완료: 모든 보호 API가 deny-by-default로 동작하고, 권한과 privacy 결과가 신규·legacy route에서 동일하다.

#### P1.3 - 핵심 상태 전이와 동시성 불변식을 원자화한다

- 파일: `src/main/java/com/townpet/care/`, `src/main/java/com/townpet/marketplace/`, `src/main/java/com/townpet/lostfound/`, `src/main/java/com/townpet/gathering/`, `src/main/java/com/townpet/publication/`, `src/main/java/com/townpet/engagement/`, 해당 migration·test
- 변경:
  - Care request/application/assignment/feedback, marketplace listing/group-buy, lost-found alert/sighting, gathering 참가, publication/comment/reaction/bookmark의 허용 상태 전이를 domain/application service에 모은다.
  - controller에 남은 상태·소유권 판단을 제거하고 transaction 경계를 application service에 둔다.
  - version·조건부 update·unique/check/FK constraint로 중복 신청·중복 반응·정원 초과·잘못된 종료·이미 사용한 token을 방지한다.
  - 재시도 가능한 command는 idempotency key 또는 unique business key를 사용하고, 충돌은 409로 돌려준다.
  - event publication이 필요한 변경만 source transaction과 함께 기록하고 consumer는 재처리 가능하게 만든다.
- 검증: 상태 전이 표의 정상·역전이·중복 요청·동시 요청을 integration test로 실행하고, PostgreSQL constraint/optimistic lock 실패를 실제 응답까지 확인한다.
- 완료: 모든 핵심 aggregate가 허용된 전이만 수행하며, 동시 요청에서도 데이터가 깨지지 않고 재시도 결과가 결정적이다.

#### P1.4 - 유지 대상 API의 기능 공백과 legacy compatibility를 닫는다

- 파일: `src/main/java/com/townpet/*/`, `api/` 계약 참조 파일, `src/test/java/com/townpet/contract/`, `src/test/java/com/townpet/parity/`
- 변경:
  - parity matrix의 유지 대상 API를 endpoint 단위로 다시 확인해 route만 존재하고 빈 응답·placeholder·무시된 입력을 반환하는 구현을 제거한다.
  - feed/search/best, catalog/local guide, welfare/adoption/volunteer/hospital, marketplace, gathering, notification, trust & safety, admin projection의 정상·빈 결과·잘못된 query·권한 결과를 명시한다.
  - 유지하지 않는 외부 계약은 조용히 성공시키지 않고 명시적인 404/409/capability 응답과 ADR 근거를 둔다.
- 검증: `./gradlew test --tests 'com.townpet.contract.*' parityInventoryTest`를 실행하고, 변경 module의 API integration test를 추가로 통과시킨다.
- 완료: 유지 대상 API에는 알려진 기능 공백이 없고, 제외 API는 오해를 일으키지 않는 명시적 동작을 갖는다.

### P2 - 구조와 데이터 접근을 리팩터링해 효율과 유지보수성을 확보한다

#### P2.1 - Modulith 경계와 계층 책임을 정리한다

- 파일: `src/main/java/com/townpet/*/`, `src/test/java/com/townpet/architecture/`, `src/test/java/com/townpet/platform/`
- 변경:
  - controller는 transport 변환만, application service는 use case·transaction·authorization만, domain은 business invariant만, infrastructure는 JPA/jOOQ/외부 adapter만 담당하도록 이동한다.
  - module 간 JPA association·repository·entity·controller DTO 참조를 제거하고 공개 `api` 또는 식별자/event로 연결한다.
  - 중복 principal 해석, ownership 검사, pagination parsing, 날짜/enum 변환, 동일 query adapter를 공통 기술 component로 추출하되 business shared model은 만들지 않는다.
  - 사용되지 않는 interface·추상 factory·legacy wrapper와 동일 조회의 JPA/jOOQ 이중 구현을 제거한다.
- 검증: `./gradlew modulithTest test`, ArchUnit/Modulith 경계 테스트, `spotlessCheck`, Error Prone/NullAway compile을 실행한다.
- 완료: 모듈 경계 위반과 controller 비대화가 사라지고, 각 use case의 transaction·의존 방향을 코드만 보고 설명할 수 있다.

#### P2.2 - 목록·feed·검색·admin 조회의 쿼리와 pagination을 측정 기반으로 개선한다

- 파일: `src/main/java/com/townpet/discovery/`, `src/main/java/com/townpet/catalog/`, `src/main/java/com/townpet/localguide/`, 각 list/report service·repository, `src/main/resources/db/migration/`
- 변경:
  - 반복적인 entity lazy-load/N+1과 controller 내 임의 SQL을 query adapter로 모으고, 읽기 전용 projection은 jOOQ/JDBC로 일관되게 반환한다.
  - 모든 목록에 상한·안정적인 tie-breaker·cursor 또는 명시적인 page 정책을 적용하고, 검색어·기간·scope·type filter의 의미를 고정한다.
  - 실제 query plan에서 필요한 복합/부분/index를 추가하고, 불필요한 select·count·중복 join을 줄인다. 측정 없이 Redis/검색 서버를 도입하지 않는다.
  - query count와 representative latency를 측정할 수 있는 test fixture를 만든다.
- 검증: PostgreSQL `EXPLAIN (ANALYZE, BUFFERS)` 기준 fixture, query-count/integration test, `./gradlew performanceTest`로 개선 전후를 기록한다.
- 완료: 대표 목록·feed·검색·admin 조회에 N+1·무제한 조회·불안정 정렬이 없고, 변경한 index/query가 측정 결과로 설명된다.

#### P2.3 - Flyway schema와 persistence lifecycle을 깨끗한 DB에서 재현한다

- 파일: `src/main/resources/db/migration/`, `src/test/java/com/townpet/platform/`, `migration/`, `deploy/compose/`
- 변경:
  - 적용된 migration은 수정하지 않고 새 migration으로 constraint/index/default/backfill을 추가한다.
  - `ddl-auto=validate`, UTC `Instant`, UUIDv7, optimistic version, FK/unique/check constraint가 entity mapping과 일치하는지 정리한다.
  - seed/demo/reset이 application data와 분리되고 반복 실행에 안전한지 확인한다. invalid migration row는 quarantine하고 조용히 삭제하지 않는다.
  - schema history, event publication, JDBC session, upload metadata가 재시작 후에도 일관되게 복구되는지 확인한다.
- 검증: 빈 PostgreSQL에서 migration 전체 적용, application 재기동, seed→reset→seed 반복, `./gradlew migrationTest integrationTest`를 실행한다.
- 완료: 새 DB와 기존 local DB 모두에서 startup validation 실패가 없고, migration·seed·reset이 재현 가능하며 data ownership이 유지된다.

### P3 - 실제 실행 중 실패를 조기에 발견하고 release candidate를 고정한다

#### P3.1 - profile·adapter·startup/shutdown 실패 정책을 명확히 한다

- 파일: `src/main/resources/application*.yml`, `src/main/java/com/townpet/media/`, `src/main/java/com/townpet/identity/`, `src/main/java/com/townpet/operations/`, `deploy/compose/`
- 변경:
  - local/test/e2e/smoke 설정의 datasource·session·media·email·demo flag 차이를 명시하고 필수 secret/위험한 기본값은 startup에서 fail-fast한다.
  - local filesystem media와 production 미설정 adapter, email capture와 production unavailable 정책의 API 응답·로그·UI 계약을 일치시킨다.
  - health/readiness, graceful shutdown, DB connection timeout, upload size/type/결로 traversal, session cookie/CSRF 설정을 실제 실행 조건에 맞춘다.
  - demo 계정·seed가 production profile에서 우연히 켜지지 않도록 이중 gate를 둔다.
- 검증: profile별 startup/health, 잘못된 env, DB down, media missing, email unavailable, SIGTERM graceful shutdown 시나리오를 Docker 또는 integration test로 실행한다.
- 완료: 설정 누락·외부 adapter 장애·재시작이 사용자에게 모호한 500을 남기지 않고, 안전한 실패와 복구 경로를 가진다.

#### P3.2 - 관측·보안·운영 진단 정보를 backend에 고정한다

- 파일: `src/main/java/com/townpet/common/`, `src/main/java/com/townpet/operations/`, `src/main/resources/application*.yml`, 관련 test
- 변경:
  - request/trace id, method·route·status·duration·DB failure를 구조화해 기록하되 credential/session/token/정확 위치/개인정보는 마스킹한다.
  - Actuator health/info와 핵심 4xx/5xx·DB pool·migration·media cleanup 상태를 확인할 수 있는 최소 metric을 정한다.
  - rate/size 제한, CORS, security headers, file/object key 검증, password/token hash와 secret 로그 누출을 점검한다.
  - 운영자가 실패 원인을 로그 한 건과 재현 command로 추적할 수 있도록 correlation contract를 문서화한다.
- 검증: log assertion/security test, actuator endpoint test, 민감정보 grep 및 오류 주입 test를 실행한다.
- 완료: 장애·권한 실패·데이터 오류를 진단할 증거가 있고, 민감정보가 response/log/metric에 남지 않는다.

#### P3.3 - backend test pyramid와 실패 회귀 묶음을 완성한다

- 파일: `src/test/java/com/townpet/`, `src/test/resources/`, `scripts/validate-release-candidate.sh`, `deploy/compose/`
- 변경:
  - domain/service 단위 test, controller contract test, PostgreSQL Testcontainers integration/migration test, Modulith/ArchUnit test의 책임을 겹치지 않게 정리한다.
  - 모든 핵심 여정에 정상·빈 결과·validation·401·403·404·409·동시성·재시작 case를 대표 fixture로 연결한다.
  - flaky clock/random/network 의존을 고정하고, 테스트가 실제 DB constraint·transaction·session을 우회하지 않도록 구분한다.
  - 실패 시 원인과 재현 명령이 출력되도록 Gradle task와 smoke script를 정리한다.
- 검증: 변경 중에는 module test만 실행하고, phase 완료 때 `./gradlew clean check integrationTest modulithTest migrationTest performanceTest --no-daemon`을 fresh run한다.
- 완료: backend test suite가 반복 실행해도 안정적이고, 기능·권한·데이터·구조·성능 회귀를 각각 잡는다.

#### P3.4 - 깨끗한 환경에서 backend release candidate를 최종 고정한다

- 파일: `deploy/Dockerfile.backend`, `deploy/compose/local.yml`, `scripts/seed-local-demo.sh`, `scripts/reset-demo-data.sh`, `scripts/frontend-backend-smoke.sh`, `docs/runbooks/`
- 변경:
  - Docker PostgreSQL과 backend를 빈 volume에서 시작해 migration→seed→login→대표 API 정상/오류/권한→media/email local adapter→reset→재시작 순서로 재현한다.
  - Gradle bootJar/container build와 IDE 실행 profile의 결과를 맞추고, 환경 변수·포트·health URL·demo credential 문서를 최신 코드와 일치시킨다.
  - 현재 HEAD의 변경을 모두 의도적으로 commit한 뒤 untracked/secret/generated artifact가 없는지 확인한다.
  - 검증 결과, 남은 제한, 배포 전 deferred 항목만 report에 기록한다. 통과한 명령 목록을 경험담처럼 복사하지 않는다.
- 검증:
  - `./gradlew clean check integrationTest modulithTest migrationTest performanceTest --no-daemon`
  - `./scripts/validate-release-candidate.sh`
  - 빈 Docker volume에서 local Compose smoke와 seed/reset 반복
  - `./gradlew bootJar` 및 backend container health check
- 완료: 깨끗한 clone/volume에서 같은 명령으로 backend가 기동하고 대표 사용자 여정이 재현되며, 알려진 blocker·flaky test·미커밋 산출물이 없다.

## Backlog

- G9 - 사용자가 배포를 시작할 때 Hetzner VPS, DNS/TLS/Caddy, 외부 object storage/SMTP, off-site backup·restore·rollback, monitoring·alerting, 공개 URL smoke와 비용을 구성한다.
- 실제 Legacy 개인정보 migration, Kakao/Naver OAuth, 결제·정산·환불·private chat은 현재 범위에 포함하지 않는다.
- 실제 검색 corpus·ranking 품질·latency가 P2 측정 기준을 넘을 때만 SearchDocument/GIN·trigram·personalization projection을 별도 ADR로 결정한다.

## 완료 판정

P1~P3의 모든 완료 조건과 최종 fresh gate를 통과하면 “배포 전 backend release candidate 완료”라고 표현한다. 실제 공개 운영의 TLS·외부 서비스·백업 복구·SLA까지 완료했다고 표현하지 않는다.
