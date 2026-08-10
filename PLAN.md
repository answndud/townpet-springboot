# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 49개 page·55개 API를 Java 25, Spring Boot 4.1, React·Vite와 PostgreSQL 18로 재아키텍처한다. 완료 시 Next.js·Prisma·NextAuth server runtime 없이 공개 showcase가 동작하고, parity·migration·성능·배포·복구 증거가 자동 검증된다.

## Completed

### P1.1 - Gradle·Spring Boot repository skeleton 생성

- 결과: Gradle 9.7 Wrapper, Java 25 toolchain, Spring Boot 4.1.0, Spring Modulith 2.1.0, MVC·Security·Validation·JPA·jOOQ·Flyway·Session JDBC·Actuator·Testcontainers 기반과 Spotless·Error Prone·NullAway·JaCoCo gate를 구성했다.
- 검증: `./gradlew --version && ./gradlew clean check`, `./gradlew integrationTest modulithTest migrationTest performanceTest` 통과. 네 개 verification task가 실제 Gradle task로 등록된 것도 확인했다.
- 보고서: [`docs/report/evolution/EV-001-p1-1-build-foundation.md`](docs/report/evolution/EV-001-p1-1-build-foundation.md), [`docs/report/knowledge/java-gradle-spring-foundation.md`](docs/report/knowledge/java-gradle-spring-foundation.md)

### P1.2 - PostgreSQL·PostGIS·MinIO local runtime과 최초 Flyway migration

- 결과: PostgreSQL 18·PostGIS 3.6 Compose, MinIO local object storage, least-privilege app role, Flyway `V001` platform baseline과 Testcontainers migration test를 구성했다. Modulith event schema 자동 생성은 끄고 Flyway가 schema authority가 되도록 정리했다.
- 검증: `docker compose -f deploy/compose/local.yml config`, Compose 두 서비스 healthy, `./gradlew migrationTest`, `./gradlew bootRun`, `/actuator/health` `UP`, `flyway_schema_history` version `001` 확인.
- 보고서: [`docs/report/evolution/EV-002-p1-2-database-baseline.md`](docs/report/evolution/EV-002-p1-2-database-baseline.md), [`docs/report/knowledge/postgres-flyway-baseline.md`](docs/report/knowledge/postgres-flyway-baseline.md)

### P1.3 - 17개 Application Module 경계와 architecture 문서를 코드로 검증한다

- 결과: ADR-0011의 17개 bounded context와 `api` named interface를 package 선언으로 추가하고, Spring Modulith cycle/dependency 검증과 ArchUnit 내부 계층 노출 규칙을 구성했다. Mermaid module map과 면접용 evolution/knowledge report를 추가했다.
- 검증: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew modulithTest --no-daemon` 통과. 17개 module detection, Spring context, migration test와 architecture rules가 모두 통과했다.
- 보고서: [`docs/report/evolution/EV-003-p1-3-module-boundaries.md`](docs/report/evolution/EV-003-p1-3-module-boundaries.md), [`docs/report/knowledge/spring-modulith-architecture.md`](docs/report/knowledge/spring-modulith-architecture.md)

### P1.4 - OpenAPI code generation과 ProblemDetail contract를 만든다

- 결과: OpenAPI 3.1 `/api/v1` contract에서 Spring Java transport와 TypeScript fetch client를 생성하고, RFC 9457 ProblemDetail 공통 handler와 contract test를 추가했다. generated source는 `build/generated`에서만 관리한다.
- 검증: `./gradlew openApiValidate generateOpenApiClients checkGeneratedSources contractTest` 통과. `clean check`에도 OpenAPI validation·contract gate가 연결됐다.
- 보고서: [`docs/report/evolution/EV-004-p1-4-openapi-contract.md`](docs/report/evolution/EV-004-p1-4-openapi-contract.md), [`docs/report/knowledge/openapi-problemdetail.md`](docs/report/knowledge/openapi-problemdetail.md)

### P1.5 - React·Vite shell과 기존 URL·visual parity 하네스를 만든다

- 결과: React 19·Vite 6·React Router shell에 기존 로고·공개 header·blue palette·홈 CTA를 이식하고, `/api` proxy와 OpenAPI transport seam을 추가했다. desktop Chromium/mobile Pixel 5 Playwright shell smoke를 구성했다.
- 검증: `corepack pnpm -C frontend typecheck && corepack pnpm -C frontend test && corepack pnpm -C frontend build && corepack pnpm -C frontend test:e2e` 통과. 4개 E2E(2 journeys × 2 viewports)가 통과했다.
- 보고서: [`docs/parity/shell.md`](docs/parity/shell.md), [`docs/report/evolution/EV-005-p1-5-vite-shell.md`](docs/report/evolution/EV-005-p1-5-vite-shell.md), [`docs/report/knowledge/react-vite-parity.md`](docs/report/knowledge/react-vite-parity.md)

### P1.6 - Page·API·data parity matrix와 differential runner를 생성한다

- 결과: legacy 49 page·55 API route file과 HTTP method를 `docs/parity/matrix.yaml`에 고정하고, guest/member/staff logical fixture, Java inventory test, UUID·timestamp·signed URL normalization을 추가했다.
- 검증: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew parityInventoryTest`와 `corepack pnpm -C frontend typecheck && corepack pnpm -C frontend test && corepack pnpm -C frontend build` 통과. parity inventory도 `clean check` gate에 연결했다.
- 보고서: [`docs/report/evolution/EV-006-p1-6-parity-inventory.md`](docs/report/evolution/EV-006-p1-6-parity-inventory.md), [`docs/report/knowledge/parity-differential-testing.md`](docs/report/knowledge/parity-differential-testing.md)

