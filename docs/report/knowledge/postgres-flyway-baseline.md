# PostgreSQL·Flyway Baseline

## Source of truth

Flyway migration이 schema 변경의 권위이며 Hibernate `ddl-auto=validate`는 mapping이 이미 존재하는 schema와 맞는지만 확인한다. Hibernate가 table을 만들게 두면 migration history와 운영 schema drift를 통제할 수 없으므로 production 기본값으로 사용하지 않는다.

## Bootstrap과 application 권한

PostGIS·citext 같은 extension 설치는 database bootstrap admin이 수행한다. application role은 `CONNECT`, `USAGE`, 필요한 DML과 migration 대상 schema 권한만 가진다. 권한 경계가 필요한 이유는 웹 애플리케이션 침해가 database superuser 권한으로 확대되지 않게 하기 위해서다.

## Migration lifecycle

Spring Boot 4에서는 `spring-boot-starter-flyway`가 Flyway auto-configuration을 연결하고 PostgreSQL은 별도 database module이 필요하다. 시작 시 `flyway_schema_history`를 확인하고 아직 적용되지 않은 `V001__...sql`을 순서대로 실행한다. 실패한 migration은 startup을 막아 애플리케이션이 부분적으로 올라가지 않게 한다.

## Test layers

- H2 context test: Spring bean graph와 빠른 auto-configuration 검증
- Testcontainers PostgreSQL: 실제 extension·Flyway·권한·table 검증
- Compose PostgreSQL: 개발자가 반복해서 사용할 local runtime 검증
- bootRun + Actuator: migration 이후 애플리케이션이 실제 DB에 연결되는지 검증

서로 다른 계층의 성공을 같은 의미로 취급하지 않는다. H2 통과만으로 PostgreSQL spatial/migration 호환성을 주장할 수 없다.

## Event publication schema

Modulith JDBC event publication registry는 at-least-once 후속 처리를 위한 durable publication row를 저장한다. 자동 schema 초기화와 Flyway를 동시에 켜면 생성 순서와 history table이 충돌할 수 있으므로 한 가지 schema owner만 둔다.
