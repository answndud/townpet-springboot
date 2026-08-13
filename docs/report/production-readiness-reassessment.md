# Production readiness 재평가

작성일: 2026-08-13

## 결론

기존 계획은 Redis·Kafka 같은 확장 기술은 신중하게 보류하면서도, 실제 공개 배포에 필요한 SMTP·미디어·데이터 격리·복구·관측성을 너무 늦게 두고 있었다. 현재 목표는 “기술을 많이 붙이는 것”이 아니라 production에서 기능이 조용히 503으로 끊기거나 demo 데이터가 노출되지 않는 상태를 만드는 것이다.

## 현재 코드에서 확인한 사실

| 영역 | 확인된 상태 | 위험 |
|---|---|---|
| 계정 이메일 | production은 `UnavailableAccountTokenDelivery`를 사용 | 인증·비밀번호 복구가 production에서 실패할 수 있음 |
| 미디어 | production은 `UnavailableObjectStorage`, local은 filesystem 중심 | 공개 업로드가 production에서 실패하고 presigned 계약도 완성되지 않음 |
| demo identity | `V003`이 migration 중 4개 계정을 생성 | flag가 login만 막아도 row·소유 콘텐츠가 남을 수 있음 |
| demo content | gathering·notification 등 migration insert 존재 | web 노출 전 정리하지 않으면 공개 데이터로 보일 수 있음 |
| backup | guarded PostgreSQL script는 있으나 MinIO paired backup과 restore rehearsal 부족 | DB와 object metadata가 서로 어긋날 수 있음 |
| observability | health와 일부 metric은 있으나 SMTP·backup·MinIO 운영 신호 부족 | 장애를 사용자의 5xx로 처음 발견할 수 있음 |

## 재분류

### 배포 전 필수

- production demo sanitize와 private operator bootstrap
- SMTP verification/password reset delivery
- private MinIO와 presigned media upload/read/delete
- PostgreSQL·MinIO paired backup/restore
- health/readiness, structured log, 최소 실패 신호
- 배포·재시작·복구·rollback runbook

### interactive 공개 전 조건부 필수

- public signup abuse 방어
- 공개 user-generated content moderation 정책
- marketplace safety rule
- privacy/retention/terms 화면

현재 public signup과 public demo 계정을 열지 않으므로 이 그룹은 production read-only sandbox의 선행 조건이 아니다.

### 현재도 보류가 맞는 항목

- Redis, Kafka
- Elasticsearch/SearchDocument
- versioned personalization ranking
- Kubernetes/microservice
- 고급 WAL/PITR/HA
- Kakao/Naver OAuth

보류 근거는 기술 선호가 아니라 현재 성능 측정에서 해당 병목·event backlog·검색 규모가 재현되지 않았기 때문이다.

## 구현 순서

1. 문서·ADR·compose 기본값 정합화
2. production data isolation
3. SMTP account delivery
4. MinIO media vertical slice
5. backup·observability·runbook
6. fresh-volume 및 배포 전 release gate

## 면접에서 설명할 trade-off

처음부터 Kafka와 Redis를 넣지 않았다. PostgreSQL transaction과 Modulith event registry만으로 현재 트래픽·일관성 요구를 만족하고, 실제 saturation이나 외부 consumer backlog가 생길 때만 운영 복잡도를 늘리기로 했다. 반대로 SMTP와 object storage는 기술 확장이 아니라 현재 API가 production에서 실패하는 필수 기능이므로 배포 전에 구현하기로 재분류했다.

## 상태 기록 규칙

이 문서는 정책 재평가의 기준 기록이다. 실제 구현·실패 원인·복구 수치가 생기면 `engineering-story.md`, `technical-notes.md`, `docs/performance/`의 canonical 문서에 결과만 추가한다. 단순 파일 생성이나 통과한 테스트 목록은 별도 report로 만들지 않는다.
