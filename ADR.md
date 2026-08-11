# Architecture Decision Record

이 문서는 현재 `townpet-springboot`의 장기적인 기술 경계만 기록한다. 구현 순서와 미완료 기능은 [`PLAN.md`](PLAN.md), 목표 구조는 [`docs/TRD.md`](docs/TRD.md), 운영 절차는 `docs/runbooks/`에 둔다.

## 상태 규칙

- `accepted`: 현재 구현과 다음 작업을 구속하는 결정
- `deferred`: 방향은 보류하며, 명시한 trigger가 생길 때 다시 결정한다. 현재 완료 조건이 아니다.
- `superseded`: 새 결정으로 대체된 과거 결정
- `deprecated`: 더 이상 사용하지 않는 결정

`deferred`는 의도적인 범위 조정이지 실패나 누락을 뜻하지 않는다. 구현되지 않은 설계를 `accepted`로 남겨 완료를 과장하지 않는다.

## ADR-0001 - 제품 동등성을 유지하고 내부 구현은 Spring 방식으로 재설계한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

Legacy TownPet의 핵심 화면·URL·권한·상태·반응형 경험은 기준선으로 유지하되 Next.js/Prisma 내부 구현은 Spring Boot 방식으로 교체한다. route가 존재하는 것만으로 동등성을 주장하지 않고 대표 사용자 여정의 동작을 기준으로 한다.

### Consequences

- UI parity와 backend architecture를 분리해 검증한다.
- 모든 Legacy 내부 구현을 그대로 복제하지 않는다.

### Evidence

- `docs/PRD.md`: 재작성 범위와 acceptance criteria
- `docs/parity/matrix.yaml`: 페이지·API 기준선

## ADR-0002 - 도메인별 수직 전환으로 Spring 백엔드를 교체한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

큰 migration branch 대신 하나의 사용자 여정이 frontend·API·transaction·database까지 연결되는 vertical slice로 전환한다. 각 slice는 가까운 검증 후 기능 단위로 commit한다.

### Evidence

- `PLAN.md`: G1~G7 작업 단위
- `AGENTS.md`: slice와 검증 원칙

## ADR-0003 - 실제 Legacy 데이터 migration은 필요할 때 별도 수행한다

- 상태: deferred
- 날짜: 2026-08-11
- 근거 유형: inferred

### Decision

현재 공개 산출물은 합성 demo data를 사용하는 sandbox이므로 실제 Legacy 개인정보 migration은 수행하지 않는다. 실제 데이터 보존이 요구되면 quarantine·mapping·대사·rollback을 포함한 별도 migration goal과 decision을 만든다.

### Trigger

- 실제 Legacy data를 production 또는 fixture로 옮기기로 결정할 때

### Evidence

- `ADR.md`의 기존 migration 결정과 `ADR-0029`
- `deploy/compose/portfolio.yml`: 합성 sandbox 배포 구성

## ADR-0004 - PostgreSQL을 단일 영속 원장으로 유지한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

업무 원장·session·migration history는 PostgreSQL에 둔다. Redis, Kafka, Elasticsearch는 실제 병목이나 외부 계약이 측정될 때만 별도 결정한다.

### Evidence

- `build.gradle.kts`: PostgreSQL/JDBC/Flyway
- `src/main/resources/db/migration/`

## ADR-0005 - Next.js와 Node.js 서버 런타임을 제거한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

Node.js는 frontend 개발·build 도구로만 사용하고, production server 책임은 Spring Boot와 Caddy가 가진다. Next.js, Prisma, NextAuth server runtime은 최종 산출물에 포함하지 않는다.

### Evidence

- `frontend/package.json`
- `deploy/Dockerfile.backend`
- `ADR.md`의 ADR-0040

## ADR-0006 - React, TypeScript와 Vite로 브라우저 UI를 구성한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

React/Vite SPA와 React Router를 사용한다. API 호출은 얇은 `fetch` client와 Spring DTO를 기준으로 유지한다.

