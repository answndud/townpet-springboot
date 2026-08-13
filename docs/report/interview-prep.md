# TownPet Spring Boot 면접 복기 문서

이 문서는 면접 직전에 프로젝트를 다시 설명하기 위한 복기 자료다. 공식 요구사항은 [`../PRD.md`](../PRD.md), 기술 구조는 [`../TRD.md`](../TRD.md), 오래 유지할 결정은 [`../../ADR.md`](../../ADR.md), 현재 구현 근거는 [`engineering-story.md`](engineering-story.md)와 [`technical-notes.md`](technical-notes.md)가 소유한다.

여기서는 “무엇을 만들었다”보다 다음을 설명하는 데 집중한다.

```text
상황/한계 → 선택한 대안 → 구현 경계 → 검증 방법 → trade-off와 다음 한계
```

아직 실제 VPS, production object storage, 외부 email provider를 검증하지 않았으므로 해당 항목은 반드시 `미완료` 또는 `deferred`라고 말한다.

---

## 1. 프로젝트 한 줄 설명

TownPet은 반려동물 커뮤니티의 게시글·댓글·반응·북마크·관계·분실동물·돌봄·봉사·거래·운영 신고를 제공하는 웹 서비스다. 기존 TownPet의 사용자 경험과 route/API parity를 유지하면서, Node/Next.js 기반 backend를 Java 25·Spring Boot 4.1·Spring Modulith·PostgreSQL 기반의 modular monolith로 재작성했다.

핵심은 Spring 문법을 적용한 것이 아니라 다음 네 가지를 동시에 만족한 것이다.

1. 기존 화면과 사용자 여정을 잃지 않는다.
2. 각 도메인의 write ownership과 권한 경계를 분리한다.
3. PostgreSQL constraint·transaction·lock으로 중요한 불변식을 지킨다.
4. 성능은 추측하지 않고 동일 fixture·동일 workload로 측정한다.

---

## 2. 30초 답변

> TownPet은 반려동물 커뮤니티 서비스이고, 저는 기존 서비스의 화면과 사용자 경험을 유지하면서 backend를 Java 25와 Spring Boot 기반으로 재작성했습니다. 혼자 개발하는 프로젝트라 microservice로 나누기보다 Spring Modulith modular monolith를 선택했고, PostgreSQL을 유일한 원장으로 두면서 Flyway migration, Spring Session JDBC, CSRF, RBAC를 적용했습니다. 개발 중에는 단순히 정상 흐름만 만든 것이 아니라 권한 우회, lost update, capacity 초과 신청, 대량 entity 로딩 같은 문제를 실제로 찾아 DB constraint·row lock·bulk update로 수정했습니다. 성능은 k6와 전용 fixture로 측정했고, 현재 병목 근거가 없어 Redis와 Kafka는 보류했습니다. 다만 실제 VPS와 production object storage·SMTP는 아직 배포 전 검증 범위입니다.

### 30초 답변에서 반드시 포함할 것

- 무엇을 만든 서비스인가
- 어떤 기술로 재작성했는가
- 가장 중요한 설계 선택 하나
- 실제로 해결한 문제 하나
- 아직 검증하지 않은 한계 하나

---

## 3. 2분 답변

### 문제와 목표

기존 TownPet은 사용자에게 필요한 화면과 기능이 있었지만, backend를 Java/Spring 기반 포트폴리오로 설명하기에는 Node server runtime과 frontend/backend 경계가 남아 있었다. 목표는 UI를 새로 만드는 것이 아니라 observable behavior를 유지하면서 backend write ownership을 Spring으로 옮기는 것이었다.

### 구조 선택

frontend는 React 19·TypeScript·Vite·React Router로 두고, production에는 정적 asset만 제공한다. backend는 하나의 Spring Boot process 안에 Spring Modulith module을 둔다. 모듈 간에는 entity나 repository를 공유하지 않고, 식별자·공개 application API·event만 사용한다.

PostgreSQL은 업무 데이터·session·Flyway history·Modulith event publication의 source of truth다. JPA는 aggregate write에, jOOQ는 feed와 제한된 복잡 read model에 사용한다. Hibernate는 schema를 만들지 않고 `ddl-auto=validate`로 mapping drift만 검사한다.

### 실제로 해결한 문제

