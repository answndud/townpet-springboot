# DB·media 백업/복구 evidence

## 보장하는 범위

`deploy/backup-portfolio.sh`는 TownPet backend의 write ingress를 maintenance marker로 닫고, 진행 중인 write가 0이 될 때까지 drain한다. 이미 발급된 MinIO presigned upload는 설정된 `TOWNPET_MINIO_PRESIGN_EXPIRY_SECONDS`와 30초 grace를 기다린 뒤, 두 번의 bucket inventory가 같을 때 PostgreSQL custom dump와 MinIO bucket mirror를 만든다. 백업 manifest에는 다음 대사가 남는다.

- snapshot 시각, 실행 ID, 소요 시간
- Flyway schema version
- source backend/web image reference
- publication와 `UPLOADING`·`READY`·`ATTACHED`·`ABANDONED` upload asset 상태별 개수
- DB object key 목록과 백업 media key 목록의 SHA-256

`UPLOADING`·`READY`·`ATTACHED` asset의 object key는 모두 백업 대상이다. `READY`는 아직
publication에 연결되지 않았어도 정상적인 재개·복구 대상이므로 orphan으로 판정하지 않는다.
`ABANDONED` row에 object가 남아 있거나, 보존 대상 DB key가 백업에 없거나, 백업에만
존재하는 object가 있으면 백업을 성공으로 표시하지 않는다. 실패·중단 시 maintenance
marker는 trap으로 제거된다.

`deploy/restore-portfolio.sh`는 checksum을 먼저 확인한 뒤 disposable PostgreSQL·MinIO에
복원하고, 세 가지 non-terminal 상태의 모든 `upload_asset.object_key`가 복원 bucket에
존재하는지, `ABANDONED` object가 복원되지 않았는지, orphan object가 없는지를 확인한다.
선택적으로 `RESTORE_HEALTH_URL`, `RESTORE_API_URL`, `RESTORE_MEDIA_URL`을 지정하면
application readiness, 대표 API, signed media GET도 검증한다.

P2 정책 fixture는 다음 네 경계를 확인한다.

- `READY`·`ATTACHED`·`UPLOADING` object가 모두 있으면 성공
- DB key 누락, DB에 없는 object, `ABANDONED` object 잔존은 실패
- `scripts/backup-reference-policy-test.sh`는 실제 volume을 건드리지 않는 disposable fake PostgreSQL/MinIO 경계 테스트다.

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
MINIO_RESTORE_ACCESS_KEY="$TOWNPET_MINIO_ROOT_ACCESS_KEY" \
MINIO_RESTORE_SECRET_KEY="$TOWNPET_MINIO_ROOT_SECRET_KEY" \
deploy/restore-portfolio.sh
```

## 검증 상태

- 구현 및 script/unit 검증: 완료. P2 정책 fixture 4개 경계 통과
- VPS production backup: stale `UPLOADING` 2건을 공식 cleanup 조건으로 정리한 뒤 현재 정책 backup 성공
- VPS disposable DB·MinIO fresh restore: restore-capable MinIO credential을 명시해 checksum·DB/media key 대사·status count·media mirror 복구 성공. application/signed media GET은 backend를 포함한 별도 rehearsal에서 추가 확인 범위
- netcup 서버 손실 복구: provider 전체 재구축이 아닌 disposable fresh-volume rehearsal까지만 검증

| 실행 시각(UTC) | 환경 | 결과 | duration | 원자료 |
|---|---|---|---:|---|
| 2026-09-07 08:08 UTC | netcup VPS disposable project `townpet-p5` | 성공: checksum·DB restore·runtime grant·DB/media key 대사·health·discovery API 통과; media 0개 | 3s | `/opt/townpet/p5-rehearsal/restore-20260907T0810Z.log`, source `/opt/backups/townpet-20260820T024434Z` |
| 2026-09-07 09:10 UTC | netcup VPS production | 성공: maintenance quiesce 후 PostgreSQL dump·MinIO 2개 mirror·manifest checksum·DB/media key 대사 통과 | 1s | `/opt/backups/townpet-20260907T091030Z`, execution `p5-production-20260907-v2` |
| 2026-09-07 09:16 UTC | netcup VPS disposable project `townpet-p5` | 성공: 실제 media 2개 포함 backup의 checksum·DB restore·runtime grant·DB/media key 대사·health·discovery API 통과 | 3s | `/opt/townpet/p5-rehearsal/restore-media-20260907.log`, source `/opt/backups/townpet-20260907T091030Z` |
| 2026-09-07 12:24 UTC | netcup VPS production | 실패(의도된 차단): 현재 non-terminal 정책으로 만료 `UPLOADING` 2건의 DB key가 backup media에 없어 중단, maintenance marker cleanup 확인 | 약 40s | execution `p2-policy-20260907`, phase `reference_verify` |
| 2026-09-07 12:26 UTC | netcup VPS disposable project `p2-restore-20260907` | 실패(의도된 차단): historical backup restore 후 동일한 누락 `UPLOADING` 참조를 감지, 새 volume/network는 종료·삭제 | 약 10s | execution `p2-disposable-restore-20260907`, phase `reference_verify` |
| 2026-09-07 13:16 UTC | netcup VPS production | 성공: dry-run 2건·67,282 bytes 확인 후 expired non-attached `UPLOADING` cleanup, 현재 DB/media 정합성 backup 성공 | 4s | `/opt/backups/townpet-20260907T131623Z`, execution `p2-policy-clean-20260907`; 사전 dump `/opt/backups/upload-asset-before-p2-cleanup-20260907T131601Z.dump` |
| 2026-09-07 13:17 UTC | netcup VPS disposable project `p2-restore-clean-root-20260907` | 성공: fresh volume에 checksum·DB restore·runtime grant·DB/media key 대사·media restore 통과; restore-capable credential 명시 | 3s | execution `p2-disposable-restore-clean-root-20260907`, source `/opt/backups/townpet-20260907T131623Z` |

2026-09-07 09:10 production artifact와 그 fresh restore는 P2 정책 적용 전,
`publication_id IS NOT NULL` 기준의 historical artifact다. 따라서 연결되지 않은 `READY`·
`UPLOADING` asset을 보존하는 현재 정책의 성공 evidence로 사용하지 않는다. 12:24 이후의
실행은 현재 정책이 운영 데이터 불일치를 정확히 차단했다는 실패 evidence다. 13:16 이후
실행은 기존 media lifecycle cleanup 조건(만료·비연결 `UPLOADING`)으로 stale row를 정리한
뒤 생성한 현재 정책의 성공 paired backup이다. cleanup 전 row 전체는 별도 data-only dump로
보관했으며, restore script는 application key가 아닌 restore-capable MinIO credential을
명시적으로 요구한다.

기존 08:08 UTC artifact는 `media_objects=0`, `db_upload_assets=0`이었다. 따라서 해당 실행의 media 검사는 빈 집합 통과이며 signed media GET evidence로 해석하지 않는다.

개수 일치만으로 복구 성공을 주장하지 않는다. 실제 DB 참조 media와 대표 application/media 요청이 모두 통과한 실행만 성공 evidence로 기록한다.
