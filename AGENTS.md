# AGENTS.md

## 1. 작업 시작

다음 문서를 필요한 범위만 읽는다.

1. [`PLAN.md`](PLAN.md): 지금 실행할 slice
2. [`docs/PRD.md`](docs/PRD.md): 해당 기능의 제품 동작과 완료 조건
3. [`docs/TRD.md`](docs/TRD.md), [`ADR.md`](ADR.md): 관련 구현 제약과 결정 근거
4. [`GLOSSARY.md`](GLOSSARY.md): 관련 상태·용어 의미

매 작업마다 긴 문서를 처음부터 다시 요약하지 않는다. 충돌 시 제품 범위는 PRD, 구현 제약은 accepted ADR과 TRD, 작업 순서는 PLAN을 따른다. 해석이 결과를 크게 바꾸는 충돌만 구현 전에 문서로 해소한다.

작업 대상은 현재 저장소 `/Users/alex/project/townpet-springboot`로 한정한다. Downloads, Desktop, 다른 앱 데이터와 외부 프로젝트 경로에는 접근하지 않는다. Legacy 기준선은 외부 경로에 남아 있지만, 사용자가 별도로 요청하지 않는 한 읽지도 수정하지도 않고 저장소 안의 parity·문서·fixture만 사용한다.

## 2. 현재 상태

P1 application foundation과 P2 Identity·Member·Catalog의 초기 vertical slice가 구현됐다. Gradle/Spring Boot, PostgreSQL·Flyway, Modulith 경계, React·Vite shell, parity inventory, CI/smoke, session login·logout·onboarding·demo role까지 실제 검증이 있다. 현재 작업과 남은 범위는 `PLAN.md`만 기준으로 삼는다.

## 3. 고정 기술 경계

- Java 25, Spring Boot 4.1, Spring Framework 7, Spring Modulith 2.1, Gradle Wrapper를 사용한다. Java preview feature는 사용하지 않는다.
- Frontend는 React 19, TypeScript, Vite, React Router다. Node.js는 개발·build 도구이며 production server가 아니다.
- Next.js, Prisma, NextAuth와 Vercel server runtime을 최종 artifact에 포함하지 않는다.
- PostgreSQL 18·PostGIS 3.6이 source of truth다. Flyway만 schema를 변경하고 Hibernate는 `ddl-auto=validate`를 사용한다.
- 단순 write/read는 Spring Data JPA, 검증된 복잡 read model은 jOOQ를 사용한다. 사용 근거 없이 같은 조회를 이중 구현하지 않는다.
- Browser 인증은 Spring Security·Spring Session JDBC와 CSRF를 사용한다. Browser용 JWT를 만들지 않는다.
- 외부 HTTP 계약은 Spring controller·request/response DTO와 frontend API client가 직접 소유한다. 별도 OpenAPI 파일·생성 client는 사용하지 않는다.
- Module 간 JPA association, entity·repository·controller DTO 노출과 순환 의존을 금지한다. 공개 application API, 식별자 또는 event로 연결한다.
- 비동기 후속 처리는 Spring Modulith Event Publication Registry와 PostgreSQL을 사용하며 consumer는 idempotent하게 만든다.
- Redis, Kafka, Elasticsearch, Kubernetes, microservice는 실측 요구와 새 ADR 없이는 추가하지 않는다.
- 공개 배포는 합성 demo data만 쓰는 portfolio sandbox다. 실제 공개 가입·개인정보 수집을 켜지 않는다. Kakao·Naver 인증은 현재 제품 범위가 아니다.

## 4. 구현 방식

1. `PLAN.md`의 첫 미완료 slice 하나를 고른다.
2. 관련 Legacy page·route·validation·schema·test만 읽고 관찰 가능한 계약을 확인한다.
3. domain/application/data/web/frontend가 연결되는 충분히 큰 기능 vertical slice로 구현한다. 기능을 인위적으로 작은 작업으로 분할하지 않는다.
4. Business rule은 application/domain과 DB constraint에 두고 controller·React component에 숨기지 않는다.
5. 권한·상태·오류처럼 실제 회귀 위험이 큰 경계만 필요한 테스트로 확인한다. 테스트 수를 늘리기 위한 테스트와 과도한 contract/e2e 조합을 만들지 않는다.
6. 구현을 먼저 빠르게 진행하고, 작업 중에는 가장 가까운 최소 검증만 실행한다. 전체 gate는 큰 phase가 끝났거나 배포 직전에 한 번 실행한다.
7. 중요한 설계 변화나 재사용 가능한 실패가 있을 때만 `docs/report/`의 기존 문서를 갱신한다.
8. 완료된 slice는 `PLAN.md`에서 제거하고 다음 실행 항목을 맨 앞에 둔다. PLAN을 완료 이력 문서로 사용하지 않는다.

한 commit은 기능 vertical slice 또는 의미 있는 phase 결과를 중심으로 유지한다. 혼자 개발하는 프로젝트이므로 사용 사례가 요구하지 않는 interface, 계층, event, 추상화와 운영 구성은 미리 만들지 않는다.

## 5. 문서 운영

문서마다 책임을 하나만 둔다.