### Evidence

- `frontend/package.json`
- `frontend/src/App.tsx`

## ADR-0007 - Spring Modulith 기반 모듈형 모놀리스를 사용한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

하나의 Spring Boot 배포 단위 안에서 business module의 공개 application API와 내부 구현을 분리한다. microservice·Kubernetes는 현재 범위가 아니다.

### Evidence

- `build.gradle.kts`: Spring Modulith
- `src/main/java/com/townpet/*/package-info.java`
- `src/test/java/com/townpet/architecture/ModularityTest.java`

## ADR-0008 - 공통 Publication과 도메인별 Aggregate를 분리한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

일반 커뮤니티 publication과 거래·입양·분실·Care 같은 구조화 aggregate를 같은 entity graph로 섞지 않는다. module 간 연결은 식별자와 공개 API로 한다.

### Evidence

- `src/main/java/com/townpet/publication/`
- `src/main/java/com/townpet/marketplace/`
- `src/main/java/com/townpet/lostfound/`

## ADR-0009 - Spring Security와 PostgreSQL 서버 세션을 사용한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

Browser 인증은 Spring Security·Spring Session JDBC·CSRF를 사용한다. Browser JWT나 NextAuth session 호환은 만들지 않는다.

### Evidence

- `src/main/java/com/townpet/identity/SecurityConfig.java`
- `src/main/java/com/townpet/identity/SessionController.java`
- `src/main/resources/db/migration/V001__platform_baseline.sql`

## ADR-0010 - 비회원 자격과 Abuse Signal을 분리한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

Guest cookie·step-up 자격은 제한된 작업 권한일 뿐 IP/User-Agent만으로 신원을 확정하지 않는다. guest token은 scope·만료·single-use·실패 잠금을 갖는다.

### Evidence

- `src/main/java/com/townpet/identity/GuestStepUpService.java`
- `src/main/resources/db/migration/V036__guest_step_up.sql`

## ADR-0011 - 실제로 필요한 Application Module만 유지한다

- 상태: deferred
- 날짜: 2026-08-11
- 근거 유형: inferred

### Decision

기존의 17개 Bounded Context 목록을 사전 구현 의무로 고정하지 않는다. 현재는 실제 business rule과 data ownership이 있는 module만 유지하고, 분리 이유가 생길 때 추가한다. 빈 package marker만으로 module 완료를 주장하지 않는다.

### Trigger

- module 간 변경 독립성·소유권·transaction 경계가 실제로 생길 때

### Evidence

- `src/main/java/com/townpet/*/package-info.java`
- 현재 `care` package가 marker만 가진다는 코드 상태

## ADR-0012 - OpenAPI 3.1 Contract-first는 superseded 되었다

- 상태: superseded
- 날짜: 2026-08-11
- 근거 유형: explicit

### Decision

별도 OpenAPI generator 계약은 ADR-0020으로 대체한다. 이 항목은 역사 보존용이다.

## ADR-0020 - 별도 OpenAPI 계약 파일과 생성 client를 사용하지 않는다

- 상태: accepted
- 날짜: 2026-08-11
- 근거 유형: explicit

### Decision

현재 규모에서는 Spring controller DTO와 `frontend/src/api/client.ts`를 HTTP 계약의 source로 사용한다. `api/openapi/townpet.yaml`과 generated client를 다시 만들지 않는다.

### Consequences

- 외부 계약 변경 시 해당 controller DTO와 client를 함께 검토한다.
- 실제 제3자 API가 생기면 별도 계약 문서를 재평가한다.

### Evidence

- `frontend/src/api/client.ts`
- `ADR.md`의 기존 ADR-0020 결정

## ADR-0013 - 내구성 module event는 실제 후속 처리 시 도입한다

- 상태: deferred
- 날짜: 2026-08-11
- 근거 유형: inferred

### Decision