### P1.7 - Frontend·backend 통합 smoke와 CI quality gate를 연결한다

- 결과: Java 25 backend, Node 22 frontend, integration smoke, main browser smoke를 계층형 GitHub Actions workflow로 연결하고, 동일한 `scripts/frontend-backend-smoke.sh`를 로컬·CI에서 실행하게 했다. smoke profile은 H2 `developmentOnly`로 production runtime과 분리했다.
- 검증: `./gradlew clean check`, `corepack pnpm -C frontend install --frozen-lockfile && corepack pnpm -C frontend typecheck && corepack pnpm -C frontend test && corepack pnpm -C frontend build`, `./scripts/frontend-backend-smoke.sh` 통과.
- 보고서: [`docs/report/evolution/EV-007-p1-7-quality-gate.md`](docs/report/evolution/EV-007-p1-7-quality-gate.md), [`docs/report/knowledge/ci-quality-gates.md`](docs/report/knowledge/ci-quality-gates.md)

### P2.1a - Identity·Member·Catalog thin vertical slice를 연결한다

- 결과: V002 identity/member/catalog schema, Spring Session JDBC·BCrypt·Cookie CSRF 인증, login/current member/onboarding/catalog API와 React login form을 연결했다. 17개 모듈을 한 번에 확장하지 않고 핵심 흐름만 먼저 검증했다.
- 검증: `./gradlew clean check migrationTest`, `corepack pnpm -C frontend install --frozen-lockfile && corepack pnpm -C frontend typecheck && corepack pnpm -C frontend test && corepack pnpm -C frontend build && corepack pnpm -C frontend test:e2e`, `./scripts/frontend-backend-smoke.sh` 통과.
- 보고서: [`docs/report/evolution/EV-008-p2-1a-identity-member-catalog.md`](docs/report/evolution/EV-008-p2-1a-identity-member-catalog.md), [`docs/report/knowledge/spring-session-csrf-auth.md`](docs/report/knowledge/spring-session-csrf-auth.md)

### P2.1b-1 - Session revoke와 Member onboarding pet slice를 harden한다

- 결과: 반려동물 entity/repository와 회원 소유 목록 교체를 onboarding transaction에 연결하고, 현재 회원 응답·React profile·logout을 추가했다. CSRF token cookie를 명시적으로 보장하고 입력 길이·목록 개수 validation을 적용했다.
- 검증: `./gradlew test --tests '*IdentityMemberControllerTest'` 통과. 전체 `clean check`, OpenAPI, frontend와 smoke는 다음 slice 종료 gate에서 다시 실행한다.
- 보고서: [`docs/report/evolution/EV-009-p2-1b-session-onboarding.md`](docs/report/evolution/EV-009-p2-1b-session-onboarding.md), [`docs/report/knowledge/member-onboarding-and-session-revoke.md`](docs/report/knowledge/member-onboarding-and-session-revoke.md)

## Active

### P2 - Domain별 vertical slice로 Write Owner를 Spring으로 옮긴다

1. P2.1b-2 - Identity 정책과 demo onboarding parity를 닫는다
   - 파일: `src/main/java/com/townpet/identity/**`, `src/main/java/com/townpet/member/**`, `src/main/java/com/townpet/catalog/**`, `frontend/src/LoginPage.tsx`, `frontend/e2e/auth-parity.spec.ts`
   - 변경: password reset·verification, demo account seed/lifecycle, deny-by-default member/staff policy와 legacy auth differential row를 추가한다. OAuth는 provider stub contract로만 검증한다. P2.1b-1의 logout·pet onboarding 회귀를 유지한다.
   - 검증: `./gradlew check integrationTest --tests '*Identity*' --tests '*Member*' && corepack pnpm -C frontend test:e2e -- auth-parity.spec.ts`
   - 완료: login·logout·session revoke·onboarding·member/profile IDOR·CSRF가 실제 Spring session과 parity fixture에서 통과하고 P2.1 전체 범위를 닫는다.

