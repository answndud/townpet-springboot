# AGENTS.md

## 1. 작업을 시작하기 전에

다음 문서를 순서대로 읽는다.

1. [`docs/PRD.md`](docs/PRD.md): 유지해야 할 제품 동작과 완료 조건
2. [`docs/TRD.md`](docs/TRD.md): 목표 architecture와 구현 제약
3. [`ADR.md`](ADR.md): 결정 근거와 허용하지 않은 대안
4. [`GLOSSARY.md`](GLOSSARY.md): ubiquitous language와 상태 의미
5. [`PLAN.md`](PLAN.md): 현재 실행할 slice

충돌이 있으면 제품 범위는 PRD, 구현 제약은 accepted ADR과 TRD, 작업 순서는 PLAN을 따른다. 번호가 더 큰 accepted ADR이 앞선 결정을 명시적으로 대체하면 이후 ADR이 우선한다. 임의로 해석하기 어려운 충돌은 구현 전에 새 ADR 또는 문서 수정으로 해소한다.

Legacy 기준선은 `/Users/alex/project/townpet`의 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`다. Legacy repository는 요구사항·migration input·differential/visual baseline으로만 읽고, 사용자가 별도로 요청하지 않는 한 수정하지 않는다.

## 2. 현재 상태

현재 repository에는 설계 문서만 있고 application scaffold는 없다. 다음 작업은 `PLAN.md`의 P1.1이다. `gradlew`, Spring source, frontend manifest가 생성되기 전에는 build나 test가 가능하다고 주장하지 않는다.

## 3. 변경할 수 없는 기술 경계

- Runtime은 Java 25, Spring Boot 4.1, Spring Framework 7, Spring Modulith 2.1과 Gradle 9 Wrapper를 사용한다. Java preview feature는 사용하지 않는다.
- Frontend는 React 19, TypeScript, Vite와 React Router를 사용한다. Node.js는 개발·build 도구일 뿐 production server가 아니다.
- Next.js, Prisma, NextAuth와 Vercel server runtime을 최종 artifact에 포함하지 않는다.
- PostgreSQL 18과 PostGIS 3.6이 durable source of truth다. Flyway만 schema를 변경하며 Hibernate는 `ddl-auto=validate`를 사용한다.
- Command/write model은 Spring Data JPA, 복잡한 read model은 jOOQ를 사용한다. 모든 read를 jOOQ로 옮기거나 단순 조회까지 이중 구현하지 않는다.
- Server session은 Spring Security와 Spring Session JDBC를 사용한다. Browser 인증용 JWT를 만들지 않는다. State-changing request는 CSRF 방어를 통과해야 한다.
- 외부 HTTP 계약은 `api/openapi/townpet.yaml`의 OpenAPI 3.1이 권위다. Java transport와 TypeScript client를 생성하되 domain·entity·repository는 생성하지 않는다.
- Module 간 JPA association, entity·repository·controller DTO 노출과 순환 의존을 금지한다. 다른 module은 공개 application API, 식별자 또는 event로만 연결한다.
- 비동기 후속 처리는 Spring Modulith Event Publication Registry와 PostgreSQL을 사용한다. Consumer는 at-least-once delivery를 전제로 idempotent해야 한다.
- Redis, Kafka, Elasticsearch, Kubernetes와 microservice 분리는 실측 요구와 새 ADR 없이 추가하지 않는다.
- 공개 배포는 portfolio showcase sandbox다. 실제 공개 회원가입·실 OAuth를 켜거나 개인정보를 수집하지 않는다.

## 4. 구현 방식

1. `PLAN.md`에서 가장 앞선 미완료 slice 하나를 선택한다.
2. 해당 기능의 Legacy page, route, validation, schema와 test를 읽어 관찰 가능한 계약을 기록한다.
3. `domain → application → infrastructure → web → frontend`가 연결된 작은 vertical slice로 구현한다.
4. Database constraint와 authorization을 application validation과 함께 둔다. Controller나 React component에 business rule을 숨기지 않는다.
5. Parity matrix에 actor, fixture, 권한, 상태, 오류, responsive, accessibility, SEO, migration과 자동 test를 연결한다.
6. 관련 검증을 실행하고 성공 출력 또는 실패 원인을 증거로 남긴다.
7. 구현 중 생긴 중요한 문제·판단·학습을 `docs/report/`에 즉시 반영한다.
8. 완료 조건을 충족한 slice는 `PLAN.md`에서 제거하거나 다음 slice가 선명하도록 축약한다. 미래 세부사항을 완료된 작업처럼 기록하지 않는다.

한 PR은 하나의 문제와 검증 가능한 결과를 중심으로 작게 유지한다. 생성 파일, format 결과와 migration은 의도한 source 변경과 분리해 review할 수 있게 한다.

## 5. 면접·학습 보고서 운영

`docs/report/`는 완성 후 작성하는 회고문이 아니라 개발 과정에서 축적하는 면접 준비 자료다. 단순 작업 일지나 기술 사전이 아니라 `제품 이해 → 문제 발견 → 판단 → 구현 → 검증 → 학습 → 면접 답변`을 연결해야 한다. Canonical 요구사항과 결정은 PRD·TRD·ADR에 남기고, report는 이를 이해하고 설명하기 위한 evidence-backed projection으로 유지한다.

### 5.1 문서 구조

필요한 문서만 만들되 다음 구조를 기준으로 한다.

```text
docs/report/
├── README.md                 # 전체 index, coverage, 면접 준비도와 빈틈
├── project-overview.md       # 제품 목적, 사용자, 핵심 여정과 전체 architecture 설명
├── feature-map.md            # 49 pages·55 APIs·domain 기능, 상태·권한·예외 연결
├── architecture-walkthrough.md # 대표 request·transaction·event·data·failure 흐름
├── evolution/                # 문제와 한계를 계기로 바뀐 과정
├── knowledge/                # Java·Spring·DB·보안·테스트·운영 개념 정리
└── interview/
    ├── question-bank.md      # 예상 질문, 단계별 답변과 꼬리 질문
    ├── story-bank.md         # 문제 해결·실패·trade-off 사례
    └── gaps.md               # 근거가 약하거나 아직 답하지 못하는 항목