- `docs/PRD.md`: 제품 범위와 acceptance criteria
- `docs/TRD.md`: 목표 기술 구조와 품질 요구
- `ADR.md`: 오래 유지할 결정과 대안
- `GLOSSARY.md`: 용어·상태 의미
- `PLAN.md`: 현재와 다음 실행 작업만
- `docs/parity/`: legacy 대비 동작·화면 coverage
- `docs/report/README.md`: 포트폴리오 문서 index와 증거 상태
- `docs/report/engineering-story.md`: 실제로 방향이 바뀐 사건과 trade-off
- `docs/report/technical-notes.md`: 현재 코드로 설명 가능한 핵심 개념과 빈틈
- `docs/performance/`, `docs/runbooks/`: 실제 측정·운영 evidence가 생겼을 때만 생성

### 기록할 때

아래 중 하나에 해당할 때만 report를 갱신한다.

- 예상과 다른 test·migration·운영 결과의 root cause를 찾았다.
- 보안, transaction, data ownership, module boundary 같은 중요한 경계를 실제 구현했다.
- 현실적인 대안과 trade-off를 비교해 선택했다.
- 성능, query plan, 동시성, migration, 복구의 재현 가능한 수치가 생겼다.
- 큰 사용자 여정이나 phase가 끝나 전체 설명이 달라졌다.

단순 CRUD·DTO·파일 추가, format, dependency 변경, 통과한 test 목록, commit 요약은 별도 report를 만들지 않는다. 같은 내용을 여러 문서에 복사하지 않고 canonical 문서를 링크한다. 새 report 파일은 기존 문서에 넣기 어렵고 반복 참고 가치가 있을 때만 만든다. 문서화를 위해 의도적으로 실패를 만들거나, 구현하지 않은 기능·측정하지 않은 성능을 경험처럼 쓰지 않는다.

면접 자료는 `상황 → 선택 → 구현 근거 → 검증 → trade-off·한계` 순서로 설명할 수 있으면 충분하다. 30초·2분·deep-dive 답변과 question bank는 핵심 domain phase가 끝난 뒤 선별해 만든다.

## 6. 코드 배치

- `src/main/java/com/townpet/<module>/`: business module. 공개 application API 외 구현은 가능하면 `internal`에 둔다.
- `src/main/java/com/townpet/common/`: 기술 공통 요소만 둔다. User·Post 같은 business shared model을 만들지 않는다.
- `src/main/resources/db/migration/`: append-only Flyway migration. 적용된 migration을 수정하지 않는다.
- HTTP 계약은 각 controller와 frontend API 타입에 둔다. 별도 계약 파일을 만들지 않는다.
- `frontend/src/features/`: 사용자 여정별 UI와 상태. 생성 client 밖에서 wire DTO와 URL을 반복 정의하지 않는다.
- `migration/`: 재실행 가능한 ETL·mapping·익명 fixture.
- `deploy/`: Compose, Caddy, IaC, backup·deployment 자동화. Secret을 commit하지 않는다.

## 7. 데이터와 보안

- 식별자는 UUIDv7, 시각은 UTC `Instant`, 금액은 KRW 정수 `bigint`를 기본으로 한다.
- FK, unique, check, optimistic version으로 불변식을 표현하고 중요한 동시성 정책을 test한다.
- 비회원 작성은 `GuestPrincipal`과 범위 제한 자격을 사용한다. IP·User-Agent는 신원이나 단독 authorization 근거가 아니다.
- Staff는 deny-by-default RBAC와 resource ownership을 함께 검사한다.
- LostFound 정확 위치·연락 증거는 암호화하고 공개 응답·log·projection에는 근사 정보만 둔다.
- 실제 Legacy data를 fixture로 commit하지 않는다. Invalid migration row는 quarantine하고 조용히 버리지 않는다.
- Credential, session, account recovery token, 관리 자격, 정확 위치와 개인정보를 log·trace·metric·error에 남기지 않는다.

## 8. 검증

기본 gate:

```bash
./gradlew clean check migrationTest
corepack pnpm -C frontend install --frozen-lockfile
corepack pnpm -C frontend typecheck
corepack pnpm -C frontend test
corepack pnpm -C frontend build
corepack pnpm -C frontend test:e2e
./scripts/frontend-backend-smoke.sh
```

매 작은 수정마다 전체 gate·문서 갱신·브라우저 E2E를 반복하지 않는다. 구현 중에는 컴파일 또는 가장 가까운 기능 테스트 하나만 우선 실행한다. report 문서는 중요한 설계 변화·실패 원인·phase 종료 때만 갱신한다. 전체 gate는 큰 phase 종료 또는 배포 전 한 번 실행한다. 실행하지 않은 검증은 실행했다고 말하지 않는다.

기능 완료는 사용자에게 필요한 정상 흐름과 핵심 실패 경계가 연결된 상태를 뜻한다. 모든 가능한 조합의 테스트·문서·E2E를 만들 필요는 없다. Migration·운영에 영향을 주는 phase만 대사, metric, runbook, rollback·restore evidence를 추가한다. 전체 프로젝트 완료 주장은 별도의 최종 release gate에서만 한다.