2. P2.2 - Publication·Media 작성·상세 vertical slice를 완성한다
   - 파일: `src/main/java/com/townpet/publication/**`, `src/main/java/com/townpet/media/**`, `frontend/src/features/publication/**`, `src/main/resources/db/migration/V1*__publication_media.sql`
   - 변경: Publication lifecycle, actor ownership, LOCAL·GLOBAL scope, structured aggregate link와 presigned upload·finalize·derivative lifecycle을 구현한다. Spring MVC HTML shell metadata와 기존 작성·상세 UI를 연결한다.
   - 검증: `./gradlew integrationTest --tests '*Publication*' --tests '*Media*' && corepack pnpm -C frontend test:e2e -- publication-parity.spec.ts upload-parity.spec.ts`
   - 완료: 회원·guest 작성, 수정·삭제, direct URL, metadata와 media orphan cleanup이 Spring API만 사용하고 publication legacy write를 제거한다.

3. P2.3 - Engagement·Relationship 동시성 vertical slice를 완성한다
   - 파일: `src/main/java/com/townpet/engagement/**`, `src/main/java/com/townpet/relationship/**`, `frontend/src/features/engagement/**`, `src/test/java/com/townpet/engagement/**`
   - 변경: Comment·reply, reaction, bookmark, view bucket, atomic summary, follow·block와 authorization을 구현한다. Source·summary reconciliation과 concurrent duplicate test를 추가한다.
   - 검증: `./gradlew integrationTest mutationTest --tests '*Engagement*' --tests '*Relationship*' && corepack pnpm -C frontend test:e2e -- engagement-parity.spec.ts`
   - 완료: 동시 요청 후 source·summary가 일치하고 block·restriction·IDOR matrix와 기존 댓글·반응 UI parity가 통과한다.

4. P2.4 - LostFound 대표 domain을 완성한다
   - 파일: `src/main/java/com/townpet/lostfound/**`, `src/main/resources/db/migration/V2*__lost_found.sql`, `frontend/src/features/lostfound/**`, `frontend/e2e/lost-found-parity.spec.ts`
   - 변경: Alert, SightingReport, PostGIS approximate point, encrypted exact evidence, outcome·reason lifecycle, reminder, share text·전단과 owner management를 구현한다. Legacy sighting comment ETL을 포함한다.
   - 검증: `./gradlew integrationTest mutationTest --tests '*LostFound*' && corepack pnpm -C frontend test:e2e -- lost-found-parity.spec.ts`
   - 완료: 등록·공개/보호자 제보·반경 검색·공유·해결·종료·재개가 privacy leakage 없이 동작하고 LostFound legacy adapter를 제거한다.

5. P2.5 - Marketplace와 나머지 구조화 게시 domain을 이전한다
   - 파일: `src/main/java/com/townpet/marketplace/**`, `src/main/java/com/townpet/{localguide,welfare,care,gathering}/**`, `frontend/src/features/structured/**`, `src/main/resources/db/migration/V3*__structured_domains.sql`
   - 변경: Marketplace sealed terms·lifecycle·safety corpus를 우선 구현하고 LocalGuide, Welfare, CareAssignment, Meetup 정원과 기존 구조화 field를 parity 중심으로 이전한다.
   - 검증: `./gradlew integrationTest mutationTest --tests '*Marketplace*' --tests '*Structured*' && corepack pnpm -C frontend test:e2e -- structured-domain-parity.spec.ts`
   - 완료: 유형별 constraint·상태·권한·concurrency test와 관련 board·detail·form parity가 통과하고 해당 legacy server code를 제거한다.

6. P2.6 - TrustSafety·Discovery·Notification·Operations를 연결한다
   - 파일: `src/main/java/com/townpet/{trustsafety,discovery,notification,operations}/**`, `src/main/resources/db/migration/V4*__projections_operations.sql`, `frontend/src/features/{admin,search,feed,notifications}/**`, `src/test/java/com/townpet/operations/**`
   - 변경: report·restriction·sanction·audit, SearchDocument·FeedDocument, durable notification와 Event Publication Registry retry, rebuild·reconciliation·demo scoped reset을 구현한다.
   - 검증: `./gradlew integrationTest mutationTest performanceTest --tests '*TrustSafety*' --tests '*Discovery*' --tests '*Notification*' --tests '*Operations*' && corepack pnpm -C frontend test:e2e -- admin-search-feed-parity.spec.ts`
   - 완료: hidden·blocked 정보가 stale projection에서도 누출되지 않고 retry·rebuild·reset이 중복 없이 성공하며 모든 page가 Spring read/write owner를 사용한다.

### P3 - Migration·성능·운영 증거를 닫고 Legacy를 제거한다