인증에서는 session이 실제 JDBC에 저장되지 않고 memory session처럼 동작하던 문제를 발견해 `spring-boot-starter-session-jdbc`와 실제 `SESSION` cookie 검증으로 수정했다. 권한 감사에서는 URL matcher와 method security의 경계가 달라 MEMBER가 운영 API에 접근할 수 있던 문제와, guest cookie가 인증된 MODERATOR 경로를 우회할 수 있던 문제를 닫았다.

동시성에서는 조회수 upsert, volunteer capacity row lock, 신고 partial unique index, media attachment lock을 적용했다. 대량 공개 범위 변경은 entity 전체를 읽지 않고 조건부 bulk update로 바꿨다.

### 검증과 한계

backend·migration·Modulith·parity·frontend·browser smoke를 나누고, 성능은 전용 PostgreSQL과 k6 fixture로 측정했다. feed index는 100,000건 fixture에서 p95 67.13ms에서 5.01ms로 개선됐다. capacity 경합은 애플리케이션 처리 p95 238.98ms로 분리 측정했고, query projection 후보는 재현 가능한 개선이 없어 원복했다. 현재 결과는 local Docker 기준이므로 운영 SLA가 아니며, VPS·object storage·외부 email은 배포 전 남은 작업이다.

---

## 4. 전체 구조를 설명하는 방법

```text
Browser
  └─ React 19 + TypeScript + Vite + React Router
       └─ src/api/client.ts (fetch, CSRF, DTO)
            ↓ same-origin /api
       Caddy (production reverse proxy, static asset)
            ↓
       Spring Boot 4.1
       ├─ web/controller + request/response DTO
       ├─ application/domain service
       ├─ JPA write model
       ├─ jOOQ read model
       ├─ Spring Security + Spring Session JDBC
       └─ Spring Modulith event publication
            ↓
       PostgreSQL 18 + PostGIS 3.6 + Flyway
```

### 왜 modular monolith인가?

현재는 혼자 개발하고, 단일 VPS 비용을 월 1만 원 이하로 제한하며, 독립 배포나 팀별 소유권이 없다. microservice를 도입하면 네트워크 장애·분산 transaction·배포 단위·관측 구성이 먼저 생긴다. 대신 package와 Spring Modulith로 module boundary를 강제하면 한 process 안에서도 다음을 얻을 수 있다.

- 어떤 module이 어떤 table을 쓰는지 설명 가능
- entity/repository/controller DTO 누출을 ArchUnit으로 차단
- 순환 의존을 Modulith 검증으로 발견
- 필요할 때만 별도 process로 분리할 수 있는 application API 경계 확보

### module을 나누는 기준

기술 layer가 아니라 변경 이유와 write ownership이다.

| module | 책임 | 대표 원장 |
|---|---|---|
| `identity` | credential, session, token, staff role | `member_account`, token, audit |
| `member` | profile, pet, member directory | member profile/pet |
| `publication` | 게시글 lifecycle·visibility | `publication` |
| `engagement` | comment, reaction, bookmark | 각 engagement 원장 |
| `relationship` | follow, block | follow/block 원장 |
| `discovery` | feed, search event, read model | projection/search event |
| `lostfound` | 분실 신고, 목격, 정확 위치 접근 | alert/sighting/location evidence |
| `care` | 돌봄 요청·지원·배정·후기 | care request/application/assignment |
| `welfare` | 입양·봉사·병원 후기 | opportunity/listing/review |
| `marketplace` | 결제 없는 거래 listing | listing/status history |
| `trustsafety` | 신고와 moderation | report/moderation case |
| `media` | upload metadata와 lifecycle | `upload_asset` |
| `operations` | 운영·repair·web vital·정책 | operation/metric 계열 |

---

## 5. 기술 선택을 설명하는 답변

### Java 25와 Spring Boot 4.1

Java toolchain과 Gradle Wrapper로 local과 CI의 compile 환경을 고정했다. Spring Boot는 web, security, validation, data access, actuator의 기본 통합을 제공하고, Spring Modulith는 process 내부 module 경계를 검증한다.

**질문: 왜 최신 기술을 사용했나?**

> 기술 자체가 목적은 아니었습니다. Java 25와 Spring Boot 4.1을 기준선으로 고정하되 preview feature는 사용하지 않았고, Wrapper와 CI에서 같은 toolchain을 재현하는 것이 더 중요했습니다. 면접에서는 버전의 신기함보다 build reproducibility와 현재 코드의 근거를 설명할 수 있습니다.

### PostgreSQL과 Flyway

