# 저장소 내부 보안 리뷰

작성일: 2026-08-13

## 판정

현재 저장소에서 재현 가능한 P0/P1 보안 결함은 확인되지 않았다. Session JDBC·CSRF·method security·resource ownership·private media·account token 상한이 핵심 경계를 담당한다. 이번 리뷰에서는 공개 telemetry ingress와 운영 credential 경계를 추가로 하드닝했다.

## 범위

- `SecurityConfig`의 public permit와 staff authorization
- member/resource ownership을 검사하는 media·publication·engagement 경계
- password reset/email verification token 발급과 generic response
- presigned media upload/finalize/read/delete
- 공개 acquisition·web-vitals·CSP report endpoint의 abuse surface
- production Compose와 backend image의 credential·process privilege

## 변경

### 1. 익명 telemetry ingress 상한

상황: acquisition, web-vitals, CSP report는 인증 없이 호출되므로 payload validation만으로는 DB write 폭주를 막을 수 없었다.

선택: Redis를 추가하지 않고 단일 portfolio application 인스턴스에서 공유하는 in-memory window limiter를 적용했다. 세 endpoint가 분당 600건 한도를 공유하며 초과 시 `429`를 반환한다. 분산 배포가 필요해지면 edge/distributed limiter를 별도 ADR로 결정한다.

근거: `src/main/java/com/townpet/operations/PublicIngressRateLimiter.java`, 세 public controller와 `PublicIngressRateLimiterTest`.

검증: 600회 허용 후 601회 `ResponseStatusException(429)` 회귀 테스트 통과.

한계: 프로세스 재시작·다중 인스턴스 간 카운터는 공유되지 않는다. 현재 VPS 외부 운영 검증과 Redis 도입은 범위 밖이다.

### 2. MinIO root/application credential 분리

상황: 기존 portfolio Compose는 backend가 MinIO root credential을 그대로 사용했다.

선택: `minio-init` one-shot service가 root credential로 bucket과 제한된 `townpet-media-app` policy를 만들고, backend는 object read/write/delete에 필요한 application credential만 사용하도록 분리했다.

근거: `deploy/compose/portfolio.yml`, `deploy/compose/minio-policy.json`, `deploy/portfolio.env.example`, `scripts/validate-portfolio-env.sh`.

검증: dummy environment를 주입한 `docker compose ... config` 성공과 required root/application variable 검증.

한계: 실제 DNS/TLS/CORS와 외부 VPS에서의 secret rotation은 [`docs/runbooks/external-production-checklist.md`](../runbooks/external-production-checklist.md)에 남아 있다.

### 3. Backend container non-root

상황: runtime image가 기본 root 사용자로 실행되고 있었다.

선택: image build 단계에서 UID/GID 10001 `townpet` 사용자를 만들고 `/app`을 소유하게 한 뒤 runtime process를 해당 사용자로 실행한다.

근거: `deploy/Dockerfile.backend`.

검증: `docker build --file deploy/Dockerfile.backend ...` 성공, `docker run --rm --entrypoint id` 결과 `uid=10001(townpet) gid=10001(townpet)` 확인.

### 4. 로그인·guest abuse 방어

상황: telemetry뿐 아니라 로그인과 guest step-up도 인증 전 endpoint다. 공격자가 비밀번호 후보를 반복하거나 guest author를 대량 생성하면 정상 사용자가 영향을 받는다.

선택: `RequestRateLimiter`를 공통 bounded fixed-window 컴포넌트로 만들고 remote address와 bucket을 조합해 제한한다. 현재 단일 인스턴스 기준 로그인·guest step-up은 분당 30회, guest author 생성은 분당 30회다. map은 최대 10,000 key로 제한해 공격자가 서로 다른 주소를 보내 메모리를 무한히 늘리지 못하게 했다.

근거: `src/main/java/com/townpet/common/RequestRateLimiter.java`, `SessionController`, `GuestStepUpController`, `RequestRateLimiterTest`.

검증: bucket과 IP가 서로 독립적으로 동작하고 동일 bucket·IP가 한도를 넘으면 `429`가 되는 단위 테스트와 전체 backend gate.

한계: Caddy가 `X-Forwarded-For`를 덮어쓰는 현재 단일 proxy 구성을 전제로 한다. 다중 proxy/다중 backend instance에서는 trusted proxy 목록과 공유 저장소 기반 limiter를 별도로 결정해야 한다.

### 5. Browser security headers와 공급망 gate

`deploy/Caddyfile`에 HSTS, CSP, frame-ancestors, nosniff, Referrer-Policy, Permissions-Policy와 media CORS 정책을 명시했다. `scripts/security-static-check.sh`는 production cookie, non-root image, Caddy header, MinIO least-privilege policy와 auth limiter의 존재를 검사한다.

`.github/workflows/security.yml`에는 Trivy filesystem·secret scan, PR dependency review, frontend SBOM 생성, backend/frontend container image scan을 추가했다. Trivy local source scan은 현재 frontend lockfile에서 HIGH/CRITICAL unfixed vulnerability 없이 종료됐다. 이는 dependency가 영원히 안전하다는 증명이 아니라, scan 당시 database와 lockfile 기준의 결과다.

로컬 Caddy 2.9 parser validation, static checks, shell syntax, backend 전체 gate, frontend 35개 unit/build, browser 54개 desktop/mobile E2E가 통과했다.

## 이미 통과한 경계

- Spring Session JDBC와 CSRF token repository
- `@EnableMethodSecurity`, `@MemberOnly`, moderator deny-by-default
- owner-only media signed read URL과 finalize checksum/magic-byte validation
- password reset/email verification generic `202` response와 member별 시간당 발급 상한
- exact location/private media를 public response와 log에 노출하지 않는 정책
- optimistic version·DB constraint·atomic view count
- JaCoCo release gate: line coverage 60%, branch coverage 40% 하한을 `check`에 연결

## 남은 보안 작업

- 실제 공개 전 Caddy/VPS edge rate limit과 forwarded-header 신뢰 범위 확정
- MinIO application policy의 실제 browser upload/read/delete 검증
- SMTP·TLS·secret rotation 실환경 검증
- public signup을 열 경우 IP/계정/장치 단위 abuse 방어와 MFA/관리자 재인증 ADR 추가

현재 sandbox를 “보안 검증 완료”라고 표현할 때는 위의 저장소 내부 evidence만 의미하며, 외부 인프라 검증까지 완료했다는 뜻으로 사용하지 않는다.
