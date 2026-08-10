# Technical Notes

이 문서는 일반 기술 사전이 아니라 현재 TownPet 코드로 설명할 수 있는 개념만 요약한다. 세부 결정은 ADR/TRD, 진행 순서는 PLAN을 기준으로 한다.

## Java·Gradle·Spring 기반

- Gradle Wrapper는 build tool 버전을 고정하고 Java toolchain은 compile JDK를 선택한다. `options.release=25`는 생성 bytecode/API 수준을 맞춘다.
- `contextLoads`는 bean graph와 auto-configuration 검증이지 PostgreSQL 호환성이나 기능 완료 증거가 아니다.
- Spotless는 형식, Error Prone·NullAway는 compile-time 오류 패턴과 null 계약, JaCoCo는 실행된 test coverage를 담당한다. 서로 대체하지 않는다.
- 근거: `build.gradle.kts`, `TownPetApplicationTests`, `./gradlew clean check`

## PostgreSQL·Flyway

- Flyway만 versioned schema를 변경하고 Hibernate `ddl-auto=validate`는 mapping drift를 확인한다.
- PostGIS·citext extension은 bootstrap admin, migration과 runtime은 제한된 application role 책임이다.
- H2는 빠른 web/context test, Testcontainers PostgreSQL은 실제 dialect·constraint·migration test, Compose는 반복 가능한 local runtime에 사용한다.
- Modulith event publication과 Spring Session table도 Flyway가 소유해 자동 schema 초기화와 충돌하지 않게 한다.
- 근거: `deploy/compose`, Flyway V001~V003, `DatabaseBaselineTest`

## Modular monolith와 API 계약

- 모듈은 기술 layer가 아니라 변경 이유와 data write ownership으로 나눈다. 다른 모듈의 JPA entity/repository 대신 식별자, 공개 application API 또는 event를 사용한다.
- Spring Modulith는 module/cycle을, ArchUnit은 내부 package와 type 노출 규칙을 검사한다.
- OpenAPI는 HTTP transport의 source of truth다. Java·TypeScript transport 코드는 생성하지만 aggregate·entity·repository는 생성하지 않는다.
- ProblemDetail은 status와 기계 판독 code, traceId, field error를 한 오류 계약으로 묶는다.
- 근거: `ModularityTest`, `LayerRulesTest`, `api/openapi/townpet.yaml`, `OpenApiContractTest`

## React·Vite와 parity

- Vite는 정적 frontend build와 local `/api` proxy만 담당하고 production Node server 역할을 갖지 않는다.
- parity는 내부 구현 동일성이 아니라 같은 actor·state에서 사용자가 관찰하는 의미가 같은지 비교한다.
- UUID·시간·서명 URL 같은 volatile field만 allowlist로 normalize한다. status, permission, business field는 그대로 비교한다.
- backend/frontend 단위 gate와 실제 두 프로세스 smoke, browser E2E를 분리해 실패 위치와 실행 비용을 조절한다.
- 근거: `frontend`, `docs/parity/matrix.yaml`, `ParityInventoryTest`, CI workflow, smoke script

## Session·CSRF·authorization

- 브라우저에는 opaque session identifier만 두고 SecurityContext는 Spring Session JDBC에 저장한다. 로그인 시 session을 먼저 만든 뒤 ID를 교체해 fixation을 방어한다.
- CSRF token은 `XSRF-TOKEN` cookie와 응답 body로 전달하고 React가 변경 요청의 `X-XSRF-TOKEN` header로 돌려준다.
- logout은 cookie UI만 바꾸는 것이 아니라 서버 session을 invalidate한다. 같은 session으로 보호 API를 다시 호출해 401을 확인한다.
- 현재 회원 ID는 request body/path가 아니라 authenticated principal에서 가져와 profile·pet IDOR 표면을 줄인다.
- 401은 인증 부재/실패, 403은 인증됐지만 role이 부족한 경우다. 운영 prefix는 MODERATOR만 허용한다.
- demo identity는 실제 사용자 데이터가 아닌 합성 fixture이며 password 평문은 저장하지 않는다.
- 근거: `SecurityConfig`, `SessionController`, `MemberController`, V002~V003, `IdentityMemberControllerTest`

## 현재 학습·증거의 빈틈

- password reset/verification token의 hash·expiry·single-use·session revoke
- OAuth collision/link/unlink를 실제 provider 없이 검증하는 stub contract
- publication transaction과 media lifecycle
- event retry/idempotency, concurrency mutation, jOOQ read model
- 성능 수치, query plan, backup/restore와 배포 관측 evidence

이 항목들은 구현·실험 근거가 생길 때 해당 절에 추가한다. 미리 일반론을 채우지 않는다.
