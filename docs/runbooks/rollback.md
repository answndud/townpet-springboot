# Rollback

Rollback은 application image만 되돌리는 경우와 schema/data를 되돌리는 경우를 구분한다.

## Application-only rollback

1. 현재 image tag와 migration version을 기록한다.
2. 새 image가 health 또는 smoke를 통과하지 못하면 web을 유지한 채 backend image를 이전 tag로 교체한다.
3. readiness와 핵심 read/write smoke를 확인한다.

## Data rollback

1. 현재 DB와 MinIO를 paired backup한다.
2. `deploy/restore-portfolio.sh`의 backup manifest checksum을 먼저 확인한다.
3. 영향 범위와 destructive confirmation을 기록한다.
4. backend와 web을 중지해 restore 중 쓰기가 발생하지 않게 한다. PostgreSQL·MinIO는 restore 명령이 요구하는 상태를 유지한다.
5. 새 volume 또는 격리된 restore 대상에서 먼저 복원한다.
6. DB와 object가 같은 backup id인지 확인한 뒤에만 서비스 volume을 교체한다.
7. backend를 먼저 시작해 Flyway·health를 확인하고, 그 뒤 web을 시작해 핵심 smoke를 실행한다.

`ALLOW_DESTRUCTIVE_RESTORE=YES` 없이는 복원하지 않는다. 적용된 Flyway migration을 임의로 삭제하거나 수정하지 않는다.
