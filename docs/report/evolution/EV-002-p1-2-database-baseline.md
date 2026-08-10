# EV-002 — P1.2 Database Baseline

- 상태: evidenced
- 출처: `planned-upfront` + `implementation-discovery`
- 범위: `PLAN.md` P1.2

## 1. 목표와 출발 상태

P1.1에는 H2 기반 context test만 있었고 실제 PostgreSQL 연결·migration·object storage가 없었다. 이번 slice의 목표는 PostgreSQL 18·PostGIS 3.6을 durable source of truth로 두고, Flyway baseline과 local MinIO를 재현 가능하게 기동하는 것이었다.

## 2. 구현과 선택

- Compose는 `postgis/postgis:18-3.6`과 pinned MinIO release를 사용한다.
- PostgreSQL bootstrap admin이 PostGIS·citext extension과 `townpet_app` login role을 만들고, application role에는 schema 사용·DML 권한만 준다.
- Flyway `V001__platform_baseline.sql`은 extension이 사전 provision됐는지 확인한 뒤 Spring Session JDBC와 Modulith event publication registry table을 생성한다.
- Spring Modulith 자동 schema initialization은 끄고 Flyway를 유일한 schema authority로 둔다.
- Testcontainers는 `postgis` 이미지를 PostgreSQL compatible substitute로 명시하고 migration을 독립적으로 검증한다.

## 3. 구현 중 발견한 문제와 대응

### 3.1 Flyway starter 누락

처음에는 `flyway-core`와 PostgreSQL database module만 선언해 application이 시작됐지만 Flyway migration이 실행되지 않았다. Spring Boot 4의 `spring-boot-starter-flyway`가 auto-configuration 진입점이라는 것을 확인하고 starter를 추가했다.

### 3.2 Modulith 자동 schema와 Flyway 충돌

Modulith가 먼저 `event_publication`을 만들면 Flyway history table이 없는 non-empty schema가 되어 migration이 거부됐다. 기본 자동 초기화를 끄고 Flyway migration에 event table을 넣어 단일 schema owner를 만들었다.

### 3.3 extension 권한 경계

application role로 PostGIS extension을 만들 수 없어 bootRun이 실패했다. extension 생성은 bootstrap admin 책임으로 이동하고 migration은 누락 시 명시적인 예외를 내도록 했다. 이는 운영 migration role에 superuser 권한을 주지 않기 위한 의도적인 경계다.

### 3.4 ARM host와 PostGIS image

Apple Silicon에서 PostGIS image가 linux/amd64로 실행되어 platform warning이 발생했다. Compose에 `platform: linux/amd64`를 명시해 개발·CI 차이를 드러내고, production Hetzner x86 환경과의 차이를 report에 남겼다.

## 4. 검증 evidence

```text
docker compose -f deploy/compose/local.yml config       PASS
docker compose up -d                                   PostgreSQL/MinIO healthy
./gradlew migrationTest                                DatabaseBaselineTest PASS
./gradlew bootRun                                      Flyway v001 applied
GET /actuator/health                                   {"status":"UP"}
DB query                                               flyway_schema_history=001,
                                                       spring_session,
                                                       spring_session_attributes,
                                                       event_publication present
```

Compose 검증 후 local named volume은 보존한 채 컨테이너를 내렸다. Testcontainers는 별도의 disposable PostgreSQL을 사용했다.

## 5. 다음 단계

P1.3에서 17개 Spring Modulith application module과 ArchUnit 경계를 코드로 검증한다. P1.2에서는 아직 domain table, PostGIS spatial query, object upload lifecycle을 구현하지 않았으므로 해당 기능 완료로 확대 해석하지 않는다.

## 면접 답변

### 30초

실제 PostgreSQL을 기준으로 쓰기 전에 Flyway와 권한 경계를 먼저 고정했습니다. extension은 bootstrap admin이 만들고 application role은 migration·DML에 필요한 권한만 가지게 했으며, Spring Session과 Modulith event publication schema를 Flyway V001에서 관리했습니다. Testcontainers와 Compose, 실제 bootRun health check로 같은 baseline이 재현되는지 확인했습니다.

### 예상 꼬리 질문

- 왜 extension 생성 권한을 application role에 주지 않았나?
- `spring-boot-starter-flyway`가 필요한 이유는 무엇인가?
- Modulith schema 초기화와 Flyway를 동시에 켜면 어떤 문제가 생기나?
- H2 context test와 PostgreSQL migration test의 역할은 어떻게 다른가?
- ARM 개발 머신에서 amd64 image를 사용하는 비용과 대안은 무엇인가?
