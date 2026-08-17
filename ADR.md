# Architecture Decision Record

이 문서는 현재 `townpet-springboot`의 장기적인 기술 경계만 기록한다. 구현 순서와 미완료 기능은 [`PLAN.md`](PLAN.md), 목표 구조는 [`docs/01-기준/기술-요구사항.md`](docs/01-기준/기술-요구사항.md), 운영 절차는 `docs/09-운영-가이드/`에 둔다.

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

- `docs/01-기준/제품-요구사항.md`: 재작성 범위와 acceptance criteria
- `docs/05-패리티/대조표.yaml`: 페이지·API 기준선

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

## ADR-0013 - 외부 account delivery는 PostgreSQL 기반 내구성 전달로 처리한다

- 상태: accepted
- 날짜: 2026-08-11
- 근거 유형: explicit

### Decision

이메일 인증·비밀번호 재설정처럼 외부 SMTP에 전달해야 하는 account token은 transaction commit 이후 Spring Modulith Event Publication Registry와 PostgreSQL outbox 상태를 사용한다. consumer는 idempotent하게 재시도하며 raw token·credential을 event나 log에 넣지 않는다. 이 결정은 Kafka를 도입한다는 뜻이 아니며, 단순한 내부 동기 흐름에는 event consumer를 만들지 않는다.

### Trigger

- SMTP 외부 전달 실패를 재시도·관찰해야 할 때
- PostgreSQL registry로 처리할 수 없는 외부 consumer·처리량이 생길 때는 별도 ADR을 만든다.

### Evidence

- `src/main/java/com/townpet/identity/AccountTokenDeliveryListener.java`
- `src/main/java/com/townpet/identity/AccountTokenCipher.java`
- `src/main/resources/db/migration/V001__platform_baseline.sql`의 event publication registry

## ADR-0014 - Production media는 private MinIO와 presigned URL을 사용한다

- 상태: accepted
- 날짜: 2026-08-11
- 근거 유형: explicit

### Decision

PostgreSQL은 upload metadata와 lifecycle의 source of truth로 유지하고, production object는 private S3-compatible MinIO에 둔다. Browser는 짧은 만료의 presigned PUT으로 직접 업로드하고 server finalize·checksum·magic byte 검증을 통과한 뒤에만 publication에 연결한다. private media는 권한이 확인된 요청에만 짧은 signed GET을 발급한다. local/test/e2e는 기존 filesystem/in-memory adapter를 계속 사용할 수 있다.

### Trigger

- public 또는 member image upload를 production에서 허용할 때

### Evidence

- `src/main/java/com/townpet/media/UnavailableObjectStorage.java`
- `src/main/java/com/townpet/media/LocalObjectStorage.java`
- `src/main/java/com/townpet/media/MinioObjectStorage.java`
- `deploy/Caddyfile`, `frontend/src/api/client.ts`

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

삭제·숨김 lifecycle과 block·moderation 접근 정책을 별도로 표현한다. 일반 publication에는 지역 공개 범위를 두지 않고 모든 active 글을 동일한 공개 피드에서 조회하며, controller가 아닌 application/data query에서 lifecycle·관계 정책을 적용한다.

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

- `docs/05-패리티/대조표.yaml`
- `src/test/java/com/townpet/parity/ParityInventoryTest.java`
- `frontend/src/*Flows.test.tsx`

## ADR-0020B - 운영비 상한을 월 1만 원 이하로 둔다

- 상태: superseded
- 날짜: 2026-08-10
- 근거 유형: explicit
- 대체: ADR-0024

### Decision

항상 실행되는 Spring Boot와 PostgreSQL을 저가 단일 VPS에 두고 managed service를 최소화한다. 최초 목표는 월 1만 원 이하였으나, 두 프로젝트를 동시에 실행할 때 필요한 8GB 메모리와 외부 백업을 고려해 후속 ADR에서 현실적인 예산으로 조정했다.

### Evidence

- `deploy/compose/portfolio.yml`
- 사용자 결정: 월 1만 원 이하

## ADR-0021 - Hetzner 직접 운영은 배포 시작 시 확정한다

- 상태: superseded
- 날짜: 2026-08-11
- 근거 유형: explicit
- 대체: ADR-0024

### Decision

Hetzner CX23 topology를 목표 후보로 유지하되, 실제 계정·도메인·DNS·비용을 준비하기 전에는 production 완료로 주장하지 않는다. 단일 VPS는 Caddy·Spring·PostgreSQL을 격리된 container로 실행한다. 이후 비용·메모리·x86 호환성을 다시 비교해 ADR-0024로 대체했다.

### Trigger

- 사용자가 실제 VPS 배포를 시작하고 접속·도메인 정보를 제공할 때

### Evidence

