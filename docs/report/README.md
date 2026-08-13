# TownPet 포트폴리오 보고서

이 폴더는 구현 내역을 모두 보관하는 작업 일지가 아니다. 면접에서 설명할 가치가 있는 설계 판단, 실제로 겪은 문제, 검증 근거만 선별해 유지한다. 제품 요구사항은 [`../PRD.md`](../PRD.md), 기술 경계는 [`../TRD.md`](../TRD.md), 결정 자체는 [`../../ADR.md`](../../ADR.md), 현재 작업은 [`../../PLAN.md`](../../PLAN.md)가 기준이다.

## 읽는 순서

1. [`engineering-story.md`](engineering-story.md): 어떤 순서로 기반을 만들었고 실제 실패에서 무엇을 바꿨는지
2. [`technical-notes.md`](technical-notes.md): 현재 코드에서 설명할 수 있는 핵심 Java·Spring·DB·보안·테스트 지식
3. 성능 측정 계획과 결과는 [`../performance/README.md`](../performance/README.md)를 참고한다.
4. 구현 범위와 전체 architecture는 canonical 문서와 [`../architecture/module-map.md`](../architecture/module-map.md), [`../parity/matrix.md`](../parity/matrix.md)를 참고한다.

5. 배포 전 위험과 아직 결정하지 않은 운영 항목은 [`release-readiness.md`](release-readiness.md)에서 확인한다.
6. 면접 직전의 말하기 순서·기능 흐름·질문 답변은 [`interview-prep.md`](interview-prep.md)를 사용한다.

별도 interview question bank는 P2 핵심 도메인 구현 후 만든다. 지금 만들면 아직 구현하지 않은 기능을 경험처럼 암기하거나 같은 설명을 복제할 가능성이 크다.

`interview-prep.md`는 질문 bank만 모은 문서가 아니다. 프로젝트를 30초·2분·deep-dive로 설명하는 순서, 실제 사용자 흐름, 실패에서 수정한 경계, 수치 근거와 미완료 범위를 한곳에서 복기하기 위한 문서다. 질문 답변은 반드시 현재 코드와 결과 문서에 연결한다.

## 문서화 기준

다음 중 하나에 해당할 때만 기존 문서를 갱신한다.

- 테스트·운영·측정에서 예상과 다른 결과가 나왔고 재사용 가능한 원인을 찾았다.
- 보안, transaction, module/data ownership처럼 면접에서 반드시 설명해야 할 경계가 실제 코드로 구현됐다.
- 두 개 이상의 현실적인 대안을 비교하고 선택했으며 trade-off가 남는다.
- 성능·복구·migration·동시성 결과처럼 수치나 재현 명령이 생겼다.
- 큰 사용자 여정 또는 PLAN phase가 닫혀 전체 구조 설명이 달라졌다.

다음은 별도로 기록하지 않는다.

- 단순 CRUD, DTO·파일 목록, formatting, dependency 추가
- 테스트가 통과했다는 사실만 있는 작업
- PLAN 완료 항목이나 commit message의 반복
- 공식 문서 일반론, 아직 구현하지 않은 미래 설계
- 같은 내용을 evolution·knowledge·interview 문서에 다시 쓰는 것

새 파일은 기존 두 문서에 자연스럽게 넣을 수 없고, 독립적으로 반복해서 참고할 주제일 때만 만든다. 한 작업이 끝날 때마다 문서를 만드는 규칙은 없다.

## 현재 근거 지도

| 설명할 주제 | 직접 근거 | 상태 |
|---|---|---|
| 재현 가능한 Java·Spring 기반 | `build.gradle.kts`, Wrapper, `clean check` | evidenced |
| PostgreSQL schema authority와 권한 분리 | Flyway V001~V003, Compose, `DatabaseBaselineTest` | evidenced |
| Modular monolith 경계 | module package, Modulith·ArchUnit tests | evidenced |
| React/Vite·parity gate | frontend API client, parity matrix, Vitest·Playwright·smoke | evidenced |
| Session·CSRF·onboarding·RBAC·password/email lifecycle | Identity code, V004~V005, JDBC session security tests | evidenced |
| Credentials 전체 auth parity | PostgreSQL·Spring·Vite Playwright와 JDBC row 대사 | evidenced |
| 게시·분실·거래·지역정보·모임·신고 핵심 도메인 | 관련 controller, migration, React flows, migrationTest/Vitest | evidenced |
| 동시성·DB 불변식 | upsert, row lock, partial unique index, `ReleaseCandidateQueryPlanTest` | evidenced |
| 대량 moderation·오류 계약·trace | bulk update, security problem handler, `RequestTraceFilter` | evidenced |
| 성능 개선과 확장 기술 판단 | `docs/performance/results/`, k6 raw output | evidenced |
| 배포 운영 | `release-readiness.md`, Compose·backup scripts | partial; VPS 미검증 |

`evidenced`는 코드와 재현 가능한 test/migration이 있을 때만 사용한다. 면접 답변을 실제로 연습한 뒤에만 별도 `rehearsed` 상태를 도입한다.

## 사건 기록 형식

새로운 사건을 추가할 때는 다음 순서를 지킨다.

1. 상황: 실제 실패·제약·관찰 결과
2. 선택: 검토한 대안과 선택 이유
3. 구현 근거: 코드·migration·설정 위치
4. 검증: 재현 명령·테스트·측정 결과
5. trade-off·한계: 얻은 것과 아직 검증하지 않은 것

단순 CRUD, 파일 목록, 통과한 테스트 목록, 구현하지 않은 미래 설계는 report에 추가하지 않는다. 성능 수치와 원본 산출물은 `docs/performance/`가 소유하고 report에서는 링크만 둔다.
