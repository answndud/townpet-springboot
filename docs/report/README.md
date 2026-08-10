# TownPet 면접·학습 보고서

이 디렉터리는 TownPet Springboot를 개발하면서 얻은 제품 이해, 기술 지식, 문제 해결 과정과 면접 답변을 실제 evidence에 연결해 축적한다. 운영 원칙과 작성 형식은 [`../../AGENTS.md`](../../AGENTS.md)의 `면접·학습 보고서 운영`을 따른다.

P1.1 application scaffold와 첫 검증 기록이 추가됐다. 이후에도 구현·실험·측정 근거가 생기는 slice마다 필요한 문서를 점진적으로 추가한다. 아직 하지 않은 작업을 경험처럼 작성하지 않는다.

## Document Index

| 문서 | 목적 | 상태 |
|---|---|---|
| `project-overview.md` | 제품·사용자·핵심 여정과 전체 구조 설명 | 근거 발생 시 생성 |
| `feature-map.md` | 기능·정책·module/API·data·test 연결 | parity inventory와 함께 생성 |
| `architecture-walkthrough.md` | 대표 요청·transaction·event·failure 흐름 | 기반 구현 후 생성 |
| `evolution/EV-NNN-<slug>.md` | 한계·대안·개선·검증의 실제 chronology | 사건 발생 시 생성 |
| `knowledge/<topic>.md` | 프로젝트에 적용한 기술 개념과 failure mode | 개념 사용 시 생성 |
| `interview/question-bank.md` | 30초·2분·deep-dive 답변과 꼬리 질문 | evidence 축적 후 생성 |
| `interview/story-bank.md` | 문제 해결·실패·trade-off 사례 | evolution에서 선별 |
| `interview/gaps.md` | 아직 근거가 약하거나 답하지 못하는 질문 | 첫 slice부터 유지 |
| [`evolution/EV-001-p1-1-build-foundation.md`](evolution/EV-001-p1-1-build-foundation.md) | 첫 build foundation의 선택·실패·해결 chronology | P1.1 완료 |
| [`knowledge/java-gradle-spring-foundation.md`](knowledge/java-gradle-spring-foundation.md) | Java toolchain·Wrapper·Spring context·quality gate | P1.1 완료 |
| [`evolution/EV-002-p1-2-database-baseline.md`](evolution/EV-002-p1-2-database-baseline.md) | PostgreSQL·Flyway·Compose baseline의 문제·해결 chronology | P1.2 완료 |
| [`knowledge/postgres-flyway-baseline.md`](knowledge/postgres-flyway-baseline.md) | extension·권한·migration·session/event schema 개념 | P1.2 완료 |
| [`evolution/EV-003-p1-3-module-boundaries.md`](evolution/EV-003-p1-3-module-boundaries.md) | 17개 모듈 경계와 architecture 검증의 문제·선택·증거 | P1.3 완료 |
| [`knowledge/spring-modulith-architecture.md`](knowledge/spring-modulith-architecture.md) | Spring Modulith·ArchUnit과 모듈 경계 면접 노트 | P1.3 완료 |

## 기능 Coverage

| Actor·Journey | Domain rule | API·Data | Test evidence | Report | 준비도 |
|---|---|---|---|---|---|
| 전체 기준선 | [`../PRD.md`](../PRD.md)와 parity matrix에서 추적 예정 | 미구현 | 미구현 | 미작성 | captured |

## 기술 Coverage

| 개념 | 도입 출처 | 적용 위치 | 대안·Failure mode | Evidence | Report | 준비도 |
|---|---|---|---|---|---|---|
| Java 25·Gradle Wrapper | [`../TRD.md`](../TRD.md), ADR 결정 | P1.1 build | Java 21/26 환경 차이, Wrapper 재현성 | `./gradlew clean check` | [`knowledge/java-gradle-spring-foundation.md`](knowledge/java-gradle-spring-foundation.md) | evidenced |
| Spring Boot·Modulith foundation | [`../TRD.md`](../TRD.md) | application context·event registry | serializer·schema 초기화 | context test·verification tasks | [`evolution/EV-001-p1-1-build-foundation.md`](evolution/EV-001-p1-1-build-foundation.md) | evidenced |
| PostgreSQL·PostGIS·Flyway | [`../TRD.md`](../TRD.md), ADR 결정 | Compose·V001·Testcontainers | extension 권한, schema authority, ARM image | migration test·bootRun·health·DB query | [`evolution/EV-002-p1-2-database-baseline.md`](evolution/EV-002-p1-2-database-baseline.md) | evidenced |
| Spring Modulith·ArchUnit | ADR-0011 | 17개 package 경계·architecture tests | 물리 multi-project, shared business common | `./gradlew modulithTest` | [`knowledge/spring-modulith-architecture.md`](knowledge/spring-modulith-architecture.md) | evidenced |

준비도는 `captured → understood → evidenced → rehearsed` 순서로만 올린다.