현재 동기 transaction으로 충분한 기능에는 event consumer를 만들지 않는다. 여러 module에 걸친 비동기 후속 처리, 재시도 또는 projection이 실제로 필요해질 때 Spring Modulith Event Publication Registry와 idempotent listener를 도입한다.

### Trigger

- 비동기 projection·notification·외부 delivery가 사용자 응답과 분리되어야 할 때

### Evidence

- `src/main/resources/db/migration/V001__platform_baseline.sql`에는 registry table만 있고 listener 구현은 없다.

## ADR-0014 - Media object storage는 production 전환 시 결정한다

- 상태: deferred
- 날짜: 2026-08-11
- 근거 유형: inferred

### Decision

현재 test/e2e는 in-memory storage를 사용하고, 실제 public upload를 켜지 않는다. production media를 열 때 PostgreSQL metadata + S3-compatible storage(R2/MinIO) + presigned upload를 하나의 implementation goal로 도입한다.

### Trigger

- 공개 sandbox에서 실제 image upload를 허용할 때

### Evidence

- `src/main/java/com/townpet/media/UnavailableObjectStorage.java`
- `src/main/java/com/townpet/media/LocalObjectStorage.java`

## ADR-0015 - SearchDocument read model은 검색 병목이 측정될 때 도입한다

- 상태: deferred
- 날짜: 2026-08-11
- 근거 유형: inferred

### Decision

현재 검색은 PostgreSQL 조회로 유지한다. corpus·query plan·latency가 기준을 넘을 때 `tsvector`/GIN/`pg_trgm`, SearchDocument, projection repair를 함께 설계한다.

### Trigger

- 구조화 검색 field가 늘거나 측정된 검색 latency·정확도가 현재 방식으로 부족할 때

### Evidence

- `src/main/java/com/townpet/publication/api/PublicationFeed.java`: 현재 조회 기반 검색
- 현재 migration에 SearchDocument table이 없음

## ADR-0016 - Feed projection과 versioned personalization은 실제 ranking 요구 시 도입한다

- 상태: deferred
- 날짜: 2026-08-11
- 근거 유형: inferred

### Decision

현재 최신·인기 feed는 PostgreSQL 원장과 metric 조회로 제공한다. 사용자별 signal·ranking 품질·query latency를 측정해 필요성이 입증될 때 FeedDocument, ranking version, rebuild와 freshness metric을 추가한다.

### Trigger

- 단순 feed가 기능 요구나 측정 기준을 충족하지 못할 때

### Evidence

- `src/main/java/com/townpet/publication/api/PublicationFeed.java`
- `src/main/java/com/townpet/discovery/BestFeedController.java`

## ADR-0017 - Engagement 원장은 동기 transaction을 우선한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: inferred

### Decision

댓글·반응·북마크와 publication metric의 핵심 불변식은 PostgreSQL transaction과 constraint로 처리한다. 지연 집계 summary는 실제 조회 비용이 측정될 때만 추가한다.

### Evidence

- `src/main/java/com/townpet/engagement/`
- `src/main/resources/db/migration/V008__engagement_comment.sql`
- `src/main/resources/db/migration/V025__publication_metrics.sql`

## ADR-0018 - Publication lifecycle과 visibility restriction을 분리한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

삭제·숨김·공개 lifecycle과 GLOBAL/LOCAL scope, block·moderation 접근 정책을 별도로 표현한다. controller가 아닌 application/data query에서 적용한다.

### Evidence

- `src/main/java/com/townpet/publication/PublicationEntity.java`
- `src/main/java/com/townpet/publication/api/PublicationFeed.java`

## ADR-0019 - Parity는 route inventory와 대표 사용자 여정으로 판정한다

- 상태: accepted
- 날짜: 2026-08-11
- 근거 유형: explicit

### Decision

