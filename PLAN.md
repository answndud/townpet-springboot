# PLAN.md

## Goal

배포 전 TownPet의 실제 기능·보안·운영 경계를 완성한다. 공개 환경은 실제 개인정보와 공개 demo 계정을 사용하지 않는 portfolio sandbox로 유지한다. SMTP 계정 복구와 private media는 실제 production 경로로 구현하고, Redis·Kafka 같은 확장은 측정된 병목이 생길 때만 검토한다.

## Active

1. **정책 문서와 production 경계를 정합화한다.**
   - ADR-0013/0014/0022/0023/0029/0030, PRD, TRD를 현재 결정과 맞춘다.
   - 공개 demo 계정·콘텐츠 없음, public signup 비활성, SMTP 활성, MinIO private media를 명시한다.
   - 완료 조건: 코드·compose·문서의 production 기본값이 서로 모순되지 않는다.

2. **Production data isolation을 구현한다.**
   - migration에 포함된 local demo identity/content가 web 노출 전에 제거되는 명시적 초기 sanitize를 추가한다.
   - 일반 데이터가 있는 DB에서 중단하는 dry-run과 private operator bootstrap 절차를 제공한다.
   - 완료 조건: 새 volume에서 migration 후 demo row·demo credential이 없음을 검증하고, local/test fixture는 유지된다.

3. **SMTP account delivery를 완성한다.**
   - Spring Mail 기반 verification/password reset 발송, commit 이후 처리, 재시도·idempotency, rate limit과 Mailpit local profile을 구현한다.
   - public signup은 계속 비활성으로 둘 수 있지만 이메일 인증·복구 API가 production에서 503이 되지 않게 한다.
   - 완료 조건: 성공·만료·중복·SMTP 실패·session revoke 흐름이 재현 가능하다.

4. **Production media를 MinIO presigned flow로 전환한다.**
   - private bucket, presigned PUT/GET, metadata finalize, 권한 검증, orphan/expiration cleanup과 object backup을 구현한다.
   - 완료 조건: 이미지 업로드부터 게시물 연결·비공개 조회·삭제까지 browser 흐름이 동작하고 public URL로 우회할 수 없다.

5. **최소 운영 기반을 완성한다.**
   - PostgreSQL·MinIO paired backup/restore, health/readiness, 구조화 로그, 자원·backup·SMTP 실패 확인을 추가한다.
   - `docs/runbooks/`에 배포, 재시작, backup/restore, 초기 sanitize, secret 교체, rollback을 기록한다.
   - 완료 조건: fresh volume과 복구 volume에서 재현 가능한 절차와 evidence가 있다.

6. **배포 전 기능·성능 release gate를 실행한다.**
   - 권한·주요 사용자 여정·media/email 흐름과 VPS 유사 workload를 한 번에 검증한다.
   - Redis·Kafka·검색엔진은 병목과 운영 이득이 측정될 때만 별도 ADR로 승격한다.
   - 완료 조건: 측정한 결과와 미측정 한계를 `docs/report/`에 남기고 배포 여부를 판단한다.

## Deferred (trigger가 생길 때만)

- Redis: DB/cache/session 병목이 반복 재현될 때
- Kafka: PostgreSQL event publication으로 감당할 수 없는 외부 consumer·처리량이 생길 때
- Elasticsearch/SearchDocument: 검색 corpus·latency·정확도가 PostgreSQL 기준을 넘을 때
- 개인화 ranking projection: 실제 ranking 요구와 refresh 비용이 생길 때
- Kubernetes/microservice, 고급 WAL/PITR/HA, social login, 실제 public signup
- Marketplace 안전 규칙: public listing과 실제 사용자 입력을 열 때
- Hetzner 실제 DNS/TLS 배포: 로컬 release rehearsal이 끝난 뒤

## Working rules

- 기능은 작은 파일 단위가 아니라 충분한 vertical slice로 진행한다.
- 구현 중에는 가장 가까운 컴파일·기능 테스트만 실행하고, 큰 phase 종료 때 full gate를 실행한다.
- 중요한 결정·실패 원인·재현 가능한 수치만 `docs/report/`에 기록한다.
- 적용된 Flyway migration은 수정하지 않고 새 migration 또는 명시적 운영 스크립트를 추가한다.