1. P3.1 - 전체 Legacy ETL과 reconciliation을 완성한다
   - 파일: `migration/src/main/java/com/townpet/migration/**`, `migration/fixtures/**`, `docs/parity/data-mapping.md`, `src/test/java/com/townpet/migration/**`
   - 변경: ID·관계·상태·시간·media manifest를 idempotent하게 변환하고 invalid row quarantine, count·FK·orphan·summary·sample 대사와 snapshot dry-run report를 만든다.
   - 검증: `./gradlew migrationTest && ./gradlew migrationRehearsal --args='--fixture=full --dry-run'`
   - 완료: 같은 snapshot을 두 번 실행해도 결과가 같고 모든 mismatch가 zero 또는 승인된 quarantine 근거를 가진다.

2. P3.2 - 49 page·55 API 동등성과 Legacy 제거를 완료한다
   - 파일: `docs/parity/matrix.md`, `frontend/e2e/**`, `src/test/java/com/townpet/parity/**`, `frontend/src/**`
   - 변경: 전체 differential·visual·accessibility·SEO 행을 닫고 temporary adapter·feature flag·Next.js·Prisma·NextAuth import와 Node server script를 제거한다.
   - 검증: `./gradlew clean check parityTest && corepack pnpm -C frontend test:e2e --project=legacy --project=spring && rg -n 'next/|next-auth|@prisma|PrismaClient' frontend src build.gradle.kts`
   - 완료: Parity matrix가 100% PASS 또는 승인 차이이고 금지 import 검색 결과가 없으며 production artifact에 Node server가 없다.

3. P3.3 - 성능 baseline과 선택적 최적화를 증명한다
   - 파일: `src/test/java/com/townpet/performance/**`, `docs/performance/baseline.md`, `docs/performance/query-plans/**`, `frontend/e2e/web-vitals.spec.ts`
   - 변경: 고정 seed·warm-up·concurrency에서 feed, search, detail, write의 p50·p95·p99, SQL count, buffers, lock, JVM을 측정한다. Index·query·fetch를 한 번에 하나씩 변경하고 cache는 측정된 병목이 있을 때만 추가한다.
   - 검증: `./gradlew performanceTest && corepack pnpm -C frontend test:e2e -- web-vitals.spec.ts`
   - 완료: Read p95 300ms·write p95 500ms와 Web Vitals 목표를 평가하고 전후 수치·EXPLAIN·포기한 대안을 재현 가능하게 기록한다.

4. P3.4 - CI/CD·observability와 Hetzner showcase를 배포한다
   - 파일: `.github/workflows/{pr,main,nightly,deploy}.yml`, `Dockerfile`, `deploy/{terraform,ansible,caddy,compose}/**`, `src/main/java/com/townpet/common/observability/**`
   - 변경: 계층형 quality gate, SBOM·scan·provenance·GHCR, structured telemetry·Alloy, CX23 IaC, Caddy A/B 전환·rollback과 고정 demo account 공개를 구성한다.
   - 검증: `./gradlew clean check && docker build -t townpet-springboot:local . && terraform -chdir=deploy/terraform validate && ansible-playbook --syntax-check deploy/ansible/site.yml`
   - 완료: 검증된 image digest만 공개되고 deploy smoke·SLO 관찰·실패 rollback 기록과 월 1만 원 이하 비용 evidence가 남는다.

5. P3.5 - Backup·restore·projection rebuild와 운영 인수 조건을 검증한다
   - 파일: `deploy/backup/**`, `docs/runbooks/{incident,backup-restore,deployment,projection-rebuild}.md`, `src/test/java/com/townpet/operations/**`, `README.md`
   - 변경: WAL archive, logical·physical backup, retention, encrypted key custody, weekly restore, quarterly clean rebuild와 operation phase timing을 구현한다. README에 fresh clone·architecture·대표 문제·성능·공개 URL을 연결한다.
   - 검증: `./gradlew operationsTest && deploy/backup/verify-restore.sh --target disposable && ./gradlew bootJar && java -jar build/libs/townpet-springboot.jar --spring.profiles.active=smoke`
   - 완료: RPO 5분·RTO 60분 drill, event·search·feed rebuild, scoped demo reset과 fresh-clone 실행이 실제 evidence로 검증되고 최종 portfolio handoff가 가능하다.

## Backlog

- 실제 community launch가 필요해질 때 개인정보·국외 이전·retention·약관·moderation staffing PRD를 별도로 작성한다.
- 실측 병목이 PostgreSQL projection 경계를 넘을 때 Kafka·Elasticsearch·Redis 분리 실험을 독립 ADR로 수행한다.
- 단일 CX23의 SLO가 지속적으로 부족할 때 region·managed database·두 번째 compute 비용 시뮬레이션을 갱신한다.