matrix는 누락 방지용 inventory로 사용하고, 완료 주장은 대표 정상·오류·권한·상태 전이의 API/frontend/browser evidence가 있을 때만 한다. 모든 Legacy 내부 구현과 모든 응답 byte-level 동일성을 완료 조건으로 요구하지 않는다.

### Evidence

- `docs/parity/matrix.yaml`
- `src/test/java/com/townpet/parity/ParityInventoryTest.java`
- `frontend/src/*Flows.test.tsx`

## ADR-0020B - 운영비 상한을 월 1만 원 이하로 둔다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

항상 실행되는 Spring Boot와 PostgreSQL을 저가 단일 VPS에 두고 managed service를 최소화한다. 비용은 실제 계약·환율을 배포 전에 다시 확인한다.

### Evidence

- `deploy/compose/portfolio.yml`
- 사용자 결정: 월 1만 원 이하

## ADR-0021 - Hetzner 직접 운영은 배포 시작 시 확정한다

- 상태: deferred
- 날짜: 2026-08-11
- 근거 유형: explicit

### Decision

Hetzner CX23 topology를 목표 후보로 유지하되, 실제 계정·도메인·DNS·비용을 준비하기 전에는 production 완료로 주장하지 않는다. 단일 VPS는 Caddy·Spring·PostgreSQL을 격리된 container로 실행한다.

### Trigger

- 사용자가 실제 VPS 배포를 시작하고 접속·도메인 정보를 제공할 때

### Evidence

- `deploy/compose/portfolio.yml`
- `docs/report/release-readiness.md`: 실제 VPS 미실행 상태

## ADR-0022 - PostgreSQL 복구 목표는 실제 운영을 시작할 때 확정한다

- 상태: deferred
- 날짜: 2026-08-11
- 근거 유형: inferred

### Decision

현재는 guarded `pg_dump`/`pg_restore`와 local rehearsal만 제공한다. 실제 운영을 열 때 offsite encryption, WAL/PITR, retention, RPO/RTO와 정기 restore drill을 측정 기반으로 확정한다.

### Trigger

- 실제 VPS에 persistent data를 공개하기로 할 때

### Evidence

- `deploy/backup-postgres.sh`
- `deploy/restore-postgres.sh`

## ADR-0023 - 외부 관측 체계는 production 운영 시 도입한다

- 상태: deferred
- 날짜: 2026-08-11
- 근거 유형: inferred

### Decision

현재 Actuator health와 web vital endpoint만 유지한다. 실제 VPS 운영을 시작할 때 JVM·HTTP·DB·host·backup 지표를 측정하고 경량 collector와 외부 backend를 선택한다.

### Trigger

- public VPS에서 장애 감지·성능 추적이 필요해질 때

### Evidence

- `src/main/resources/application.yml`
- `src/main/java/com/townpet/operations/WebVitalMetricController.java`

## ADR-0024 - SLO와 error budget은 측정 후 선언한다

- 상태: deferred
- 날짜: 2026-08-11
- 근거 유형: inferred

### Decision

현재 SLA나 처리량을 선언하지 않는다. representative workload와 실제 network·DB·JVM 측정 후 낮은 비용에 맞는 latency/availability 목표를 문서화한다.

### Trigger

- production endpoint와 데이터 규모가 정해지고 baseline 측정이 가능할 때

### Evidence

- `docs/report/release-readiness.md`: 성능 수치 미측정

## ADR-0025 - Java 25 LTS와 Spring Boot 4.1을 기준선으로 사용한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

Java 25, Spring Boot 4.1, Gradle Wrapper를 사용하고 preview feature는 사용하지 않는다.

### Evidence

- `build.gradle.kts`
- `gradle/wrapper/gradle-wrapper.properties`

## ADR-0026 - JPA write model과 필요한 조회용 jOOQ를 Flyway 위에서 함께 사용한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

aggregate write는 Spring Data JPA를 우선하고 feed·집계·PostgreSQL 특화 조회는 jOOQ/JDBC를 사용한다. Hibernate schema auto-create는 사용하지 않는다.