- `deploy/compose/portfolio.yml`
- `docs/08-면접-복기/릴리스-준비도.md`: 실제 VPS 미실행 상태

## ADR-0024 - 두 포트폴리오 프로젝트를 netcup x86 VPS Lite 2에 함께 배포한다

- 상태: accepted
- 날짜: 2026-08-16
- 근거 유형: explicit

### Context

TownPet과 `kindergarten-erp/erp`를 상용 트래픽이 거의 없는 포트폴리오 환경에서 24시간 공개한다. 두 프로젝트는 각각 Spring Boot와 데이터베이스를 필요로 하므로 4GB 단일 VPS보다 8GB 단일 VPS가 안전하지만, 프로젝트별 VPS나 managed DB는 현재 목적과 비용에 비해 과하다.

### Decision

netcup VPS Lite 2 G12s(4 vCore, 8GB RAM, 160GB SSD, x86)를 기본 배포 대상으로 선택한다. 공용 Caddy 하나만 80/443을 사용하고, TownPet은 PostgreSQL/PostGIS·MinIO, ERP는 MySQL·Redis를 각각 별도 Compose·volume·network로 실행한다. 이미지는 GitHub Actions에서 빌드해 GHCR에서 pull하고, VPS에서는 실행만 한다. Cloudflare Free는 DNS·proxy·SSL에, Resend SMTP 무료 구간은 TownPet·ERP 이메일에 사용한다. DB와 MinIO 백업은 암호화해 VPS 외부 failure domain에 보관한다.

### Alternatives

- Hetzner CX33: 관리 편의성과 API는 좋지만 IPv4·VAT를 포함하면 netcup보다 비용 이점이 작다.
- OVH VPS-2: 일일 백업이 포함되지만 provider backup만으로 외부 백업 요구를 대체하지 않으며, 최종 가격·리전 조건을 별도 확인해야 한다.
- 4GB VPS: 저트래픽에서는 동작할 수 있으나 두 JVM·PostgreSQL·MySQL·MinIO를 동시에 운영할 메모리 여유가 부족하다.
- 프로젝트별 VPS·managed DB: failure domain은 좋아지지만 포트폴리오 트래픽과 비용 제약에 비해 과하다.

### Consequences

- x86을 사용하므로 ARM64 이미지 호환성 확인 부담이 줄어든다.
- 단일 VPS 장애가 두 프로젝트에 동시에 영향을 준다. 외부 encrypted backup과 restore rehearsal로 완화한다.
- 공용 Caddy가 필요하므로 기존 각 프로젝트의 80/443 공개 설정을 내부 upstream 방식으로 변경해야 한다.
- 실제 공개 트래픽이 증가하거나 가용성 요구가 생기면 프로젝트 분리·managed DB를 재평가한다.

### Evidence

