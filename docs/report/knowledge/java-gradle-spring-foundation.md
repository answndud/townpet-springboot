# Java·Gradle·Spring Foundation

## Java toolchain

`build.gradle.kts`의 `java.toolchain`과 `JavaCompile.options.release`는 개발자의 현재 `java` 명령과 별개로 compile language level과 bytecode target을 선언한다. 둘을 함께 둔 이유는 Gradle이 사용할 JDK와 compiler가 생성할 target을 명시적으로 맞추기 위해서다. 이 프로젝트의 local prerequisite는 Java 25이며, P1.1에서 `./gradlew --version`의 Launcher/Daemon JVM 25.0.4를 확인했다.

## Gradle Wrapper

Wrapper는 repository의 `gradle-wrapper.properties`가 지정한 Gradle distribution을 다운로드해 `./gradlew`로 실행한다. system Gradle 설치에 의존하지 않는 CI·fresh clone 진입점이지만, Java toolchain 자체는 별도 JDK 설치 또는 resolver 정책이 필요하므로 둘을 혼동하지 않는다.

## Spring Boot context

`@SpringBootApplication`은 component scan과 auto-configuration의 시작점이다. P1.1의 `contextLoads`는 실제 기능 test가 아니라 dependency graph와 auto-configuration이 최소 환경에서 함께 올라오는지 확인한다. H2는 이 단계의 빠른 context test용이며 PostgreSQL 스키마와 호환된다는 의미가 아니다.

## Spring Modulith event foundation

`spring-modulith-events-jdbc`는 publication registry persistence를 제공하지만 event serialization 구현과 schema 초기화가 별도다. 그래서 Jackson event module과 test schema 설정을 함께 넣었다. 이후 P1.2에서는 H2 설정을 production과 같은 Flyway/PostgreSQL authority로 바꾼다.

## Quality gates

- Spotless: source formatting drift를 막는다.
- Error Prone: javac compilation 중 정적 오류 패턴을 검사한다.
- NullAway: `com.townpet` package의 nullability contract를 검사한다.
- JaCoCo: test execution coverage report를 만든다.

P1.1에서는 gate가 등록되고 빈 context에 적용되는 것까지 검증했다. coverage threshold와 domain별 rule은 실제 source가 쌓이는 단계에서 측정값과 함께 추가한다.

## Interview prompts

- Gradle Wrapper와 Java toolchain은 각각 어떤 문제를 해결하는가?
- Spring context test가 통과해도 database migration이 안전하다고 말할 수 없는 이유는 무엇인가?
- Modulith event registry와 일반 application event의 차이는 무엇인가?
- 정적 분석을 build gate로 두면 어떤 trade-off가 생기는가?
