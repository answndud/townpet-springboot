# TownPet 면접·학습 보고서

이 디렉터리는 TownPet Springboot를 개발하면서 얻은 제품 이해, 기술 지식, 문제 해결 과정과 면접 답변을 실제 evidence에 연결해 축적한다. 운영 원칙과 작성 형식은 [`../../AGENTS.md`](../../AGENTS.md)의 `면접·학습 보고서 운영`을 따른다.

현재는 application scaffold 이전이므로 보고서 체계만 생성했다. 구현·실험·측정 근거가 생기는 `PLAN.md` P1.1부터 필요한 문서를 점진적으로 추가한다. 아직 하지 않은 작업을 경험처럼 작성하지 않는다.

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

## 기능 Coverage

| Actor·Journey | Domain rule | API·Data | Test evidence | Report | 준비도 |
|---|---|---|---|---|---|
| 전체 기준선 | [`../PRD.md`](../PRD.md)와 parity matrix에서 추적 예정 | 미구현 | 미구현 | 미작성 | captured |

## 기술 Coverage

| 개념 | 도입 출처 | 적용 위치 | 대안·Failure mode | Evidence | Report | 준비도 |
|---|---|---|---|---|---|---|
| 목표 기술 전체 | [`../TRD.md`](../TRD.md), [`../../ADR.md`](../../ADR.md) | 설계 단계 | ADR에 기록 | 구현 evidence 없음 | 미작성 | captured |

준비도는 `captured → understood → evidenced → rehearsed` 순서로만 올린다.