```

빈 폴더와 내용 없는 문서를 미리 대량 생성하지 않는다. `README.md`를 index로 사용하고 실제 근거가 생길 때 해당 문서를 만든다. 같은 설명을 여러 파일에 복사하지 말고 canonical 문서를 링크한다.

### 5.2 반드시 기록하는 시점

다음 사건이 발생한 작업에서는 report 갱신을 완료 조건에 포함한다.

- 새로운 domain invariant, 상태 전이, 권한 또는 개인정보 경계를 구현했을 때
- 기술이나 library를 도입·교체·제거하거나 module/API/data 경계를 바꿨을 때
- 처음 접근이 실패했거나 test, migration, 성능, 동시성, 보안 검증에서 예상과 다른 결과가 나왔을 때
- N+1, lock contention, 중복 event, stale projection, IDOR, CSRF, data mismatch 같은 실제 한계를 발견하고 개선했을 때
- 측정 결과 때문에 index, query, transaction, cache, retry 또는 배포 전략을 변경했을 때
- production 배포, 장애, rollback, restore drill과 운영 자동화에서 학습이 생겼을 때
- PLAN phase를 닫거나 PRD/TRD/ADR의 큰 경계가 달라졌을 때

작은 rename, format, 기계적 dependency update처럼 면접·개념 학습 가치가 없는 변경은 별도 보고서를 만들지 않는다. 반대로 중요한 발견은 phase 종료까지 미루지 말고 같은 slice에서 기록한다.

### 5.3 변화 과정 기록 형식

`docs/report/evolution/EV-NNN-<slug>.md`에는 최소한 다음 내용을 포함한다.

1. 시점과 범위: 관련 PLAN slice, domain, commit 또는 PR
2. 출발 상태: 변경 전 구조와 그 구조를 선택했던 이유
3. Trigger: 실제 증상, 실패한 test, metric, query plan, 운영 제약 또는 Legacy 근거
4. 한계의 원인: 재현 조건과 기술적 root cause
5. 대안: 최소 2개, 선택하지 않은 이유와 당시 제약
6. 결정과 구현: 바뀐 책임, transaction, schema, API와 핵심 code path
7. 검증: 실행한 명령, test 이름, 전후 수치, 실패·복구 결과
8. 결과와 trade-off: 좋아진 점, 새 비용, 남은 위험과 되돌릴 조건
9. 학습: 다음 설계에서 재사용할 원칙과 다시 한다면 바꿀 점
10. 면접 답변: 30초 요약, 2분 설명, 예상 꼬리 질문

변화의 출처를 다음 중 하나로 명시한다.

- `planned-upfront`: 요구사항·보안·운영 제약 때문에 처음부터 선택
- `legacy-derived`: 기존 TownPet의 코드·장애·복잡성에서 확인한 한계에 대응
- `implementation-discovery`: Spring 재구현 도중 test나 설계 검토로 발견
- `measurement-driven`: 성능·부하·운영 지표를 측정한 뒤 개선
- `incident-driven`: 배포 또는 장애와 복구 과정에서 개선

사전 설계를 사후 장애 대응처럼 서술하지 않는다. 면접용 서사를 만들기 위해 의도적으로 취약하거나 비효율적인 구현을 먼저 만들지 않는다. 실제 chronology와 evidence가 없는 “문제가 생겨 도입했다”, “성능이 개선됐다”, “대규모 트래픽을 처리했다” 같은 표현을 금지한다.

### 5.4 기술 지식 기록 형식

`docs/report/knowledge/`는 프로젝트에서 실제 사용하거나 비교·검증한 개념만 다룬다. Java/JVM, Spring, Modulith·DDD, HTTP·OpenAPI, PostgreSQL·JPA·jOOQ, transaction·concurrency, security, event·idempotency, testing, observability, deployment와 frontend integration을 빠뜨리지 않도록 `README.md` coverage에서 추적한다.

각 주제는 다음 질문에 답해야 한다.

- 무엇이며 어떤 문제를 해결하는가?
- 내부에서 어떻게 동작하는가? 주요 lifecycle과 failure mode는 무엇인가?
- TownPet의 어느 기능·code·schema·test에서 사용했는가?
- 왜 이 프로젝트의 제약에서 적합했으며 어떤 대안을 버렸는가?
- 잘못 사용하면 어떤 문제가 생기며 우리는 어떻게 검증·관측하는가?
- 요구 규모나 제약이 달라지면 언제 다른 선택으로 바꿀 것인가?
- 면접관이 `왜?`, `어떻게?`, `실패하면?`, `수치는?`를 반복해도 어떤 evidence로 답할 것인가?

일반론을 길게 복사하지 않는다. 공식 문서나 신뢰할 수 있는 자료를 링크하고, 프로젝트 적용·실패 사례·수치와 자신의 설명을 중심으로 작성한다. 외부 자료의 문장을 그대로 가져오지 말고 출처와 확인 날짜를 남긴다.

### 5.5 제품·기능 이해 보존

`project-overview.md`, `feature-map.md`, `architecture-walkthrough.md`는 다음을 면접관에게 whiteboard로 설명할 수 있는 수준으로 유지한다.

- TownPet이 해결하는 사용자 문제, 주요 actor와 showcase 범위
- 17개 module의 책임·소유 데이터·동기 의존·event 연결
- 49개 page와 55개 API가 어떤 사용자 여정과 domain rule을 제공하는지
- 회원·비회원·Moderator·Operator의 인증·인가와 resource ownership 차이
- Publication, LostFound, Marketplace, Care 등 핵심 상태 전이와 불변식
- 하나의 요청이 Browser→Caddy→Spring Security→application→PostgreSQL/R2를 통과하는 과정
- transaction commit 이후 event publication, retry, idempotency와 projection 갱신 과정
- 정상 흐름뿐 아니라 validation 실패, 동시 수정, 중복 요청, 외부 provider 실패와 복구 방식
- Legacy data ETL, reconciliation, parity 판정과 의도적 차이
- 배포, 관측, backup·restore, demo reset과 비용 제약

기능을 구현하거나 계약이 바뀌면 관련 map을 같은 PR에서 갱신한다. 파일 수를 나열하는 데 그치지 말고 `사용자 가치 → 정책 → module/API → data → test → 운영 신호`를 연결한다.

### 5.6 면접 답변으로 정제

`interview/question-bank.md`는 지식 문서의 복사본이 아니라 말로 답하는 연습 카드다. 각 핵심 질문에는 다음 세 깊이를 둔다.

- 30초: 상황·선택·검증 결과를 한 문단으로 답한다.
- 2분: 제약, 대안, 구현, 수치와 trade-off를 포함한다.
- Deep dive: 내부 동작, code path, failure mode, 꼬리 질문과 한계를 설명한다.

`story-bank.md`에는 대표 문제 해결 사례를 `상황·목표·행동·결과·회고` 순서로 정리하고 evolution 문서의 evidence를 링크한다. 성공 사례만 모으지 말고 잘못된 가설, 실패한 접근, rollback과 남은 한계도 포함한다. 답을 외우기보다 어떤 방향의 질문에서도 원리와 실제 근거를 조합해 설명할 수 있어야 한다.

확실히 답할 수 없는 질문은 추측으로 채우지 않고 `interview/gaps.md`에 기록한다. 각 gap에 필요한 학습, 구현 또는 실험과 연결된 PLAN 항목을 적고 해결되면 question bank로 옮긴다.

### 5.7 Coverage와 품질 gate

`docs/report/README.md`에는 다음 두 coverage 표를 유지한다.

- 기능 coverage: actor·journey·domain rule·API/data·test·report·면접 준비도
- 기술 coverage: 개념·도입 시점·적용 위치·대안·failure mode·evidence·면접 준비도

준비도는 `captured → understood → evidenced → rehearsed`로만 올린다. `evidenced`는 code·test·metric·migration·운영 기록 중 하나 이상의 직접 근거가 있을 때, `rehearsed`는 30초·2분·deep-dive 답변과 꼬리 질문이 모두 준비됐을 때만 사용한다.

각 slice 종료 시 관련 report와 index를 확인하고, 각 phase 종료 시 PRD·TRD·ADR·GLOSSARY·parity matrix를 coverage 표와 대조한다. 누락 항목은 `gaps.md`와 PLAN에 연결한다. Report 갱신이 필요한 중요한 변경인데 문서·evidence·coverage가 없으면 해당 slice를 완료로 판정하지 않는다.

## 6. 코드 배치 규칙

- `src/main/java/com/townpet/<module>/`: business module. Module root의 공개 application API 외 구현은 `internal`에 둔다.
- `src/main/java/com/townpet/common/`: 기술 공통 요소만 둔다. 회원, 게시물, 지역 같은 business abstraction을 넣지 않는다.
- `src/main/resources/db/migration/`: append-only Flyway migration. 배포된 migration을 수정하거나 Hibernate DDL로 대체하지 않는다.
- `api/openapi/`: HTTP 계약 원본. API 변경은 contract test와 generated-source diff를 함께 갱신한다.
- `frontend/src/features/`: 사용자 여정별 UI와 상태. 생성 client 밖에서 endpoint URL과 wire DTO를 중복 정의하지 않는다.
- `migration/`: 재실행 가능한 ETL, mapping과 익명 fixture. Web request 처리 코드와 섞지 않는다.
- `deploy/`: local compose, Caddy, Terraform, Ansible, backup과 배포 자동화. Secret과 실제 개인정보를 commit하지 않는다.
- `docs/parity/`, `docs/performance/`, `docs/runbooks/`: 완료 주장을 뒷받침하는 측정·대사·복구 증거.
- `docs/report/`: 프로젝트 이해, 기술 학습, 변화 과정과 면접 답변을 evidence에 연결하는 자료. Canonical 요구사항·결정을 중복 정의하지 않는다.

## 7. 데이터와 보안 규칙

- 식별자는 UUIDv7, 시각은 UTC `Instant`, 금액은 KRW 정수 `bigint`를 기본으로 한다.
- 외래 키, unique, check와 optimistic version으로 불변식을 표현하고 동시성 정책을 test한다.
- 비회원 작성은 `GuestPrincipal`과 콘텐츠 범위 관리 자격을 사용한다. IP·User-Agent 같은 abuse signal을 신원으로 취급하지 않는다.
- Staff 권한은 deny-by-default RBAC와 resource attribute를 함께 검사한다. Moderator, Operator와 사용자의 business 상태 변경 권한을 분리한다.
- LostFound의 정확 위치와 연락 증거는 암호화하고 공개 응답·log·projection에는 근사 위치만 노출한다.
- 실제 Legacy data를 fixture로 commit하지 않는다. Migration test는 익명화한 결정적 fixture를 사용하고 invalid row는 조용히 버리지 않고 quarantine한다.
- Log, trace, metric과 error response에서 credential, session, OAuth token, 관리 자격, 정확 위치와 개인정보를 제거한다.

## 8. 검증 명령

P1.1에서 Wrapper와 task를 만든 뒤 다음 명령이 기본 gate다.

```bash
./gradlew clean check
./gradlew integrationTest modulithTest migrationTest
./gradlew openApiValidate checkGeneratedSources contractTest
corepack pnpm -C frontend lint
corepack pnpm -C frontend typecheck
corepack pnpm -C frontend test
corepack pnpm -C frontend test:e2e
```

변경 범위에 따라 mutation, differential, performance, migration rehearsal, container build와 restore test를 추가한다. `PLAN.md` 각 slice에 적힌 검증 명령이 일반 gate보다 우선한다. 전체 명령이 아직 존재하지 않으면 해당 slice에서 먼저 재현 가능한 task로 만든다.

테스트를 실행하지 않았거나 실패했다면 완료라고 쓰지 말고 정확한 미실행 이유 또는 실패 명령을 보고한다. Format, mock 또는 compile 성공을 기능 동작 증거로 대신하지 않는다.

## 9. 완료 판정

기능은 다음 조건을 모두 만족해야 완료다.

- Legacy 기준선의 정상·오류·권한·상태·responsive 동작이 자동 test 또는 승인된 의도적 차이로 설명된다.
- API, migration, domain invariant와 frontend가 한 여정으로 연결된다.
- 다른 module의 내부 type 또는 Legacy server adapter에 새 의존이 없다.
- 관련 unit, integration, module, contract, browser와 security 검증이 실제로 통과했다.
- Data migration이 idempotent하고 count·relation·orphan·summary 대사 결과가 남는다.
- 운영에 영향을 주면 metric·log·runbook·rollback·backup/restore 검증이 함께 갱신된다.
- PRD, TRD, ADR, GLOSSARY, PLAN과 parity matrix가 현재 구현과 모순되지 않는다.
- 중요한 문제·판단·학습이 `docs/report/`의 evolution·knowledge·interview 자료와 coverage에 반영된다.

최종 완료는 화면이 열리는 것만 뜻하지 않는다. 49개 page·55개 API parity, Legacy server 제거, PostgreSQL migration, 성능 목표, CI/CD, 공개 showcase, 관측성, rollback과 RPO 5분·RTO 60분 restore drill의 재현 가능한 증거가 모두 필요하다.
