# TownPet 운영 Runbook

이 디렉터리는 실제 배포 절차와 복구 evidence를 모아두는 곳이다. 구현되지 않은 절차를 완료된 것처럼 기록하지 않는다.

## 현재 상태

| 절차 | 상태 | 실행 파일/근거 |
|---|---|---|
| production demo sanitize | local dry-run·script 검증 완료, production 실행 전 | `scripts/sanitize-production-demo.sh` |
| private moderator bootstrap | 초안 구현 | `scripts/bootstrap-private-moderator.sh` |
| 배포·업데이트 | 초안 | [`deploy-update.md`](deploy-update.md) |
| 재시작·장애 대응 | 초안 | [`incident-restart.md`](incident-restart.md) |
| PostgreSQL·MinIO paired backup/restore | 구현·local rehearsal 완료, VPS 검증 전 | `deploy/backup-portfolio.sh`, `deploy/restore-portfolio.sh` |
| secret 교체 | 초안 | [`secret-rotation.md`](secret-rotation.md) |
| rollback | 초안 | [`rollback.md`](rollback.md) |
| SMTP local 검증 | 실행 가능 | [`email-local.md`](email-local.md) |
| SMTP·media·VPS 외부 전제 | 배포 전 operator 확인 필요 | [`external-production-checklist.md`](external-production-checklist.md) |
| production env policy | 실행 가능 | `scripts/validate-portfolio-env.sh` |
| 최소 observability | 초안 | [`observability.md`](observability.md) |

## 공통 원칙

- 실제 secret과 credential은 문서에 기록하지 않는다.
- production sanitize는 초기 1회 명시적으로 실행하고, 일반 배포마다 자동 실행하지 않는다.
- destructive command는 dry-run과 명시적 confirmation을 요구한다.
- DB backup만 복원하고 object backup을 누락하지 않는다.
- MinIO presigned URL은 backend 내부 endpoint가 아니라 `TOWNPET_MINIO_PUBLIC_ENDPOINT`로 서명한다.
- 실행하지 않은 복구·rollback은 성공했다고 표현하지 않는다.
- backup은 `manifest.txt`와 `manifest.sha256`가 생성되고 PostgreSQL dump가 비어 있지 않은 경우에만 성공으로 기록한다. dump와 manifest 디렉터리는 `umask 077` 권한으로 보관한다.

## 작성 순서

1. 배포 전제와 fresh-volume 준비
2. migration·sanitize·private operator bootstrap
3. backend·MinIO·web health 확인
4. backup과 restore
5. 장애 재시작과 rollback
6. secret 교체와 만료 후 확인

각 문서의 “초안”은 명령과 전제는 정리됐지만 실제 VPS 또는 fresh-volume에서 성공 evidence를 아직 만들지 않았다는 뜻이다.
