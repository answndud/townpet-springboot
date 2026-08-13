# TownPet 운영 Runbook

이 디렉터리는 실제 배포 절차와 복구 evidence를 모아두는 곳이다. 구현되지 않은 절차를 완료된 것처럼 기록하지 않는다.

## 현재 상태

| 절차 | 상태 | 실행 파일/근거 |
|---|---|---|
| production demo sanitize | 초안 구현·검증 진행 중 | `scripts/sanitize-production-demo.sh` |
| 배포·업데이트 | 미작성 | `deploy/compose/portfolio.yml` 기준으로 P4에서 작성 |
| 재시작·장애 대응 | 미작성 | health/readiness와 함께 작성 |
| PostgreSQL backup/restore | 부분 구현 | `deploy/backup-postgres.sh`, `deploy/restore-postgres.sh` |
| MinIO backup/restore | 미구현 | media slice에서 추가 |
| secret 교체 | 미작성 | SMTP·MinIO secret 확정 후 작성 |
| rollback | 미작성 | image tag·migration 정책 확정 후 작성 |

## 공통 원칙

- 실제 secret과 credential은 문서에 기록하지 않는다.
- production sanitize는 초기 1회 명시적으로 실행하고, 일반 배포마다 자동 실행하지 않는다.
- destructive command는 dry-run과 명시적 confirmation을 요구한다.
- DB backup만 복원하고 object backup을 누락하지 않는다.
- 실행하지 않은 복구·rollback은 성공했다고 표현하지 않는다.

## 작성 순서

1. 배포 전제와 fresh-volume 준비
2. migration·sanitize·private operator bootstrap
3. backend·MinIO·web health 확인
4. backup과 restore
5. 장애 재시작과 rollback
6. secret 교체와 만료 후 확인