### Evidence

- `build.gradle.kts`
- `src/main/resources/application.yml`
- `src/main/java/com/townpet/publication/api/PublicationFeed.java`

## ADR-0027 - PostgreSQL 18, UUIDv7, UTC, KRW 정수와 명시적 동시성을 사용한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

식별자는 UUIDv7, 시간은 UTC `Instant`, 금액은 원 단위 정수, 경합 invariant는 unique/check/version/조건부 update로 표현한다.

### Evidence

- `src/main/java/com/townpet/common/UuidV7.java`
- `src/main/resources/db/migration/`

## ADR-0028 - RBAC와 resource ownership을 결합하고 기본 거부한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

인증만으로 resource 접근을 허용하지 않고 역할·소유권·상태를 함께 검사한다. moderator/admin surface는 deny-by-default로 보호한다.

### Evidence

- `src/main/java/com/townpet/identity/SecurityConfig.java`
- `src/main/java/com/townpet/identity/MemberModerationController.java`
- 관련 controller test

## ADR-0029 - 공개 환경은 기능 완전형 Portfolio Sandbox로 운영한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

실제 개인정보 가입·실제 돌봄·실제 결제를 받지 않고 합성 데이터와 제한된 demo actor로 기능을 보여준다. Kakao/Naver는 현재 제공하지 않는다.

### Consequences

- 공개 배포 전 seed gating과 개인정보 노출 점검이 필요하다.

### Evidence

- `ADR.md`의 ADR-0040
- `deploy/portfolio.env.example`

## ADR-0030 - Demo seed reset은 showcase 운영을 시작할 때 추가한다

- 상태: deferred
- 날짜: 2026-08-11
- 근거 유형: inferred

### Decision

현재 demo identity migration은 개발 검증용으로 유지한다. 공개 showcase를 열 때만 seed enablement, 고정 계정, reset 주기와 초기화 절차를 별도 구현한다.

### Trigger

- 실제 public showcase URL을 운영하기로 할 때

### Evidence

- `src/main/resources/db/migration/V003__demo_identity_roles.sql`
- `deploy/compose/portfolio.yml`: 현재 flag는 있으나 application gating은 없음

## ADR-0031 - Marketplace는 결제 없는 Classified Listing으로 한정한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

판매·대여·나눔·group buy listing lifecycle만 제공하고 결제·정산·환불·수수료는 제공하지 않는다.

### Evidence

- `src/main/java/com/townpet/marketplace/`
- `src/main/resources/db/migration/V019__marketplace_listing.sql`

## ADR-0032 - 현재 Marketplace invariant는 DB constraint와 lifecycle로 지킨다

- 상태: accepted
- 날짜: 2026-08-11
- 근거 유형: inferred

### Decision

현재 구현 범위에서는 kind/status/price 조합과 optimistic version을 DB check 및 application transition으로 검증한다. sealed terms value object는 유형 수와 규칙이 늘어날 때 도입한다.

### Evidence

- `src/main/resources/db/migration/V033__marketplace_group_buy_kind.sql`
- `src/main/java/com/townpet/marketplace/MarketplaceListingService.java`

## ADR-0033 - Marketplace safety rule은 corpus와 실제 moderation 요구가 생길 때 도입한다

- 상태: deferred
- 날짜: 2026-08-11
- 근거 유형: inferred

### Decision

현재는 listing lifecycle만 제공하고 금지 품목·연락처·사기 표현의 hard/soft classifier를 완료 조건으로 두지 않는다. 공개 거래를 열기 전 deterministic rule과 moderator signal을 별도 goal로 설계한다.

### Trigger

- 실제 public listing을 허용하거나 금지 corpus가 확보될 때

### Evidence

- 현재 `MarketplaceListingService`에 safety policy가 없다는 코드 상태
- `ADR.md`의 기존 ADR-0033 요구

