# EV-003 · P1.3 모듈 경계와 아키텍처 검증

## 문제와 한계

초기 skeleton은 Spring Boot를 실행할 수 있었지만, 기능을 추가하면 모든 domain package가 서로 참조하는 계층형 monolith로 쉽게 퇴행할 수 있었다. 특히 entity·repository를 공유하면 write owner와 변경 이유가 불명확해지고, 나중에 parity를 검증할 때 누가 정책을 책임지는지 설명하기 어려워진다.

## 선택과 구현

ADR-0011의 17개 bounded context를 `com.townpet` 직속 package로 선언하고, 각 모듈에 `api` named interface를 예약했다. `ApplicationModules.verify()`는 모듈 cycle과 비공개 타입 의존을 검사하며, ArchUnit 규칙은 `domain`, `infrastructure`, `web` 구현 계층을 모듈 계약으로 노출하지 못하게 한다. 모듈 지도는 Mermaid로 문서화해 코드·문서·면접 설명의 기준을 일치시켰다.

## 검증 결과

`JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew modulithTest --no-daemon` 실행 결과, 17개 모듈 detection, Spring context, Testcontainers migration, cycle 검증과 두 ArchUnit 규칙이 모두 통과했다.

## 다음에 측정할 것

실제 vertical slice가 추가될 때 모듈 간 의존 방향이 이 지도와 일치하는지 확인하고, 공개 API가 아닌 entity·repository 참조를 의도적으로 추가하는 회귀 테스트를 유지한다. 함께 변경되는 불변식이 반복되면 모듈 merge/split은 새 ADR로 결정한다.
