# PLAN.md

## Goal

공개 쓰기 abuse와 취약점 스캔 공백을 먼저 닫고, 업로드·rate limit·moderator MFA·계정 전달·복구의 남은 Medium 보안 경계를 실제 운영 증거까지 완성한다. 비회원 콘텐츠 생성은 일일 quota가 아니라 IP·guest별 **시간당** 제한을 사용한다.

## Active

### P1 - 공개 쓰기 abuse와 보안 스캔 차단선 복구

1. 비회원 게시글·댓글 생성에 시간당 제한을 추가한다.
   - 파일: `src/main/java/com/townpet/publication/GuestPublicationController.java`, `src/main/java/com/townpet/engagement/GuestCommentController.java`, `src/main/java/com/townpet/common/RequestRateLimiter.java`, 관련 controller/service test
   - 변경: IP별 시간당 요청 상한과 guest ID별 시간당 게시글·댓글 생성 상한을 server에서 적용한다. 수정·삭제에도 요청 상한을 적용하되, 일일 생성량 quota는 만들지 않는다.
   - 검증: 동일 IP·guest의 제한 초과 요청이 `429`가 되고, 다른 guest는 독립적인지 controller 통합 테스트로 확인한다.
   - 완료: 비회원 콘텐츠 생성이 정의된 시간당 상한을 넘지 않으며 정상 생성·ownership 흐름이 유지된다.

2. 공개 검색 로그 ingress를 제한·보존한다.
   - 파일: `src/main/java/com/townpet/discovery/SearchEventController.java`, `src/main/java/com/townpet/operations/PublicIngressRateLimiter.java`, `src/main/resources/db/migration/`, 관련 test
   - 변경: `/api/search/log`에 IP별 rate limit을 적용하고, `search_event`가 무한히 누적되지 않도록 보존 기간 기반 cleanup을 추가한다.
   - 검증: 초과 요청의 `429`, replay idempotency, 만료 event cleanup을 Testcontainers PostgreSQL 테스트로 확인한다.
   - 완료: 인증되지 않은 요청이 search event write volume과 보존량을 정의된 상한 이상으로 늘릴 수 없다.

3. GitHub 보안 스캔 workflow를 실행 가능한 상태로 복구한다.
   - 파일: `.github/workflows/security.yml`, 필요 시 `.github/dependabot.yml`
   - 변경: 존재하는 Trivy action release를 검증된 commit SHA로 고정하고 filesystem secret/vulnerability 및 backend·frontend image scan을 유지한다.
   - 검증: GitHub Actions 수동 실행에서 action resolution, Trivy scan 결과, CRITICAL/HIGH 발견 시 실패를 확인한다.
   - 완료: dependency·container·secret scan이 실행 시작 단계에서 실패하지 않고 결과를 남긴다.

### P2 - 업로드·구성·분산 rate limit 경계 강화

1. presigned upload finalize에서 이미지 해상도·총 픽셀 제한을 검사한다.
   - 파일: `src/main/java/com/townpet/media/MediaService.java`, `src/main/java/com/townpet/media/MinioObjectStorage.java`, `src/test/java/com/townpet/media/MediaControllerTest.java`
   - 변경: direct upload object의 image header를 검사해 MIME, byte size, width, height, total pixels를 모두 제한하고 실패 object를 정리한다.
   - 검증: 10 MiB 이하이지만 해상도 또는 총 픽셀 한도를 넘는 image의 finalize가 거부되고 object가 남지 않는지 MinIO/Testcontainers 테스트로 확인한다.
   - 완료: local upload와 presigned upload가 같은 이미지 안전 한계를 적용한다.

2. MinIO 자격증명의 기본값을 제거한다.
   - 파일: `src/main/resources/application.yml`, `src/main/resources/application-local.yml` 또는 local compose/env example, startup validation test
   - 변경: `minioadmin` fallback을 제거하고 local·production 모두 명시적인 환경변수 또는 안전한 local-only fixture로 주입한다.
   - 검증: 자격증명 없이 시작이 실패하고 example env로 local stack이 정상 기동하는지 확인한다.
   - 완료: 실행 가능한 profile에 알려진 기본 object-storage 자격증명이 남지 않는다.

3. 보안상 중요한 rate limit을 다중 인스턴스에서도 일관되게 만든다.
   - 파일: `src/main/java/com/townpet/common/RequestRateLimiter.java`, `src/main/resources/db/migration/`, 관련 test, `deploy/compose/`
   - 변경: 현재 단일 JVM map을 PostgreSQL 기반의 원자적 window counter 또는 동등한 shared boundary로 교체하고, 만료 counter 정리와 rejection metric을 유지한다.
   - 검증: 두 application instance가 같은 PostgreSQL을 사용할 때 IP·guest별 합산 요청이 상한을 넘지 않는지 통합 테스트로 확인한다.
   - 완료: restart·수평 확장으로 auth, guest, telemetry rate limit을 우회할 수 없다.

### P3 - 관리자 계정과 운영 복구 증거 완성

1. moderator MFA를 TOTP로 구현·강제한다.
   - 파일: `ADR.md`, `src/main/resources/db/migration/`, `src/main/java/com/townpet/identity/`, `frontend/src/`, 관련 test, `docs/10-보안/`
   - 변경: TOTP enrollment·확인·recovery code·MFA 완료 세션 상태를 추가하고 moderator의 operations/admin API와 로그인 완료에 MFA를 요구한다.
   - 검증: MFA 미완료 moderator가 관리자 API에 접근할 수 없고, enrollment·recovery·세션 폐기 흐름이 정상 동작하는지 backend/frontend test로 확인한다.
   - 완료: 비밀번호만으로 moderator 권한 세션 또는 관리자 작업이 허용되지 않는다.

2. 실제 계정 메일 전달과 DNS 인증을 검증한다.
   - 파일: `docs/09-운영-가이드/외부-운영-체크리스트.md`, `docs/09-운영-가이드/운영-검증-기록-2026-08-20.md`, 필요 시 `deploy/netcup.env.example`
   - 변경: Resend sender domain, SPF, DKIM, DMARC, bounce/complaint 경로를 확인하고 verification·password reset을 실제 수신함으로 보낸다. 실패 재시도와 실패 로그의 token/credential 비노출도 확인한다.
   - 검증: VPS SMTP STARTTLS 제출, 수신함 도착, 링크 host/scheme, 실패 delivery event 상태를 기록한다.
   - 완료: 운영 계정 recovery·verification 메일의 실제 전달과 실패 관측이 재현된다.

3. paired backup 복구와 실패 알림을 운영화한다.
   - 파일: `deploy/backup-portfolio.sh`, `deploy/restore-portfolio.sh`, `deploy/encrypt-backup.sh`, `docs/09-운영-가이드/`, `docs/10-보안/`
   - 변경: 최신 PostgreSQL·MinIO paired backup을 외부 failure domain에 보관하고, disposable restore·보존 정책·backup 실패 알림·RPO/RTO 기록을 추가한다.
   - 검증: checksum과 age decrypt 뒤 격리 환경에 DB와 object를 함께 복원하고, row/object 대사·RPO/RTO·실패 알림 수신을 기록한다.
   - 완료: 최신 backup의 복구 가능성과 실패 감지가 실제 실행 증거로 남는다.

## Backlog

- `*.env` 및 `deploy/*.env` ignore 강화과 Actions SHA pinning·Gradle dependency locking
- 공개 저장소에 포함될 수 있는 개인 연락처 문서 제거 또는 placeholder 전환
- VPS workload 측정 후 DB connection·JVM memory·storage 경보 임계값 확정