PostgreSQL은 업무 원장, session, migration history, Modulith event publication을 모두 보유한다. Flyway만 versioned schema를 변경하고, runtime app role은 필요한 DML 권한만 가진다. PostGIS와 `citext` 같은 extension은 bootstrap 단계에서 설치한다.

**질문: JPA가 schema를 관리하지 않는 이유는?**

> 운영 schema를 entity 상태에 따라 암묵적으로 바꾸면 migration history와 실제 DB 상태를 추적하기 어렵습니다. Flyway로 변경 순서를 명시하고, Hibernate는 `validate`만 수행하게 해 mapping drift는 잡되 schema authority는 하나로 유지했습니다.

### JPA와 jOOQ를 함께 사용한 이유

- JPA: aggregate write, 상태 전이, optimistic version, transaction 경계
- jOOQ: feed cursor, projection, 여러 조건의 제한된 read model
- 같은 조회를 두 방식으로 중복 구현하지 않고, query shape가 중요한 read에만 jOOQ를 사용

### React·Vite

Vite는 개발 proxy와 정적 build 도구다. production Node server는 사용하지 않고 Caddy가 정적 frontend를 제공하며 `/api`를 Spring Boot로 reverse proxy한다. HTTP 계약은 controller DTO와 `frontend/src/api/client.ts`가 직접 소유한다.

**질문: OpenAPI generator를 왜 쓰지 않았나?**

> 검토는 했지만 현재 API 규모와 혼자 개발하는 상황에서는 별도 계약 파일과 generated client가 작은 변경마다 추가 동기화 비용을 만들었습니다. 그래서 controller DTO와 typed API client를 직접 근거로 두었습니다. 독립 팀·외부 client·계약 versioning이 생기면 다시 선택할 수 있습니다.

---

## 6. 사용자 기능을 흐름으로 설명하기

### 6.1 로그인·세션·onboarding

```text
CSRF token 발급
→ email/password 로그인
→ credential 검증
→ session 생성 및 ID 교체
→ JDBC session 저장
→ 현재 member/profile/pet 조회
→ React viewer shell 갱신
```

핵심 규칙:

- browser에는 opaque session cookie만 둔다.
- password hash는 BCrypt다.
- 로그인 성공 시 session fixation 방지를 위해 session ID를 교체한다.
- 모든 state-changing 요청은 `X-XSRF-TOKEN`을 보낸다.
- logout은 cookie 상태만 지우지 않고 서버 session을 invalidate한다.
- 현재 member ID는 request body가 아니라 authenticated principal에서 얻는다.

검증 근거: `IdentityMemberControllerTest`, `auth-parity.spec.ts`, V002~V006, Spring Session JDBC row 대사.

### 6.2 게시글과 public feed

게시글은 작성자, visibility, lifecycle, version을 가진다. 삭제는 row delete가 아니라 `ACTIVE → DELETED` 전이이며, feed와 상세·engagement가 동일한 active 조건을 적용한다.

feed는 `(created_at, id)`를 cursor로 사용하는 keyset pagination을 사용한다.

```text
첫 페이지: cursor 없음
→ 마지막 (created_at, id) 반환
→ 다음 요청이 그 tuple보다 오래된 row 조회
→ offset 증가에 따른 앞부분 재스캔 회피
```

`GLOBAL`은 로그인 여부와 관계없이 global 게시글만 읽고, `VIEWER`는 현재 회원의 대표 동네 정책을 추가한다. 로그인 cookie가 있다고 해서 global feed에 local 글을 섞지 않는다.

### 6.3 댓글·reaction·bookmark

- Comment: 삭제 상태를 보존하는 lifecycle 원장
- Reaction: `(publication, member, type)` unique row의 존재와 active PUT
- Bookmark: `(publication, member)` unique row의 active 상태

세 기능을 한 공통 engagement table로 합치지 않은 이유는 삭제 방식, count 의미, ownership과 상태 전이가 다르기 때문이다. 차단된 작성자에 대한 engagement는 `BlockDirectory` 정책과 DB guard를 함께 통과해야 한다.

### 6.4 follow·block

follow와 block은 서로 다른 원장이라 unique constraint만으로 상호 배타성을 보장할 수 없다. 같은 viewer-target 쌍의 mutation 시작 시 advisory lock을 획득하고, block 활성화 시 follow를 제거한다. 병렬 follow/block 요청 후 두 row가 동시에 남지 않는지 `RelationshipControllerTest`로 확인했다.

