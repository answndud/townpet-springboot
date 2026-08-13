# Production readiness 재평가

작성일: 2026-08-13

## 결론

기존 계획은 Redis·Kafka 같은 확장 기술은 신중하게 보류하면서도, 실제 공개 배포에 필요한 SMTP·미디어·데이터 격리·복구·관측성을 너무 늦게 두고 있었다. 현재 목표는 “기술을 많이 붙이는 것”이 아니라 production에서 기능이 조용히 503으로 끊기거나 demo 데이터가 노출되지 않는 상태를 만드는 것이다.

## 현재 코드에서 확인한 사실

| 영역 | 확인된 상태 | 위험 |
|---|---|---|
| 계정 이메일 | production은 SMTP delivery를 사용하도록 연결했고 설정 누락 시 fail-fast | 실제 provider credential·deliverability 검증이 남음 |
| 미디어 | production은 private MinIO adapter·Caddy media domain·paired backup 경로를 갖춤 | 실제 DNS/TLS와 fresh-volume browser 검증이 남음 |
| demo identity | `V003`이 migration 중 4개 계정을 생성 | flag가 login만 막아도 row·소유 콘텐츠가 남을 수 있음 |
| demo content | gathering·notification 등 migration insert 존재 | web 노출 전 정리하지 않으면 공개 데이터로 보일 수 있음 |
| backup | PostgreSQL·MinIO paired backup/restore script와 manifest checksum 추가, fresh-volume rehearsal은 진행 중 | 실제 VPS offsite retention은 아직 미확정 |
| observability | health, trace log, SMTP delivery failure log, backup manifest 확인 절차 추가 | 외부 collector와 장기 retention은 미도입 |

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

## 현재 구현 evidence

- SMTP: `SmtpAccountTokenDelivery`, AES-GCM encrypted event payload, production retry listener, local/test synchronous capture
- Media: `MinioObjectStorage`, private bucket initialization, presigned upload URL, direct frontend PUT path
- Compose: portfolio profile에 PostgreSQL·MinIO·backend·web health dependency 추가
- 검증: `./gradlew compileJava spotlessApply`, identity account tests, `frontend` typecheck, portfolio compose config

아직 완료로 주장하지 않는 항목: MinIO CORS·public media domain의 실제 DNS/TLS, browser upload against a fresh portfolio volume, SMTP provider delivery, 실제 VPS offsite retention.

## 운영 backup slice evidence

`deploy/backup-portfolio.sh`는 PostgreSQL custom dump와 MinIO bucket mirror를 같은 UTC backup id 디렉터리에 저장하고 manifest와 SHA-256 checksum을 함께 생성한다. `deploy/restore-portfolio.sh`는 checksum 검증과 명시적 destructive confirmation 이후 DB와 bucket을 함께 복원한다.

로컬 Docker의 별도 `townpet_restore_test` database와 임시 MinIO bucket으로 rehearsal했다. synthetic object 1개와 member row 4개가 paired backup에서 복원됐고 manifest 검증이 통과했다. 원래 local DB와 bucket은 rehearsal 뒤 복원 대상과 임시 object를 제거해 변경하지 않았다. 이 결과는 offsite backup, retention, 실제 VPS volume 복구를 증명하지 않는다.

## 면접에서 설명할 trade-off

처음부터 Kafka와 Redis를 넣지 않았다. PostgreSQL transaction과 Modulith event registry만으로 현재 트래픽·일관성 요구를 만족하고, 실제 saturation이나 외부 consumer backlog가 생길 때만 운영 복잡도를 늘리기로 했다. 반대로 SMTP와 object storage는 기술 확장이 아니라 현재 API가 production에서 실패하는 필수 기능이므로 배포 전에 구현하기로 재분류했다.

## 상태 기록 규칙

이 문서는 정책 재평가의 기준 기록이다. 실제 구현·실패 원인·복구 수치가 생기면 `engineering-story.md`, `technical-notes.md`, `docs/performance/`의 canonical 문서에 결과만 추가한다. 단순 파일 생성이나 통과한 테스트 목록은 별도 report로 만들지 않는다.