- `deploy/compose/netcup.yml`, `deploy/compose/edge.yml`, `deploy/compose/Caddyfile.netcup`: TownPet image-pull·공용 edge 구성
- `deploy/Caddyfile.netcup.web`: edge 뒤 내부 HTTP-only web proxy 구성
- `docs/09-운영-가이드/두-프로젝트-VPS-배포-워크플로.md`: 실제 실행 순서와 검증 기준
- `kindergarten-erp/erp/deploy/docker-compose.netcup.yml`: ERP MySQL·Redis·app·내부 Caddy 구성
- [netcup VPS Lite 공식 가격·사양](https://www.netcup.com/en/server/vps-lite)

### Open Questions

- 실제 가입 시 선택 가능한 netcup 위치와 한국에서의 latency
- netcup 공인 IPv4의 최종 과금·약정 조건
- 외부 암호화 백업 저장소의 최종 provider와 retention 비용

## ADR-0022 - 배포 전 기본 백업·복구를 제공하고 고급 RPO는 운영 후 확정한다

- 상태: accepted
- 날짜: 2026-08-11
- 근거 유형: inferred

### Decision

배포 전 PostgreSQL logical backup과 MinIO object backup을 같은 backup id로 묶고, 암호화된 외부 복사·checksum·fresh volume restore rehearsal을 제공한다. WAL/PITR, 다중 replica와 엄격한 RPO/RTO는 실제 데이터 규모와 운영 지표를 얻은 뒤 별도 decision으로 확정한다.

### Trigger

- public persistent data를 최초로 노출하기 전에는 기본 backup·restore를 완료해야 한다.

### Evidence

- `deploy/backup-portfolio.sh`
- `deploy/restore-portfolio.sh`

## ADR-0023 - 배포 전 최소 관측성을 제공하고 외부 backend는 운영 후 선택한다

- 상태: accepted
- 날짜: 2026-08-11
- 근거 유형: inferred

### Decision

Actuator health/readiness, correlation id가 있는 구조화 log, JVM·HTTP·DB pool·disk·MinIO·backup·SMTP 실패 확인을 배포 전에 제공한다. Prometheus/Grafana/Sentry 같은 외부 backend와 장기 보존·분산 tracing은 실제 운영 비용과 신호가 확인된 뒤 선택한다.

### Trigger

- 외부 모니터링 backend가 필요해지는 규모의 운영 신호가 생길 때

### Evidence

- `src/main/resources/application.yml`
- `src/main/java/com/townpet/operations/WebVitalMetricController.java`
- `src/main/java/com/townpet/common/web/RequestTraceFilter.java`
- `docs/09-운영-가이드/관측성.md`

## ADR-0024 - SLO와 error budget은 측정 후 선언한다

- 상태: deferred
- 날짜: 2026-08-11
- 근거 유형: inferred

### Decision

현재 SLA나 처리량을 선언하지 않는다. representative workload와 실제 network·DB·JVM 측정 후 낮은 비용에 맞는 latency/availability 목표를 문서화한다.

### Trigger

- production endpoint와 데이터 규모가 정해지고 baseline 측정이 가능할 때

### Evidence

- `docs/08-면접-복기/릴리스-준비도.md`: 성능 수치 미측정

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

## ADR-0029 - 공개 환경은 합성 demo 계정과 콘텐츠를 제공하는 Portfolio Showcase로 운영한다

- 상태: accepted
- 날짜: 2026-08-10
- 근거 유형: explicit

### Decision

실제 개인정보 가입·실제 돌봄·실제 결제를 받지 않는다. 공개 환경에는 합성 MEMBER 3개와 제한된 MODERATOR 1개를 제공하고, 게시글·댓글·답글·추천·북마크와 주요 게시판의 합성 콘텐츠를 함께 노출한다. 방문자는 공개된 자격으로 일반 사용자 흐름을 직접 확인할 수 있지만 ADMIN·OPERATOR 자격과 실제 개인정보는 공개하지 않는다. Kakao/Naver는 현재 제공하지 않는다.

### Consequences

- 공개 배포 시 demo fixture seed와 reset 절차를 함께 제공하고, demo 범위를 벗어난 데이터는 만들지 않는다.

### Evidence

- `ADR.md`의 ADR-0040
- `deploy/portfolio.env.example`

## ADR-0030 - Demo fixture는 공개 showcase에서 재실행 가능한 합성 데이터로 운영한다

- 상태: accepted
- 날짜: 2026-08-11
- 근거 유형: inferred

### Decision

기존 Flyway identity migration은 유지하고, 공개 배포 후 `local-demo.sql`과 `local-community-demo.sql`을 scoped fixture로 실행한다. fixture는 고정 ID 범위만 삭제 후 재생성하므로 VPS 재기동·데모 복구 시 재실행할 수 있다. 자동 reset cron은 사용하지 않고 운영자가 명시적으로 seed script를 실행한다.

### Trigger

- 공개 showcase DB를 처음 채우거나 demo 콘텐츠를 갱신할 때

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

- `src/main/java/com/townpet/care/`
- `src/main/resources/db/migration/V043__care_request.sql` ~ `V045__care_assignment.sql`
- `src/test/java/com/townpet/care/CareControllerTest.java`

## ADR-0035 - Care의 Request/Application/Assignment 모델은 구현 시 도입한다

- 상태: accepted
- 날짜: 2026-08-11
- 근거 유형: inferred

### Decision

Care는 Request·Application·Assignment·Feedback과 단일 active assignment invariant를 하나의 vertical slice로 구현한다. generic moderator case queue와 분리하고, 결제·전문 자격·의료행위 없이 이웃 간 coordination만 제공한다.

### Trigger

- Care 공개 사용자 여정을 다시 제품 범위에 넣을 때

### Evidence

- `src/main/java/com/townpet/care/`
- `src/main/resources/db/migration/V043__care_request.sql` ~ `V045__care_assignment.sql`
- `src/test/java/com/townpet/care/CareControllerTest.java`

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
- `docs/05-패리티/대조표.yaml`: ADR-0040 제외 항목
- `docs/01-기준/제품-요구사항.md`: 현재 social login 제외 범위

## 현재 적용 순서

1. 현재 accepted 결정과 실제 코드가 어긋나는 항목을 먼저 수정한다.
2. Care·구조화 게시물·검색·media 중 하나를 선택해 큰 vertical slice로 진행한다.
3. deferred 항목은 trigger가 생기기 전까지 PLAN의 완료 조건에 넣지 않는다.
4. 실제 VPS 공개 전에는 ADR-0024의 netcup topology·DNS 결정을 따르고, ADR-0022·0023·0030의 최소 운영 구현을 완료한다.