### 6.5 Lost & Found

분실 신고의 공개 위치는 근사 정보이고, 정확 위치·연락 증거는 보호된 별도 저장소와 권한 정책을 사용한다.

**질문: 왜 위치를 한 필드에 저장하지 않았나?**

> 공개 피드의 검색성과 소유자 보호를 같은 값으로 처리하면 정확 위치가 public response나 log로 새어 나갈 위험이 있습니다. 그래서 public projection에는 근사 정보만 두고, 정확 정보는 owner 또는 허용된 viewer만 읽는 별도 경계로 분리했습니다.

### 6.6 Care·Volunteer·Marketplace

Care는 다음 상태 흐름을 가진다.

```text
Care Request
→ Application
→ Assignment
→ Feedback
```

각 단계는 다른 actor와 허용 상태가 있다. Volunteer는 capacity를 opportunity row lock 아래에서 count·insert·FULL 전이한다. Marketplace는 결제 시스템이 아니라 listing과 lifecycle에 한정한다. 이 범위를 명확히 했기 때문에 결제·환불·정산을 구현한 것처럼 주장하지 않는다.

---

## 7. 권한 모델 복기

| actor | 공개 읽기 | 일반 사용자 mutation | 운영 API | 설명 |
|---|---|---|---|---|
| anonymous | 공개 범위 | guest 허용 범위 | 거부 | IP만으로 신원을 증명하지 않음 |
| MEMBER | 허용 범위 | 게시·engagement·관계·care 등 | 거부 | principal의 member ID 사용 |
| MODERATOR | 공개·운영 읽기 | 일반 사용자 mutation 거부 | 허용 | 신고·moderation 전용 |
| ADMIN/OPERATOR | 현재 demo 공개 안 함 | 운영 범위 | 공개하지 않음 | 공개 sandbox credential 정책 |

### 실제로 발견한 권한 문제

1. URL matcher만 보호되고 method security가 활성화되지 않아 일부 admin surface가 열려 있었다.
2. MODERATOR가 일반 회원용 게시·댓글·관계·거래 mutation을 실행할 수 있었다.
3. guest cookie가 있으면 인증된 MODERATOR가 guest write 경로를 호출할 가능성이 있었다.
4. backend는 읽기 전용으로 거부했지만 frontend가 회원 전용 버튼을 보여주는 화면이 있었다.

### 해결 방법

- Spring Method Security 활성화
- `/api/admin/**`와 compatibility alias의 공통 권한 규칙 정리
- `MemberOnly`와 moderator 전용 정책 분리
- guest write는 anonymous 또는 허용된 MEMBER만 통과
- React route guard와 action rendering도 actor에 맞춰 분리
- MEMBER/MODERATOR 실제 Docker session으로 `403/200` 결과를 확인

**면접용 핵심:** 화면에서 숨기는 것은 authorization이 아니다. principal 생성, controller annotation, service policy, DB constraint가 모두 같은 방향을 가리켜야 한다.

---

## 8. 실제 문제를 해결한 사례

### 사례 A. JDBC session이 실제로 사용되지 않던 문제

**상황**: Spring Session library와 table은 있었지만 Boot 4 JDBC starter가 없어 테스트가 servlet memory session처럼 동작했다.

**문제**: `MockHttpSession`만 전달하면 실제 production cookie와 JDBC repository가 검증되지 않는다.

**선택**: `spring-boot-starter-session-jdbc`를 사용하고, 테스트가 `SESSION` cookie와 DB row를 직접 확인하도록 바꿨다.

**결과**: logout·password reset 뒤 기존 session revoke까지 실제 저장소 기준으로 확인할 수 있었다.

**한계**: 외부 email delivery와 retry/bounce는 아직 production adapter가 없다.

근거: `build.gradle.kts`, `IdentityMemberControllerTest`, V004~V006, `auth-browser-e2e.sh`.

### 사례 B. 권한 우회와 actor 혼동

**상황**: matcher와 method security가 달라 MEMBER가 운영 API에 접근할 수 있었고, MODERATOR가 일반 mutation을 호출할 수 있었다.

**선택**: role을 UI 조건으로만 두지 않고 Spring Method Security, principal 기반 service policy, frontend route guard를 함께 수정했다.

**검증**: MEMBER와 MODERATOR demo session으로 관리자 API·일반 mutation을 교차 호출하고 `403/200`을 확인했다.

