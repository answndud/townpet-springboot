# Engineering Story

TownPet을 단순히 Spring Boot 문법으로 옮기는 대신, 기존 49개 page·55개 API의 사용자 경험을 유지하면서 backend write ownership을 Java로 이전하고 있다. 아래에는 구현 순서 전체가 아니라 설계가 실제로 달라진 순간만 남긴다.

## 1. 기능보다 재현 가능한 실행 경계를 먼저 만들었다

빈 저장소에서 Java 25 toolchain, Gradle Wrapper, Spring Boot·Modulith, 정적 분석과 test task를 먼저 구성했다. 이는 성능 문제를 겪고 도입한 것이 아니라 팀이 없어도 local과 CI의 출발점을 같게 만들기 위한 사전 결정이었다.

실행 과정에서는 Error Prone Kotlin DSL 오용, Testcontainers module version 불일치, Modulith `EventSerializer` 누락, Java 25와 formatter 비호환을 실제 build 실패로 확인했다. dependency를 무작정 낮추지 않고 각 책임에 맞는 plugin 설정·Jackson event module·formatter 버전으로 수정했다.

- 근거: `85c7725`, `./gradlew clean check`
- 배운 점: 사전 설계와 구현 중 발견한 문제를 구분해야 과장 없는 기술 서사가 된다.

## 2. Database schema owner를 Flyway 하나로 고정했다

초기에는 Modulith 자동 초기화가 `event_publication`을 먼저 생성해, Flyway가 history 없는 non-empty schema를 거부했다. 동시에 application role은 PostGIS extension을 만들 권한이 없어 startup이 실패했다.

extension 설치는 bootstrap admin, versioned table 변경은 Flyway, runtime DML은 application role로 분리했다. Hibernate는 운영 schema 생성자가 아니라 mapping 검증자로 제한했다. H2 context test와 실제 PostgreSQL Testcontainers migration test의 의미도 분리했다.

- 근거: `16e54aa`, Flyway V001~V003, `DatabaseBaselineTest`
- trade-off: local 구성 요소는 늘었지만 schema drift와 과도한 DB 권한을 build 단계에서 드러낼 수 있다.

## 3. 17개 모듈을 만들되 물리적 분산은 하지 않았다

기능이 쌓이기 전에 package를 write ownership 기준으로 나누고 Spring Modulith와 ArchUnit으로 cycle, entity·repository·web DTO 누출을 검사했다. Gradle multi-project나 microservice로 분리하지 않아 혼자 개발하는 비용을 억제했다.

OpenAPI는 Java controller와 TypeScript client 사이의 transport 계약으로만 사용하고 domain entity를 생성하지 않는다. 이 경계 덕분에 frontend와 backend가 서로 다른 언어여도 wire contract와 business model을 같은 것으로 오해하지 않는다.

- 근거: `a2d5096`, `4fd3776`, module/contract tests
- 되돌릴 조건: 독립 배포·팀 소유권·실측 부하가 생길 때만 물리 분리를 검토한다.

## 4. UI를 다시 디자인하지 않고 parity 측정 기반을 만들었다

Next.js server runtime을 제거하면서 React 19·Vite·React Router로 logo, header, palette, CTA와 responsive shell을 먼저 옮겼다. 49개 page와 55개 API를 기억으로 추적하지 않도록 inventory를 고정하고 UUID·timestamp·signed URL만 allowlist 방식으로 정규화한다.

backend, frontend, integration smoke, browser E2E를 계층으로 나눴다. smoke profile에서 H2가 runtime classpath에 없어 실패한 뒤 `developmentOnly`로 옮겨 production artifact와 test 편의를 분리했다.

- 근거: `a6c0607`, `8b9c910`, `d69dd81`, parity matrix와 CI workflow
- 배운 점: “새 화면이 열린다”보다 누락·환경 차이·의미 차이를 자동으로 찾는 기준이 먼저다.

## 5. 인증은 작은 사용자 여정으로 연결한 뒤 hardening했다

첫 slice에서 BCrypt credential, Spring Session JDBC, Cookie CSRF, 현재 회원·동네·프로필과 React login을 연결했다. 테스트에서 session 생성 전 `changeSessionId()` 호출 오류와 비인증 요청의 403/401 불일치를 발견해 실제 request lifecycle에 맞게 수정했다.

다음 단계에서 profile과 반려동물 목록을 한 transaction으로 저장하고 logout 뒤 같은 session이 401이 되는지 검증했다. CSRF cookie가 test 실행 순서에 따라 누락되는 현상은 token endpoint가 cookie 계약을 명시적으로 응답하도록 고쳤다. 공개 showcase는 실제 signup 대신 합성 MEMBER 3개와 MODERATOR 1개를 hash-only migration으로 만들고 운영 prefix를 role로 제한했다.

비밀번호 reset에서 전체 session revoke를 구현하며 더 근본적인 누락을 발견했다. `spring-session-jdbc` library와 session table만 있었고 Boot 4의 JDBC session auto-configuration starter가 없어 기존 테스트는 servlet memory session을 사용하고 있었다. 의존성을 `spring-boot-starter-session-jdbc`로 바꾸고 테스트도 `MockHttpSession` 직접 전달 대신 실제 `SESSION` cookie와 JDBC repository를 확인하도록 수정했다. Reset token은 SHA-256 hash·1시간 만료·1회 사용·optimistic version으로 저장하고, 성공 시 password 변경·audit·모든 JDBC session 삭제를 한 transaction 경계에서 수행한다.

- 근거: `482428d`, `c1f6155`, `461d4ad`, Flyway V004, `IdentityMemberControllerTest`
- 현재 한계: token 전달 경계, demo scoped reset과 전체 Credentials auth parity는 아직 완료되지 않았다.

## 면접에서 강조할 핵심

“처음부터 모든 기술을 넣었다”가 아니라, 사전 제약으로 정한 선택과 실제 실패 후 수정한 선택을 구분한다. 각 답변은 상황, 선택한 경계, 재현 가능한 test, 남은 trade-off 순서로 말한다. 아직 측정하지 않은 성능이나 구현하지 않은 기능은 성과로 주장하지 않는다.
