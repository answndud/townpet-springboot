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
- Media authorization: owner만 attached asset signed read URL을 받고 다른 member는 404가 되는 `MediaControllerTest` 추가
- Frontend media client: production absolute presigned URL은 direct PUT하고 local/test relative URL은 기존 server upload로 fallback
- 운영 경계 재감사: production sanitize는 member-owned 데이터만 지우고 익명 `search_event`·`acquisition_event`는 보존한다. private moderator bootstrap은 psql 변수 치환이 필요한 `DO` 블록을 제거하고 중복 email을 transaction 안에서 거절한다.
- 계정 토큰 남용 방어: password reset·email verification 발급을 member별 시간당 3회로 제한한다. 제한되거나 존재하지 않는 email도 같은 `202` 응답을 사용해 enumeration을 만들지 않는다.

아직 완료로 주장하지 않는 항목: MinIO CORS·public media domain의 실제 DNS/TLS, full browser UI flow against a fresh portfolio volume, 실제 SMTP provider deliverability, 실제 VPS offsite retention.

## 재감사에서 수정한 위험

초기 sanitize 스크립트는 member와 연결되지 않은 익명 telemetry 테이블까지 전체 삭제하고 있었다. production DB에 이미 의미 있는 분석 데이터가 있으면 demo 정리라는 목적을 넘어서는 삭제가 되므로 해당 문장을 제거하고 demo-scoped `web_vital_metric`만 유지했다. 이 결정으로 telemetry 보존과 demo identity/content 격리를 분리했다.

private moderator bootstrap의 중복 검사도 PostgreSQL `DO` 블록 안에서 psql 변수를 참조하고 있어 실행 환경에 따라 치환되지 않을 수 있었다. transaction 내부 `SELECT ... \gset`와 psql 조건문으로 바꿔 중복 email을 명시적으로 중단하도록 했다. 평문 비밀번호는 여전히 받지 않는다.

SMTP 발급 endpoint에는 member별 시간당 3회 상한을 추가했다. IP 기반 전역 limiter나 Redis는 현재 단일 인스턴스 목표에 비해 운영 복잡도가 크므로 도입하지 않았고, 공개 가입을 열기 전에는 edge rate limit과 별도 ADR을 추가로 검토한다.

## 운영 backup slice evidence

`deploy/backup-portfolio.sh`는 PostgreSQL custom dump와 MinIO bucket mirror를 같은 UTC backup id 디렉터리에 저장하고 manifest와 SHA-256 checksum을 함께 생성한다. `deploy/restore-portfolio.sh`는 checksum 검증과 명시적 destructive confirmation 이후 DB와 bucket을 함께 복원한다.

이번 재감사에서 backup 디렉터리를 `umask 077`로 생성하고 빈 PostgreSQL dump를 성공으로 취급하지 않도록 보강했다. rollback runbook도 restore 중 backend/web 쓰기를 중지하고 backend health를 먼저 확인하는 순서를 명시했다.

Portfolio compose는 필수 secret/domain이 없으면 config 단계에서 실패한다. 예시 값을 주입한 `portfolio.yml` 단독 config와 base+`smtp-local.yml` overlay config를 각각 통과시켜, 누락 거절과 local overlay 구성을 모두 확인했다.

로컬 Docker의 별도 `townpet_restore_test` database와 임시 MinIO bucket으로 rehearsal했다. synthetic object 1개와 member row 4개가 paired backup에서 복원됐고 manifest 검증이 통과했다. 원래 local DB와 bucket은 rehearsal 뒤 복원 대상과 임시 object를 제거해 변경하지 않았다. 이 결과는 offsite backup, retention, 실제 VPS volume 복구를 증명하지 않는다.

## SMTP local integration evidence

Mailpit을 별도 Docker network에 띄우고 `smtp-local` profile backend를 새 image로 실행했다. 임시 synthetic account에 password reset을 요청해 HTTP `202`와 Mailpit inbox의 `TownPet 비밀번호 재설정` 메일, `TOWNPET_PUBLIC_BASE_URL` 링크를 확인했다. 임시 account·token·container는 검증 뒤 제거했다. 이는 SMTP provider의 TLS·SPF·DKIM·deliverability를 증명하지 않으며, 실제 provider secret을 저장하지 않는다.

## Fresh media volume evidence

기존 local volume과 분리한 PostgreSQL·MinIO·backend를 새 network/volume으로 시작하고 Flyway 61개 migration을 적용했다. bootstrap administrator가 `postgis`·`citext`를 provision하지 않은 첫 시도는 V001에서 중단됐고, portfolio init script의 extension 전제와 동일하게 provision한 뒤 재시작했다. 이 실패는 fresh-volume runbook에 반영할 운영 전제다.

임시 verified account로 presigned URL을 발급하고, network 내부 client가 MinIO에 4-byte JPEG를 직접 PUT한 뒤 backend finalize를 호출해 HTTP `200 READY`와 SHA-256 일치를 확인했다. `TOWNPET_MINIO_PUBLIC_ENDPOINT`는 backend가 접근할 수 있고 Caddy가 동일 host로 proxy해야 signature가 유지된다. fresh volume·direct upload·finalize는 검증했지만 실제 frontend browser/CORS와 외부 DNS/TLS는 아직 검증하지 않았다. 임시 containers, volumes, network와 account는 모두 제거했다. frontend Vitest 33개와 typecheck도 통과했다.