**trade-off**: MODERATOR는 운영 검토에 집중하고 일반 글 작성이 필요하면 별도 MEMBER demo 계정을 사용한다.

근거: `SecurityConfig`, `MemberOnly`, `IdentityMemberControllerTest`, `frontend/src/App.tsx`.

### 사례 C. 조회수 lost update

**상황**: `find → save` 방식은 병렬 요청이 같은 이전 값을 읽어 증가를 잃을 수 있었다.

**선택**: PostgreSQL `INSERT ... ON CONFLICT DO UPDATE ... RETURNING`으로 증가를 DB 원자 연산에 내렸다.

**검증**: 160개 동시 요청을 8개 thread로 실행하고 최종 counter를 대사했다.

**trade-off**: DB가 counter source of truth를 계속 가지므로 Redis counter보다 단순하고 정확하지만, 매우 높은 write rate에서는 DB contention을 다시 측정해야 한다.

### 사례 D. Volunteer capacity 초과 신청

**상황**: 애플리케이션에서 count를 먼저 보고 insert하면 같은 opportunity에 병렬 신청이 들어올 때 capacity를 넘을 수 있다.

**선택**: opportunity row를 `FOR UPDATE`로 잠그고 count·insert·FULL 전이를 한 transaction으로 실행했다.

**검증**: 10개 capacity에 20개 신청을 보내 201 10건·409 10건으로 수렴하고 row 수와 상태를 SQL로 대사했다.

**성능 trade-off**: 같은 row를 직렬화하므로 application latency p95가 약 238.98ms가 됐다. 정확성을 먼저 유지하고, Redis counter나 Kafka로 우회하지 않았다.

### 사례 E. Query projection 후보 기각

**상황**: lock 동안 전체 opportunity row를 읽는 대신 `capacity,status`만 projection하면 빨라질 것이라고 추정했다.

**검증**: before/after 순서를 바꿔 10회씩 총 20회 측정했다. before 중앙값 67.29ms, after 94.37ms로 개선이 재현되지 않았다.

**결정**: 후보를 원복하고 기존 구현을 유지했다.

**면접 포인트**: 한 번의 빠른 결과를 최적화 성과로 포장하지 않고, 측정 순서와 반복성까지 확인해 기각했다.

### 사례 F. 대량 공개 범위 변경

**상황**: 모든 publication entity를 읽어 변경·저장하면 row 수에 따라 heap·flush 비용이 커지고, 이미 같은 상태인 row까지 affected 수에 포함됐다.

**선택**: `ACTIVE ↔ HIDDEN` 조건을 포함한 PostgreSQL bulk update로 변경하고 실제 변경 row만 version·updatedAt을 갱신했다.

**주의**: bulk update는 JPA lifecycle callback을 실행하지 않으므로 callback에 의존하는 규칙에는 사용하지 않는다.

### 사례 G. Security filter 오류 계약

**상황**: controller에 도달하기 전 끝나는 401·403은 MVC `ProblemDetail` handler와 다른 응답이 됐다.

**선택**: authentication entry point와 access denied handler를 직접 추가해 status·machine code·trace ID 형식을 통일했다.

**효과**: frontend가 권한 오류를 같은 DTO로 처리하고, 서버 로그 trace와 연결할 수 있게 됐다.

### 사례 H. 성능 하네스 오류와 실제 오류 분리

**상황**: k6 session cookie 누락으로 반복 write가 403이 되고, moderator fixture hash 불일치로 401이 발생했다.

**선택**: 결과를 성능 저하로 기록하지 않고 fixture와 harness를 수정한 뒤 유효한 run만 채택했다.

**교훈**: 부하 테스트에서는 애플리케이션 오류, 테스트 데이터 오류, runner/network 오류를 먼저 분리해야 수치를 해석할 수 있다.

---

## 9. 성능 개선을 설명하는 방법

### 측정 루프

```text
대표 workload 정의
→ deterministic fixture 생성
→ PostgreSQL-only baseline
→ query/index/transaction 원인 분류
→ 한 가지 변경
→ 동일 조건 before/after
→ 무결성·resource·한계 기록
```

### 주요 결과

