# DB·media 백업/복구 evidence

## 보장하는 범위

`deploy/backup-portfolio.sh`는 TownPet backend의 write ingress를 maintenance marker로 닫고, 진행 중인 write가 0이 될 때까지 drain한 뒤 PostgreSQL custom dump와 MinIO bucket mirror를 만든다. 백업 manifest에는 다음 대사가 남는다.

- snapshot 시각, 실행 ID, 소요 시간
- Flyway schema version
- source backend/web image reference
- publication/upload asset/media object 개수
- DB object key 목록과 백업 media key 목록의 SHA-256

DB에 참조된 object key가 백업에 없거나 백업에만 존재하는 object가 있으면 백업을 성공으로 표시하지 않는다. 실패·중단 시 maintenance marker는 trap으로 제거된다.

`deploy/restore-portfolio.sh`는 checksum을 먼저 확인한 뒤 disposable PostgreSQL·MinIO에 복원하고, DB의 모든 `upload_asset.object_key`가 복원 bucket에 존재하는지와 orphan object가 없는지를 확인한다. 선택적으로 `RESTORE_HEALTH_URL`, `RESTORE_API_URL`, `RESTORE_MEDIA_URL`을 지정하면 application readiness, 대표 API, signed media GET도 검증한다.

## 실행 경로

```bash
BACKUP_BEFORE_DEPLOY=1 deploy/deploy-netcup.sh
```

배포 전에 백업을 강제하려면 위 flag를 사용한다. 공동 VPS의 다른 Compose project는 건드리지 않고 `townpet-backend`만 maintenance 상태로 전환한다.

복구는 명시적인 destructive 확인이 필요하다.

```bash
ALLOW_DESTRUCTIVE_RESTORE=YES \
BACKUP_ROOT=/absolute/path/to/townpet-YYYYmmddTHHMMSSZ \
POSTGRES_CONTAINER=townpet-postgres \
MINIO_CONTAINER=townpet-minio \
POSTGRES_USER=townpet_admin POSTGRES_DB=townpet \
MINIO_ACCESS_KEY="$TOWNPET_MINIO_ACCESS_KEY" \
MINIO_SECRET_KEY="$TOWNPET_MINIO_SECRET_KEY" \
deploy/restore-portfolio.sh
```

## 검증 상태

- 구현 및 script/unit 검증: 완료
- VPS production backup: 완료. maintenance quiesce와 실제 DB·media mirror가 포함된 paired artifact 생성
- VPS disposable DB·MinIO·application fresh restore 및 signed media GET: 완료
- netcup 서버 손실 복구: provider 전체 재구축이 아닌 disposable fresh-volume rehearsal까지만 검증

| 실행 시각(UTC) | 환경 | 결과 | duration | 원자료 |
|---|---|---|---:|---|
| 2026-09-07 08:08 UTC | netcup VPS disposable project `townpet-p5` | 성공: checksum·DB restore·runtime grant·DB/media key 대사·health·discovery API 통과; media 0개 | 3s | `/opt/townpet/p5-rehearsal/restore-20260907T0810Z.log`, source `/opt/backups/townpet-20260820T024434Z` |
| 2026-09-07 09:10 UTC | netcup VPS production | 성공: maintenance quiesce 후 PostgreSQL dump·MinIO 2개 mirror·manifest checksum·DB/media key 대사 통과 | 1s | `/opt/backups/townpet-20260907T091030Z`, execution `p5-production-20260907-v2` |
| 2026-09-07 09:16 UTC | netcup VPS disposable project `townpet-p5` | 성공: 실제 media 2개 포함 backup의 checksum·DB restore·runtime grant·DB/media key 대사·health·discovery API 통과 | 3s | `/opt/townpet/p5-rehearsal/restore-media-20260907.log`, source `/opt/backups/townpet-20260907T091030Z` |

2026-09-07 production artifact는 `media_objects=2`, 연결된 `db_upload_assets=2`였고, 운영 DB의 연결되지 않은 `UPLOADING` asset 2개는 paired media 대상에서 제외했다. fresh restore 후 대표 object에 대해 presigned URL을 발급하고 HTTP 200 GET을 확인했다. 원자료 로그에는 `event=signed_media_get outcome=success http_status=200`이 남아 있다.

기존 08:08 UTC artifact는 `media_objects=0`, `db_upload_assets=0`이었다. 따라서 해당 실행의 media 검사는 빈 집합 통과이며 signed media GET evidence로 해석하지 않는다.

개수 일치만으로 복구 성공을 주장하지 않는다. 실제 DB 참조 media와 대표 application/media 요청이 모두 통과한 실행만 성공 evidence로 기록한다.
