# EV-001 — P1.1 Build Foundation

- 상태: evidenced
- 출처: `planned-upfront` + `implementation-discovery`
- 범위: `PLAN.md` P1.1
- 구현 commit: 다음 작업 commit에서 연결

## 1. 출발 상태

repository에는 PRD·TRD·ADR·GLOSSARY·PLAN만 있었고 Gradle Wrapper, Spring source, test source가 없었다. 목표는 도메인 기능을 서둘러 넣는 것이 아니라 Java 25와 Spring Boot 기반의 재현 가능한 build·test 경계를 먼저 세우는 것이었다.

## 2. 왜 이 구성을 선택했나

- Gradle Wrapper: 개발자나 CI의 system Gradle 버전 차이를 제거하고 build 진입점을 고정하기 위해 선택했다.
- Java 25 toolchain: 프로젝트 ADR·TRD의 목표 runtime과 일치시키고 source/target drift를 막기 위해 `JavaLanguageVersion.of(25)`와 `options.release=25`를 함께 설정했다.
- Spring Boot 4.1.0·Spring Modulith 2.1.0: Spring-native modular monolith와 event publication 기반을 이후 domain slice의 출발점으로 만들기 위해 선택했다.
- JPA·jOOQ·Flyway·Spring Session JDBC·Actuator·Testcontainers: 이후 write/read 분리, schema authority, session persistence, 운영 신호와 PostgreSQL 검증에 필요한 경계를 dependency 수준에서 먼저 고정했다.
- Spotless·Error Prone·NullAway·JaCoCo: 기능 코드가 쌓이기 전에 format·정적 분석·null contract·coverage의 CI gate를 만들기 위해 선택했다.

이 단계에서는 성능 병목을 근거로 JPA나 jOOQ를 추가한 것이 아니다. 해당 선택은 이미 accepted ADR/TRD에 정의된 `planned-upfront` 결정이며, 실제 query·성능 개선의 chronology는 이후 measurement-driven report에서 별도로 기록한다.

## 3. 구현 중 발견한 문제와 대응

### 3.1 Error Prone 설정 API

처음에는 `options.errorprone`을 import 없이 project-level block처럼 작성해 Gradle Kotlin DSL compilation이 실패했다. 공식 plugin 사용법에 맞춰 `net.ltgt.gradle.errorprone.errorprone` extension을 import하고 `JavaCompile` task 내부에서 NullAway option과 severity를 설정했다. raw `-Xep` compiler argument는 Error Prone plugin이 활성화되기 전에 javac에 전달되어 `invalid flag`가 발생했으므로 제거했다.

### 3.2 Testcontainers 버전

Testcontainers core의 최신 major와 `junit-jupiter`·`postgresql` module의 release line이 달라 version을 생략할 수 없었다. module metadata를 확인해 두 test dependency를 `1.21.4`로 명시했다. 이후 P1.2에서 PostgreSQL Testcontainers 실제 startup test로 다시 검증한다.

### 3.3 Modulith event serializer와 schema

`spring-modulith-events-jdbc`만 추가하면 context startup 중 `EventSerializer` bean이 없어 실패했다. `spring-modulith-events-jackson`을 추가하고, H2 context test에서 event publication schema 초기화를 켰다. 첫 테스트 종료 시 event table이 없던 경고도 사라져 `No publications outstanding!`으로 종료된다.

### 3.4 Java 25와 formatter 호환성

Google Java Format `1.25.2`는 Java 25 compiler API와 호환되지 않아 `DeferredDiagnosticHandler.getDiagnostics()` `NoSuchMethodError`가 발생했다. formatter를 `1.36.1`로 올리고 `spotlessApply` 후 `spotlessCheck`를 통과시켰다.

## 4. 검증 evidence

실행한 명령과 결과:

```text
./gradlew --version                         Gradle 9.7.0 / Launcher JVM 25.0.4
./gradlew clean check                       BUILD SUCCESSFUL
./gradlew integrationTest                   contextLoads PASSED
./gradlew modulithTest                      contextLoads PASSED
./gradlew migrationTest                    contextLoads PASSED
./gradlew performanceTest                  contextLoads PASSED
```

등록된 task는 `integrationTest`, `modulithTest`, `migrationTest`, `performanceTest`, `spotlessCheck`, `jacocoTestReport`다. 아직 PostgreSQL·PostGIS·MinIO는 연결하지 않았으므로 이번 evidence를 database baseline 통과로 과장하지 않는다.

## 5. 결과와 다음 질문

빈 Spring context와 quality gate를 Wrapper로 재현할 수 있게 됐다. 다음 P1.2에서는 H2 test convenience를 PostgreSQL 18·PostGIS 3.6 Testcontainers와 Flyway authority로 대체한다. 그때 `ddl-auto=validate`, least-privilege role, event/session schema와 timezone을 실제 database evidence로 검증한다.

## 면접 답변

### 30초

처음에는 기능보다 재현 가능한 build 경계를 먼저 만들었습니다. Java 25 toolchain과 Gradle Wrapper를 고정하고 Spring Boot·Modulith·JPA/jOOQ·Flyway·Session JDBC를 의존성으로 세운 뒤, context test와 Spotless·Error Prone·NullAway·JaCoCo gate를 통과시켰습니다. 구현 중 Java 25 formatter 호환성과 Modulith serializer 문제를 실제 로그로 확인하고 수정해 foundation을 안정화했습니다.

### 2분에서 이어갈 내용

이 선택들은 일부러 장애를 만들고 도입한 것이 아니라 ADR/TRD의 사전 제약입니다. 대신 build를 실제로 실행하는 과정에서 Error Prone DSL, Testcontainers module version, event serializer/schema와 formatter 호환성이라는 구체적 실패를 만났고, 각각 실패 로그와 재실행 결과를 남겼습니다. 그래서 이후 domain 기능에서 “무엇을 왜 선택했는지”와 “실제로 어떤 문제를 해결했는지”를 구분해 설명할 수 있습니다.

### 예상 꼬리 질문

- 왜 Java 25인데 Java 21이나 26으로 build하지 않았나?
- 왜 P1.1에서 H2를 사용하고 PostgreSQL은 P1.2로 미뤘나?
- JPA와 jOOQ를 동시에 선택한 근거는 언제 성능으로 검증할 것인가?
- Modulith event publication table이 없으면 어떤 실패가 발생하며 production schema는 누가 관리하는가?
- Error Prone과 NullAway가 단순 formatting과 무엇이 다른가?
