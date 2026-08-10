# Spring Modulith와 ArchUnit 면접 노트

## 왜 도입했나

패키지를 나누는 것만으로는 경계가 보장되지 않는다. Spring Modulith는 application module을 탐지하고 순환·허용되지 않은 의존을 build에서 검증한다. ArchUnit은 더 세밀한 규칙, 예를 들어 모듈 내부의 JPA infrastructure나 web DTO가 외부 계약으로 새어 나오는 것을 표현한다.

## 어떻게 쌓았나

처음부터 물리적 Gradle multi-project를 만들지 않았다. 배포 단위는 하나로 유지하면서 package boundary와 executable architecture test를 먼저 만들었다. 이후 실제 유스케이스에서 동기 API가 필요한지 durable event가 필요한지 측정하고, 독립 배포·소유권이 증명될 때만 구조를 분리한다.

## 면접 답변 포인트

- 모듈은 기술 계층이 아니라 변경 이유와 데이터 write ownership을 기준으로 나눴다.
- `common`을 business shared kernel로 만들지 않아 `User`, `Post`, 공용 repository가 모든 모듈을 결합하지 않게 했다.
- 모듈 간 JPA association 대신 identifier와 공개 API/event를 사용한다.
- 아키텍처 규칙도 테스트이므로 새 모듈·공개 타입이 생길 때 build가 즉시 피드백을 준다.