| 주제 | 결과 | 결정 |
|---|---|---|
| public feed 복합 index | 100,000 publication, p95 67.13ms → 5.01ms | 적용 |
| S0~S8 workload | write·contention·moderator·media·mixed·spike·soak 실행 | 제한 포함 accepted |
| capacity latency 분리 | login 비용 제거 후 application p95 238.98ms | row lock 유지 |
| lock query projection | 20회 전후 비교에서 개선 재현 안 됨 | 후보 원복 |
| 최신 public feed 3회 | p95 4.35~6.40ms, 오류 0 | Redis 보류 |
| Kafka | request 후속 backlog 근거 없음 | Kafka 보류 |

상세 수치와 재현 명령은 [`../performance/README.md`](../performance/README.md)와 결과 문서에 있다.

### Redis를 왜 넣지 않았나?

Redis가 항상 빠르다는 전제를 두지 않았다. 현재 public feed p95가 이미 낮고, cache를 도입하려면 cursor·필터별 key, 공개범위 변경 invalidation, stale response 방지, Redis 장애 fallback을 함께 보장해야 한다. 측정 가능한 DB saturation이 없으므로 비용과 일관성 위험이 이득보다 크다고 판단했다.

### Kafka를 왜 넣지 않았나?

핵심 transaction과 즉시 보여야 하는 상태 변경을 broker로 옮기면 eventual consistency와 중복 처리 문제가 생긴다. 현재 notification/projection backlog나 consumer lag이 없으므로 Spring Modulith event publication과 PostgreSQL을 유지한다. 실제 후속 작업이 request p95를 차지하거나 backlog가 생기면 candidate-enabled 실험을 연다.

### 성능 질문에 대한 주의

- local Docker 수치는 운영 SLA가 아니다.
- p95만 보고 평균이나 p99를 추정하지 않는다.
- login BCrypt 비용과 도메인 mutation 비용을 합쳐 설명하지 않는다.
- spike Docker bridge timeout을 애플리케이션 5xx로 표현하지 않는다.
- 최적화 후보가 빠르게 보였다는 이유만으로 채택하지 않는다.

---

## 10. 테스트와 검증 전략

| 계층 | 확인하는 것 | 대표 근거 |
|---|---|---|
| unit/domain | 상태 전이·입력 규칙 | service/domain tests |
| Spring integration | controller·security·transaction | `IdentityMemberControllerTest`, `CareControllerTest` |
| PostgreSQL migration | dialect·constraint·index·Flyway | `DatabaseBaselineTest`, `migrationTest` |
| Modulith/ArchUnit | module cycle·내부 type 노출 | `ModularityTest`, `LayerRulesTest` |
| frontend unit | UI 상태·권한 표시 | Vitest |
| parity inventory | legacy route/API coverage | `ParityInventoryTest`, matrix |
| browser E2E | 실제 로그인·작성·권한 여정 | Playwright auth/parity specs |
| HTTP smoke | frontend ↔ backend 연결 | `scripts/frontend-backend-smoke.sh` |
| performance | latency·throughput·integrity | k6 + perf PostgreSQL |

전체 gate는 모든 작은 수정마다 돌리지 않고, 큰 phase 종료 또는 release 직전에 실행한다. 구현 중에는 가장 가까운 검증을 먼저 실행하고, 실행하지 않은 검증은 통과했다고 말하지 않는다.

대표 release gate:

```bash
./gradlew clean check migrationTest
(cd frontend && corepack pnpm install --frozen-lockfile)
(cd frontend && corepack pnpm typecheck)
(cd frontend && corepack pnpm test)
(cd frontend && corepack pnpm build)
(cd frontend && corepack pnpm test:e2e)
./scripts/frontend-backend-smoke.sh
```

---

## 11. 아직 완성되지 않은 범위

면접에서 질문받으면 아래를 숨기지 않는다.

### 아직 배포 전인 것

- 실제 Hetzner VPS resource와 네트워크를 포함한 성능 측정
- DNS·TLS·Caddy 자동 인증서와 secure session cookie의 실제 도메인 검증
- 외부 backup 보관, restore drill, RPO/RTO
- 운영 관측 backend와 alerting

### 현재 정책 결정이 필요한 것

- production object storage: 현재 production profile은 `UnavailableObjectStorage`
- 공개 demo seed와 scoped daily reset
- email verification·password recovery를 SMTP와 함께 공개할지 여부

### 현재 의도적으로 deferred인 것

- Redis cache/counter
- Kafka broker/consumer
- Elasticsearch/search document
- Kubernetes/microservice
- 결제·정산
- Kakao/Naver social login

