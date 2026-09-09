# 공개 데모 안내

TownPet은 실제 개인정보를 수집하지 않는 합성 portfolio sandbox입니다.

## 접속

- 서비스: [townpet.cloud](https://townpet.cloud/)
- 로그인 진입점: `/login`
- 저장소 내부 실행은 `./scripts/seed-local-demo.sh`와 `deploy/compose/local.yml` 기준으로 진행합니다.

공개 문서에는 운영 secret이나 private operator 자격을 기록하지 않습니다. 저장소 내부의 로컬 fixture 계정은 의도적으로 합성된 개발 전용 자격이며 실제 서비스에서 재사용하면 안 됩니다.

## 먼저 확인할 흐름

1. 공개 feed에서 검색·cursor pagination·게시글 상세를 확인합니다.
2. 합성 회원으로 로그인해 게시글 작성·수정·삭제, 댓글·반응·북마크를 확인합니다.
3. 다른 합성 회원 관점에서 작성자 소유권과 권한 거부 경계를 확인합니다.
4. 모임 상세에서 참여·정원 초과 오류와 참여자 수 상태를 확인합니다.
5. 운영 계정이 필요한 moderation 화면은 공개 데모에서 무단 접근하지 않고, 저장소의 테스트와 sanitized evidence로 확인합니다.

## 평가자 빠른 체험

공개 주소에서는 먼저 로그인 없이 feed·검색·cursor 이동·상세를 확인할 수 있습니다. 로그인 기능과 작성자 소유권까지 확인하려면 지원 과정에서 별도로 전달된 합성 demo 계정을 사용합니다. 계정 비밀번호는 공개 저장소나 이 문서에 기록하지 않으며, 전달받은 계정이 없으면 `portfolio/evidence/`의 테스트·검증 근거로 로그인 이후 흐름을 확인합니다.

실제 개인정보를 입력하거나 공개 서비스에 새 계정을 만들지 않습니다. 게시글·댓글·모임 생성은 합성 데이터에만 수행하고, 확인이 끝나면 작성한 게시글을 삭제합니다.

## 데이터와 destructive action

- 공개 콘텐츠와 계정은 synthetic fixture입니다.
- 게시글 삭제, 댓글 삭제, 모임 취소는 demo 데이터에 상태 변화를 남길 수 있습니다.
- 실제 사용자 가입·개인정보 입력·결제는 제공하지 않습니다.
- 실제 SMTP 전달성, 운영 SLA, provider 전체 서버 복구를 공개 데모의 기능으로 주장하지 않습니다.

## 구현·검증 근거

- [아키텍처](architecture.md)
- [피드 성능 evidence](evidence/feed-performance.md)
- [모임 정원 경합 evidence](evidence/gathering-concurrency.md)
- [알림 전달·재처리 evidence](evidence/notification-delivery.md)
- [백업·복구 evidence](evidence/backup-restore.md)
- [CI와 현재 검증 범위](README.md#검증-범위)