## Phase gate evidence

- `./gradlew check`: 성공
- `./gradlew migrationTest`: 성공
- `./scripts/validate-release-candidate.sh`: parity 104개 중 verified 95, excluded 9, pending 0
- frontend: frozen install, typecheck, Vitest 33개, production build와 bundle budget 성공
- portfolio/local SMTP compose config와 Caddy validate 성공

처음 `check` 실행에서는 Testcontainers PostgreSQL startup timeout으로 media test 2개가 실패했지만, 코드 오류가 아닌 Docker resource race를 확인한 뒤 같은 phase gate를 재실행해 전체 test와 `check`를 통과시켰다. 첫 실패도 숨기지 않고 이 문서에 남긴다.

이번 운영 경계 재감사 뒤 최종 gate에서 `./gradlew check migrationTest`가 5분 44초에 다시 통과했고, `scripts/*.sh`·`deploy/*.sh` 전체 shell syntax와 `git diff --check`도 통과했다. 변경 범위에 맞춘 identity 테스트(`IdentityMemberControllerTest`, `AccountTokenDeliveryUnavailableTest`)도 별도로 통과했다.

Frontend frozen install·typecheck·Vitest 33개·production build도 통과했다. bundle budget은 entry JS 290,374 bytes, gzip 89,043 bytes, CSS 49,774 bytes로 각각 320,000·100,000·50,000 byte 한도 안이다. `validate-release-candidate.sh`는 parity 104개(verified 95, excluded 9, pending 0)를 재확인했고, portfolio 및 SMTP-local Compose config와 전체 shell syntax도 통과했다.

Production profile의 `TOWNPET_EMAIL_ENABLED` 기본값도 제거했다. 이제 env가 누락되면 application binding 단계에서 시작이 실패하고, account delivery가 설정되지 않은 상태로 조용히 503을 내는 구성은 명시적으로 `false`를 넣은 경우에만 가능하다.

추가로 `scripts/validate-portfolio-env.sh`를 만들어 production env-file이 placeholder·example domain·비HTTPS public URL을 포함하거나 SMTP를 비활성화한 경우 Compose 전에 실패하게 했다. 이 검사는 secret 값을 출력하지 않으며 실제 SMTP credential의 유효성이나 DNS deliverability까지 증명하지는 않는다.

추가로 password reset 발급을 네 번 요청해도 네 요청 모두 `202 Accepted`이고 token row는 세 개만 생성되는 상한 테스트가 통과했다. 외부 SMTP·media domain·VPS 보존 정책은 [`docs/runbooks/external-production-checklist.md`](../runbooks/external-production-checklist.md)에 실제 배포 담당자가 채우는 미완료 전제로 분리했다.

Media cleanup의 현재 보장 범위도 명확히 했다. 운영 endpoint는 DB에 기록된 만료 `UPLOADING` asset을 최대 500개씩 object와 metadata에서 함께 제거한다. MinIO bucket 전체 inventory와 DB를 대조하는 무주물 object reconciliation은 현재 단일 portfolio sandbox의 필수 release gate로 승격하지 않았으며, orphan 증가가 관측되면 별도 storage inventory 작업으로 추가한다.

Runbook 상태도 실제 증거에 맞춰 조정했다. sanitize는 local DB dry-run과 shell syntax를 확인했지만 production apply는 아직 실행하지 않았으므로 “local 검증 완료·production 실행 전”으로 표시한다. 이는 운영 DB에 대한 destructive apply를 로컬 성공으로 과장하지 않기 위한 구분이다.

추가 재현에서 실제 `townpet-postgres`와 격리된 PostgreSQL 18 client container를 연결해 sanitize dry-run을 실행했다. demo member 4명과 연결된 각 콘텐츠 삭제 문장이 끝까지 실행된 뒤 `ROLLBACK`되었고, 마지막 verification block도 통과했다. 이 과정에서 heredoc 내부의 `#` 주석이 PostgreSQL 문법이 아니라는 결함을 발견해 `--` 주석으로 수정했다. local DB는 rollback으로 보존했다.

## 면접에서 설명할 trade-off

처음부터 Kafka와 Redis를 넣지 않았다. PostgreSQL transaction과 Modulith event registry만으로 현재 트래픽·일관성 요구를 만족하고, 실제 saturation이나 외부 consumer backlog가 생길 때만 운영 복잡도를 늘리기로 했다. 반대로 SMTP와 object storage는 기술 확장이 아니라 현재 API가 production에서 실패하는 필수 기능이므로 배포 전에 구현하기로 재분류했다.

## 상태 기록 규칙

이 문서는 정책 재평가의 기준 기록이다. 실제 구현·실패 원인·복구 수치가 생기면 `engineering-story.md`, `technical-notes.md`, `docs/performance/`의 canonical 문서에 결과만 추가한다. 단순 파일 생성이나 통과한 테스트 목록은 별도 report로 만들지 않는다.