이 항목들은 빠뜨린 기능이 아니라 ADR에 trigger와 함께 보류한 범위다. “구현했다”고 말하지 않고 “어떤 조건이면 도입할지 결정했다”고 말한다.

---

## 12. 예상 질문과 답변

### Q1. 왜 microservice가 아닌가요?

현재는 혼자 개발하고 단일 VPS를 목표로 하므로 분산 운영 비용이 이득보다 크다. 대신 Spring Modulith로 module boundary와 public application API를 검증해 나중에 분리 가능한 구조를 유지했다.

### Q2. 왜 JWT가 아닌 session인가요?

Browser 기반 same-origin 서비스이고 서버에서 logout·reset 시 기존 인증을 즉시 revoke해야 한다. opaque JDBC session이면 token 폐기와 server-side security context를 직접 통제할 수 있으며, browser JWT 저장소 문제도 피한다.

### Q3. CSRF를 어떻게 막았나요?

HttpOnly session cookie만 사용하고, CSRF token을 `XSRF-TOKEN` cookie와 body로 교환한 뒤 React가 state-changing 요청에 `X-XSRF-TOKEN` header를 보낸다. Spring Security가 이를 검증한다.

### Q4. IDOR는 어떻게 줄였나요?

현재 member ID를 request body/path에서 받지 않고 authenticated principal에서 얻는다. resource 조회도 owner 조건과 public application API를 함께 사용한다. 다른 member의 profile·pet·engagement 상태를 client가 지정할 수 없게 했다.

### Q5. 권한을 frontend에서도 검사하는 이유는 무엇인가요?

frontend 검사는 UX를 위한 것이고 security boundary가 아니다. 실제 authorization은 backend principal·method security·service policy·DB guard가 담당한다. frontend guard는 MODERATOR에게 일반 mutation 버튼을 보여주는 혼란을 줄인다.

### Q6. optimistic lock과 row lock은 언제 구분하나요?

사용자가 읽은 version으로 수정 충돌을 검출할 때는 optimistic version/ETag를 사용한다. capacity처럼 여러 요청이 동일한 현재 count를 기준으로 insert해야 하는 경우에는 target row를 `FOR UPDATE`로 직렬화한다.

### Q7. 왜 bulk update가 항상 좋은 것은 아닌가요?

entity lifecycle callback, domain event, aggregate validation이 필요한 변경을 bulk update로 처리하면 JPA 상태와 DB 상태가 어긋날 수 있다. 이번에는 조건부 상태·version·timestamp만 직접 바꾸는 moderation 경로라 bulk update를 사용했다.

### Q8. PostgreSQL constraint와 service validation을 둘 다 두는 이유는?

service validation은 사용자에게 의미 있는 오류를 주고, DB constraint는 동시 요청과 다른 write 경로에서도 최종 불변식을 지킨다. 한쪽만 믿으면 race window 또는 일관되지 않은 오류가 생긴다.

### Q9. feed에 offset pagination을 쓰지 않은 이유는?

앞 페이지를 계속 건너뛰는 offset은 데이터가 커질수록 앞부분을 재스캔하고, 동시에 새 글이 들어오면 중복·누락이 생긴다. `(created_at, id)` keyset cursor와 복합 index를 사용했다.

### Q10. capacity p95가 238ms인데 개선해야 하지 않나요?

먼저 정확한 capacity 불변식이 우선이다. projection 후보를 20회 비교했지만 개선이 재현되지 않았다. 현재는 row lock을 유지하고, 실제 운영 규모에서 lock wait가 병목으로 확인될 때 transaction 범위·index·queue를 다시 평가한다.

### Q11. Redis를 사용하면 더 빠르지 않나요?

반복 read와 DB saturation이 확인될 때는 후보가 될 수 있다. 그러나 현재 public feed 기준선 p95가 4.35~6.40ms이고, cache invalidation·stale·fallback 복잡성이 추가된다. 그래서 지금은 도입하지 않고 trigger를 문서화했다.

### Q12. Kafka를 사용하면 비동기로 빨라지지 않나요?

핵심 transaction을 Kafka로 옮기면 응답은 빨라질 수 있어도 source-of-truth와 최종 처리 시점이 분리된다. 실제 notification/projection backlog가 생길 때만 idempotent consumer·retry·lag까지 포함한 실험을 한다.

### Q13. H2 테스트만으로 충분하지 않나요?