## ADR-0034 - Care는 결제 없는 이웃 간 coordination으로 한정한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

전문 자격·고용·결제·보험·의료행위를 제공하지 않는다. 실제 Care workflow를 구현할 때만 요청·지원·매칭·진행·완료와 비공개 safety feedback을 다룬다.

### Evidence

- `ADR.md`의 기존 ADR-0034
- 현재 `src/main/java/com/townpet/care/`가 아직 비어 있다는 구현 상태

## ADR-0035 - Care의 Request/Application/Assignment 모델은 구현 시 도입한다

- 상태: deferred
- 날짜: 2026-08-11
- 근거 유형: inferred

### Decision

Care를 다시 시작할 때 Request·Application·Assignment와 단일 active assignment invariant를 하나의 vertical slice로 구현한다. 현재 generic moderator case queue를 Care workflow로 간주하지 않는다.

### Trigger

- Care 공개 사용자 여정을 다시 제품 범위에 넣을 때

### Evidence

- `src/main/java/com/townpet/care/`: package marker만 존재
- Legacy evidence: `/Users/alex/project/townpet/app/prisma/schema.prisma`

## ADR-0036 - SightingReport를 일반 Comment와 분리한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

분실동물 목격 정보는 구조화 report로 저장하고 일반 댓글과 다른 권한·위치 보호 정책을 적용한다.

### Evidence

- `src/main/java/com/townpet/lostfound/LostFoundSightingService.java`
- `src/main/resources/db/migration/V016__lost_found_sighting_report.sql`

## ADR-0037 - 공개 근사 위치와 보호된 정확 위치를 분리한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

공개 응답에는 근사 위치만 제공하고 정확 위치 evidence는 owner authorization과 보호된 저장·감사 경계 안에서만 읽는다.

### Evidence

- `src/main/java/com/townpet/lostfound/LostFoundExactLocationService.java`
- `src/main/resources/db/migration/V017__lost_found_exact_location_evidence.sql`

## ADR-0038 - LostFound 종료에는 결과와 사유를 기록한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

resolved/closed 전환은 outcome과 close reason을 함께 기록하고 상태 이력을 남긴다.

### Evidence

- `src/main/java/com/townpet/lostfound/LostFoundAlertService.java`
- `src/main/resources/db/migration/V015__lost_found_alert_lifecycle.sql`

## ADR-0039 - Quality gate는 위험도에 따라 계층화한다

- 상태: accepted
- 날짜: 2026-08-11
- 근거 유형: explicit

### Decision

작업 중에는 가장 가까운 검증을 실행하고 commit·완료 주장 전에 변경 위험에 맞는 gate를 실행한다. 모든 작은 변경마다 full gate, OpenAPI lint, differential test를 반복하지 않는다.

### Evidence

- `AGENTS.md`의 검증 규칙
- `.github/workflows/main.yml`
- `build.gradle.kts`

## ADR-0040 - 현재 인증 범위를 Credentials로 한정한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

현재 회원가입·로그인은 이메일/비밀번호 Credentials와 Spring Session으로만 제공한다. Kakao/Naver OAuth, social account link/unlink, NextAuth 호환 endpoint는 구현하지 않는다. 필요해질 때 별도 product/security decision을 만든다.

### Evidence

- `src/main/java/com/townpet/identity/SessionController.java`
- `docs/parity/matrix.yaml`: ADR-0040 제외 항목
- `docs/PRD.md`: 현재 social login 제외 범위

## 현재 적용 순서

1. 현재 accepted 결정과 실제 코드가 어긋나는 항목을 먼저 수정한다.
2. Care·구조화 게시물·검색·media 중 하나를 선택해 큰 vertical slice로 진행한다.
3. deferred 항목은 trigger가 생기기 전까지 PLAN의 완료 조건에 넣지 않는다.
4. 실제 VPS 공개를 시작할 때만 ADR-0021~0024와 ADR-0030을 다시 accepted로 승격한다.
