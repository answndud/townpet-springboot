# DB·media 백업/복구 evidence

## 보장하는 범위

`deploy/backup-portfolio.sh`는 TownPet backend의 write ingress를 maintenance marker로 닫고, 진행 중인 write가 0이 될 때까지 drain한다. `UPLOADING`·`READY`·`ATTACHED` row가 있으면 발급됐을 수 있는 presigned form의 최대 잔여 유효시간과 설정된 `TOWNPET_MINIO_PRESIGN_EXPIRY_SECONDS` 중 안전한 대기시간을 계산하고 30초 grace를 더 기다린다. 이후 두 번의 bucket inventory가 같을 때 PostgreSQL custom dump와 MinIO bucket mirror를 만든다. 백업 manifest에는 다음 대사가 남는다.

- snapshot 시각, 실행 ID, 소요 시간
- Flyway schema version
- source backend/web image reference
- publication와 `UPLOADING`·`READY`·`ATTACHED`·`ABANDONED` upload asset 상태별 개수
- 필수 media asset 수, 선택적 `UPLOADING` 수, object가 없는 `UPLOADING` 수
- DB object key 목록과 백업 media key 목록의 SHA-256

`READY`·`ATTACHED` asset의 object key는 필수 백업 대상이다. `UPLOADING`은 DB row가
먼저 만들어진 뒤 client upload가 시작되는 흐름이므로 object가 아직 없어도 선택적으로
보존할 수 있다. `UPLOADING` object가 실제로 존재하면 함께 백업하며, `READY`는 아직
publication에 연결되지 않았어도 정상적인 재개·복구 대상이므로 orphan으로 판정하지 않는다.
`ABANDONED` row에 object가 남아 있거나, 필수 DB key가 백업에 없거나, 백업에만 존재하는
object가 있으면 백업을 성공으로 표시하지 않는다. 실패·중단 시 maintenance marker는
trap으로 제거된다.

`deploy/restore-portfolio.sh`는 checksum을 먼저 확인한 뒤 disposable PostgreSQL·MinIO에
복원하고, `READY`·`ATTACHED` 상태의 모든 `upload_asset.object_key`가 복원 bucket에
존재하는지, 존재하는 `UPLOADING` object도 대응하는지, `ABANDONED` object가 복원되지
않았는지, orphan object가 없는지를 확인한다. object 없는 `UPLOADING` row는 만료 후
media cleanup command로 제거할 수 있는 의도된 incomplete 상태다.
선택적으로 `RESTORE_HEALTH_URL`, `RESTORE_API_URL`, `RESTORE_MEDIA_URL`을 지정하면
application readiness, 대표 API, signed media GET도 검증한다.

P1 정책 fixture는 다음 아홉 경계를 확인한다.

- `READY`·`ATTACHED`·`UPLOADING` object가 모두 있으면 성공
- DB key 누락, DB에 없는 object, `ABANDONED` object 잔존은 실패
- backup 중 bucket inventory가 바뀌면 실패
- snapshot 직전 안정성 확인을 통과한 뒤 media copy 중 inventory가 바뀌어도 실패
- object 없는 `UPLOADING`은 성공하고 `READY` object 누락은 실패
- `UPLOADING` row가 없어도 active `READY` row가 있으면 presigned form 대기 정책을 적용
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

- 구현 및 script/unit 검증: P1 정책 fixture 9개 경계 통과
- P1 direct-upload/backup race fixture: 완료. `bash scripts/backup-reference-policy-test.sh`가
  `ready-is-preserved`, `missing-object`, `orphan-object`, `abandoned-object`,
  `inventory-changed`, `inventory-changed-during-snapshot`,
  `uploading-object-missing-is-allowed`, `ready-object-missing-is-rejected`,
  `presigned-form-wait-applies-without-uploading-row` 9개 case를 통과한다.
- VPS production backup: stale `UPLOADING` 2건을 공식 cleanup 조건으로 정리한 뒤 현재 정책 backup 성공
- VPS disposable DB·MinIO fresh restore: restore-capable MinIO credential을 명시해 checksum·DB/media key 대사·status count·media mirror 복구 성공. application/signed media GET은 backend를 포함한 별도 rehearsal에서 추가 확인 범위
- netcup 서버 손실 복구: provider 전체 재구축이 아닌 disposable fresh-volume rehearsal까지만 검증

직접 presigned upload가 진행 중인 production volume에 backup을 실행하지 않았다. 해당 race는
active upload asset 상태와 무관하게 expiry+grace 이후 snapshot을 시작하는 정책 및 fake
boundary test로 안전하게 검증한다. object 없는 `UPLOADING`은 복구 후 cleanup 가능한
상태로 허용하지만, `READY`·`ATTACHED` object 누락은 계속 backup을 차단한다.
`inventory-changed-during-snapshot`은 첫 두 inventory가 같아 안정성 확인을 통과한 뒤 세 번째
inventory에서 늦게 도착한 object를 발견하는 경우를 재현한다. 실제 운영 upload를 의도적으로
중단시키는 실험은 하지 않는다.

| 실행 시각(UTC) | 환경 | 결과 | duration | 원자료 |
|---|---|---|---:|---|
| 2026-09-07 08:08 UTC | netcup VPS disposable project | 성공: checksum·DB restore·runtime grant·DB/media key 대사·health·discovery API 통과; media 0개 | 3s | VPS restore log와 backup source는 공개하지 않는 운영 artifact |
| 2026-09-07 09:10 UTC | netcup VPS production | 성공: maintenance quiesce 후 PostgreSQL dump·MinIO 2개 mirror·manifest checksum·DB/media key 대사 통과 | 1s | VPS backup manifest와 execution log는 공개하지 않는 운영 artifact |
| 2026-09-07 09:16 UTC | netcup VPS disposable project | 성공: 실제 media 2개 포함 backup의 checksum·DB restore·runtime grant·DB/media key 대사·health·discovery API 통과 | 3s | VPS restore log와 backup source는 공개하지 않는 운영 artifact |
| 2026-09-07 12:24 UTC | netcup VPS production | 실패(의도된 차단): 현재 non-terminal 정책으로 만료 `UPLOADING` 2건의 DB key가 backup media에 없어 중단, maintenance marker cleanup 확인 | 약 40s | 운영 execution log는 공개하지 않는 artifact |
| 2026-09-07 12:26 UTC | netcup VPS disposable project | 실패(의도된 차단): historical backup restore 후 동일한 누락 `UPLOADING` 참조를 감지, 새 volume/network는 종료·삭제 | 약 10s | 운영 execution log는 공개하지 않는 artifact |
| 2026-09-07 13:16 UTC | netcup VPS production | 성공: dry-run 2건·67,282 bytes 확인 후 expired non-attached `UPLOADING` cleanup, 현재 DB/media 정합성 backup 성공 | 4s | VPS backup manifest와 사전 dump는 공개하지 않는 운영 artifact |
| 2026-09-07 13:17 UTC | netcup VPS disposable project | 성공: fresh volume에 checksum·DB restore·runtime grant·DB/media key 대사·media restore 통과; restore-capable credential 명시 | 3s | 운영 execution log와 backup source는 공개하지 않는 artifact |

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