충분하지 않다. H2는 빠른 web/context 테스트에 쓰고, PostgreSQL dialect·extension·constraint·index·migration은 Testcontainers와 실제 Compose에서 검증한다. 실제로 `citext`, `CHAR/VARCHAR`, Flyway initialization 문제가 PostgreSQL에서 드러났다.

### Q14. OpenAPI를 왜 만들지 않았나요?

현재는 controller DTO와 typed fetch client가 더 작고 직접적인 계약이다. 외부 소비자나 별도 frontend 팀이 생기면 schema versioning과 generated client의 이득이 커지므로 그때 재평가한다.

### Q15. 이 프로젝트에서 가장 어려웠던 부분은 무엇인가요?

정상 화면을 만드는 것보다, 인증된 actor·guest·moderator가 같은 endpoint에서 서로 다른 결과를 내도록 하고, 병렬 요청에서도 DB 불변식을 유지하는 일이 어려웠다. 그래서 권한과 동시성을 UI가 아니라 principal·transaction·constraint까지 내렸다.

### Q16. 테스트가 실패했을 때 어떻게 구분했나요?

애플리케이션 오류, fixture/seed 오류, harness cookie 오류, Docker network 오류를 분리했다. 성능 결과에서는 예상 4xx와 unexpected 5xx·timeout을 따로 집계하고, 데이터 row/version/capacity를 SQL로 대사했다.

### Q17. production에서 바로 사용할 수 있나요?

기능·local·CI 기준의 release candidate는 있지만, 아직 운영 complete라고 말할 수 없다. media storage, VPS·TLS, external backup, monitoring, email 정책을 마친 뒤 최종 release gate를 실행해야 한다.

---

## 13. 면접 전 복기 순서

### 30분 복기

1. 이 문서의 30초 답변을 소리 내어 말한다.
2. architecture diagram을 보지 않고 설명한다.
3. 권한 문제와 capacity 문제를 각각 상황→선택→검증으로 말한다.
4. 성능 결과에서 p95와 한계를 확인한다.
5. 아직 미완료인 media·VPS·SMTP를 정확히 말한다.

### 1시간 복기

- `engineering-story.md` 5~9절을 읽고 각 근거 파일을 한 번 연다.
- `technical-notes.md`에서 session·CSRF·module·DB·feed 단락을 확인한다.
- 성능 결과 3개를 직접 읽는다: feed index, capacity diagnostics, phase4 Redis.
- demo MEMBER와 MODERATOR 권한 차이를 실제 local 환경에서 한 번 재현한다.

### 답변 점검표

- [ ] “왜 선택했는가”를 말했는가?
- [ ] 코드나 migration 근거를 말했는가?
- [ ] 검증 명령 또는 수치를 말했는가?
- [ ] trade-off를 인정했는가?
- [ ] 아직 안 한 일을 했다고 말하지 않았는가?
- [ ] Redis/Kafka를 무조건 도입하는 것이 아니라 trigger 기반으로 판단했는가?

---

## 14. 근거 파일 지도

| 주제 | 확인할 파일 |
|---|---|
| 제품 범위 | `docs/PRD.md` |
| 기술 구조 | `docs/TRD.md`, `ADR.md` |
| 현재 작업·남은 일 | `PLAN.md` |
| module boundary | `src/main/java/com/townpet/*/package-info.java`, `ModularityTest` |
| 인증·세션 | `SecurityConfig`, `SessionController`, `IdentityMemberControllerTest` |
| 권한 | `MemberOnly`, `SecurityConfig`, `IdentityMemberControllerTest`, `frontend/src/App.tsx` |
| publication/feed | `PublicationService`, `CommunityFeed`, `FeedController`, V054~V061 |
| engagement/block | `CommentService`, `ReactionService`, `BookmarkService`, `RelationshipService`, V012 |
| concurrency | `ReleaseCandidateQueryPlanTest`, `BlockedEngagementPolicyTest`, capacity tests |
| 성능 | `docs/performance/README.md`, `docs/performance/results/` |
| 배포 준비 | `docs/report/release-readiness.md`, `deploy/compose/portfolio.yml` |
| local demo | `docs/demo/local-demo-accounts.md`, `migration/fixtures/local-demo.sql` |

이 표에 없는 일반적인 Java/Spring 개념은 공식 문서를 참고할 수 있지만, 면접 답변에서는 반드시 TownPet의 실제 코드와 연결해 설명한다.
