# TownPet 보안 가이드

작성일: 2026-08-13  
대상: Java·Spring Boot와 보안을 처음 배우는 개발자

이 문서는 보안 기능 목록이 아니라, TownPet이 어떤 공격을 어떤 순서로 막는지 설명하는 학습용 문서다. 각 항목은 무엇을 보호하는지, 공격자가 어떻게 시도하는지, 코드가 어디서 막는지, 어떻게 다시 확인하는지, 무엇을 아직 보장하지 못하는지 순서로 읽는다.

## 1. 먼저 이해할 보안 모델

웹 요청은 다음 경계를 통과한다.

```text
Browser
  ↓ HTTPS / Caddy security headers / CORS
Caddy reverse proxy
  ↓ request routing / static policy
Spring Security filter chain
  ↓ session / CSRF / authentication
Controller validation
  ↓ DTO shape / size / enum / format
Application service
  ↓ ownership / role / state transition
PostgreSQL + private MinIO
  ↓ foreign key / unique / checksum / object policy
```

앞 단계가 통과해도 뒤 단계는 다시 검사한다. 화면에서 “내 게시글”이라고 표시했다고 해서 서버가 믿지 않는다. 서버는 세션의 member id와 게시글 작성자 id를 직접 비교한다.

보안의 목표는 공격이 절대 발생하지 않게 하는 것이 아니다.

- 공격자가 권한 없는 데이터를 읽지 못하게 한다.
- 입력을 조작해도 데이터 불변식이 깨지지 않게 한다.
- 공격을 반복하기 어렵게 하고 실패를 감지한다.
- 문제가 생겨도 영향 범위와 복구 경로를 제한한다.

## 2. 인증과 세션

### 2.1 로그인 방식

TownPet은 브라우저 JWT를 만들지 않고 Spring Security와 Spring Session JDBC를 사용한다.

- 로그인 성공 시 서버가 세션을 만든다.
- 브라우저에는 세션 식별 cookie만 저장한다.
- 실제 세션 내용은 PostgreSQL의 `spring_session` 테이블에 저장된다.
- 서버가 여러 대로 늘어도 세션 저장소를 공유할 수 있다.

JWT를 사용하지 않은 이유는 로그아웃·전체 세션 폐기·비밀번호 재설정 후 세션 무효화를 서버에서 명확하게 처리하기 위해서다.

### 2.2 Session fixation 방어

로그인 전에 만들어진 세션 id를 로그인 후에도 계속 사용하면 공격자가 미리 알아낸 세션을 피해자 로그인 세션으로 재사용할 수 있다. 이를 session fixation이라고 한다.

`SessionController`는 인증 성공 뒤 `changeSessionId()`를 호출한다. 로그인 전후 세션 식별자가 바뀐다.

```bash
./gradlew test --tests '*IdentityMemberControllerTest.loginPersistsSessionAndReturnsCurrentMember'
```

### 2.3 production cookie

`application-production.yml`에서는 다음을 사용한다.

- `secure: true`: HTTPS가 아니면 cookie를 전송하지 않는다.
- `http-only: true`: JavaScript가 세션 cookie를 읽지 못한다.
- `same-site: lax`: 외부 사이트에서 임의의 상태 변경 요청을 보내기 어렵게 한다.

단, `secure` cookie는 실제 HTTPS 환경에서만 동작한다. 로컬 HTTP에서 production profile을 사용하면 로그인 자체가 실패할 수 있다.

### 2.4 비밀번호

비밀번호는 평문으로 저장하지 않고 BCrypt hash로 저장한다. 비밀번호 재설정 token도 평문을 DB에 저장하지 않고 hash만 저장한다.

비밀번호 reset 후에는 사용자의 모든 JDBC session을 폐기한다. 공격자가 예전 세션을 가지고 있어도 새 비밀번호 변경 후 계속 사용할 수 없게 하기 위해서다.

## 3. CSRF

### 3.1 CSRF가 무엇인가

브라우저는 cookie를 자동으로 전송한다. 공격자가 만든 다른 사이트에서 피해자의 브라우저로 TownPet에 요청을 보내면, 사용자가 의도하지 않았는데도 cookie가 붙을 수 있다.

예를 들어 악성 사이트가 다음과 같은 요청을 만들 수 있다.

```html
<form action="https://townpet.example/api/v1/publications" method="post">
  <input name="title" value="악성 글" />
</form>
```

Spring Security CSRF filter는 서버가 발급한 CSRF token이 요청에 함께 있는지 확인한다. session cookie가 있다는 이유만으로 write를 허용하지 않는다.

### 3.2 TownPet의 흐름

1. 프론트엔드가 `GET /api/v1/auth/csrf`를 호출한다.
2. 서버가 `XSRF-TOKEN` cookie와 응답 token을 준다.
3. frontend API client가 상태 변경 요청에 token header를 붙인다.
4. Spring Security가 token을 검증한다.

```bash
./gradlew test --tests '*IdentityMemberControllerTest.stateChangingRequestWithoutCsrfIsRejected'
```

GET 공개 조회에는 CSRF token이 필요하지 않지만 게시글 작성·댓글·반응·로그아웃·관리 작업에는 필요하다.

## 4. 권한: 역할과 소유권은 다르다

### 4.1 역할 검사

현재 sandbox의 핵심 역할은 `MEMBER`와 제한된 `MODERATOR`다.

- 일반 회원은 자기 콘텐츠와 일반 회원 기능만 사용한다.
- Moderator는 신고·운영 큐를 사용할 수 있다.
- 운영 endpoint는 기본적으로 인증 필요 또는 moderator 전용이다.
- `@EnableMethodSecurity`와 URL security를 함께 사용한다.

URL 경로만 보호하면 내부 서비스 호출이나 다른 경로에서 우회가 생길 수 있으므로 application method에서도 정책을 확인한다.

### 4.2 소유권 검사

역할이 MEMBER라고 해서 모든 게시글을 수정할 수 있는 것은 아니다. 서버는 다음을 확인한다.

```text
현재 세션 member id == resource owner id
AND resource lifecycle == 허용된 상태
AND version == 요청이 읽은 version
```

이 검사가 IDOR(Insecure Direct Object Reference)를 막는다. 공격자가 URL의 UUID만 다른 값으로 바꾸어도 다른 사람의 게시글·미디어·댓글을 가져갈 수 없어야 한다.

```bash
./gradlew test --tests '*PublicationControllerTest.authorEditsAndDeletesWhileOwnershipAndVersionAreEnforced'
./gradlew test --tests '*MediaControllerTest*'
```

### 4.3 공개 데이터와 private 데이터

분실·목격 위치처럼 민감한 정보는 public response에 정확한 값을 넣지 않는다. 공개 화면에는 근사 위치만 주고 정확한 증거는 권한을 확인한 뒤 별도 signed URL로 제공한다.

## 5. 입력 검증과 오류 응답

입력 검증은 세 단계다.

1. DTO: 길이·필수값·이메일·숫자·enum 형식
2. application/domain: 상태 전이·소유권·비즈니스 정책
3. PostgreSQL: FK·unique·check constraint

하나의 방어선이 실수해도 다음 방어선이 데이터 손상을 막는다.

오류 응답은 `ProblemDetail` 형태로 통일한다. 내부 SQL, stack trace, token과 credential은 외부 응답에 포함하지 않는다. 로그에는 trace id만 남겨 운영자가 내부 로그와 요청을 연결한다.

## 6. 인증 endpoint 남용 방어

### 6.1 로그인 brute force

로그인 endpoint는 공개되어 있어야 하므로 아무나 호출할 수 있다. 그러나 공개와 무제한은 다르다.

`RequestRateLimiter`는 현재 단일 application 인스턴스에서 다음을 제한한다.

- 같은 remote address의 로그인: 분당 30회
- 같은 remote address의 guest step-up: 분당 30회
- 익명 guest author 생성: 분당 30회
- telemetry·CSP report: 분당 600회

제한을 넘으면 `429 Too Many Requests`를 반환한다.

### 6.2 왜 메모리 limiter인가

현재 목표는 단일 portfolio 인스턴스다. Redis를 추가하면 여러 인스턴스에서 카운터를 공유할 수 있지만 운영 복잡도가 증가한다. 지금은 bounded in-memory limiter를 사용한다.

한계는 명확하다.

- 서버를 재시작하면 카운터가 초기화된다.
- 인스턴스가 두 개 이상이면 각자 따로 센다.
- 실제 공개 서비스에서는 Caddy/VPS edge rate limit 또는 Redis 같은 공유 limiter가 필요하다.

이 한계를 코드 주석과 운영 checklist에 남겼다.

Caddy는 외부 요청자가 임의로 넣은 `X-Forwarded-For`를 backend가 신뢰하지 않도록 실제 socket client 주소로 header를 덮어쓴다. backend는 Caddy 뒤에서 전달된 첫 번째 주소를 limiter key로 사용한다. reverse proxy가 여러 단계가 되면 “어떤 proxy를 신뢰할 것인지”를 다시 명시해야 한다.

## 7. 익명 telemetry 보호

acquisition event, web vital, CSP report는 로그인하지 않은 브라우저에서도 전송할 수 있어야 한다. 그러나 공격자가 DB write endpoint로 악용할 수 있다.

그래서 다음을 함께 적용한다.

- payload 길이·metric 이름·숫자 범위 검증
- client event idempotency
- 분당 요청 상한
- CSP report는 DB에 저장하지 않고 수신만 확인
- 공개 endpoint의 response에 내부 정보를 넣지 않음

## 8. 미디어 업로드 보안

### 8.1 presigned URL이 필요한 이유

파일을 Spring 서버가 모두 중계하면 애플리케이션 메모리와 네트워크가 병목이 된다. 서버가 짧은 만료시간의 presigned URL을 발급하고 브라우저가 MinIO에 직접 업로드한다.

하지만 URL을 받았다는 사실만으로 게시글에 연결할 수는 없다.

```text
create upload row: UPLOADING
        ↓
object upload
        ↓
server finalize
  - owner 확인
  - size 확인
  - MIME 확인
  - magic byte 확인
  - checksum 확인
        ↓
READY
        ↓
publication attach
```

READY가 아닌 asset은 publication에 연결할 수 없다.

### 8.2 MinIO 권한

MinIO root credential은 bucket 초기화에만 사용한다. backend는 `townpet-media-app` policy에 연결된 application credential을 사용한다.

application policy는 필요한 object 작업만 허용한다.

- bucket 위치 조회·목록
- object PUT
- object GET
- object DELETE
- multipart 중단·part 목록

MinIO console이나 root credential을 backend에 넣지 않는 것이 핵심이다.

## 9. 브라우저·Caddy 보안 헤더

`deploy/Caddyfile`은 다음 응답 헤더를 추가한다.

- HSTS: HTTPS 사용을 브라우저에 기억시킨다.
- `X-Content-Type-Options: nosniff`: 응답 MIME 추측을 막는다.
- `Referrer-Policy`: 다른 사이트로 경로가 과도하게 전달되지 않게 한다.
- `Permissions-Policy`: 카메라·위치·마이크를 기본 차단한다.
- CSP: 허용된 script/style/image/connect 출처만 사용한다.
- `frame-ancestors 'none'`: 다른 사이트 iframe 안에 TownPet을 삽입하지 못하게 한다.

CSP를 추가하면 정상 기능이 깨질 수도 있으므로 실제 배포 전 브라우저 console에서 blocked resource가 없는지 확인해야 한다. private media PUT은 media domain이 `connect-src`와 CORS에 모두 포함되어야 한다.

## 10. 공급망 보안

애플리케이션 코드가 안전해도 dependency나 Docker base image에 취약점이 있으면 공격받을 수 있다.

`.github/workflows/security.yml`은 다음을 수행한다.

- Trivy filesystem의 HIGH/CRITICAL dependency·secret 검사
- Pull request에서 새 dependency의 severity 검사
- frontend dependency SBOM 생성
- repository static security policy 검사

SBOM은 “취약점이 없다”는 증명이 아니다. 어떤 라이브러리 버전을 사용했는지 추적하는 재료다.

로컬 기본 검사:

```bash
bash scripts/security-static-check.sh
bash -n scripts/*.sh deploy/*.sh
```

Trivy가 설치되어 있다면:

```bash
trivy fs --scanners vuln,secret --severity HIGH,CRITICAL --ignore-unfixed .
```

## 11. 보안 검증 실행 순서

초보자는 다음 순서로 실행하면 된다.

```bash
# 1) 정적 보안 정책
bash scripts/security-static-check.sh

# 2) backend 컴파일·보안 경계 테스트·coverage
./gradlew clean check migrationTest --no-daemon

# 3) frontend type·unit·build
(cd frontend && corepack pnpm install --frozen-lockfile)
(cd frontend && corepack pnpm typecheck && corepack pnpm test && corepack pnpm build)

# 4) browser 정상 흐름
(cd frontend && corepack pnpm test:e2e)

# 5) production Compose 설정 검증
docker compose -f deploy/compose/portfolio.yml config
```

마지막 Compose 명령은 실제 secret file이 있어야 한다. secret을 명령행에 직접 적지 말고 보호된 env file을 사용한다.

## 12. 현재 검증 결과와 해석

현재 저장소에서 확인된 것은 다음이다.

- backend `check migrationTest` 성공
- JaCoCo line 60%, branch 40% 기준 성공
- frontend unit 35개 성공
- Playwright desktop/mobile 54개 성공
- backend image non-root UID 10001 확인
- Caddy/Compose/static security checks 성공

이 결과는 현재 코드가 의도한 방어 규칙을 지키는가를 확인한다. 다음을 증명하지는 않는다.

- 실제 VPS가 안전하다는 것
- DNS/TLS 설정이 올바르다는 것
- SMTP provider가 안전하다는 것
- 모든 dependency에 취약점이 없다는 것
- 새로운 zero-day가 없다는 것
- 공격자가 새로운 우회 방법을 찾지 못한다는 것

## 13. 공개 전 남은 보안 작업

현재 범위에서 반드시 기록해야 하는 외부 전제는 다음이다.

1. 실제 HTTPS에서 secure cookie와 Caddy HSTS 검증
2. 실제 media domain에서 presigned PUT/GET/CORS 검증
3. VPS edge에서 IP·경로별 rate limit 적용
4. SMTP TLS와 secret rotation 검증
5. 이미지·dependency scan 결과 확인
6. IDOR·권한 상승·upload abuse·CSRF 수동 점검
7. backup 복구 후 private media 접근 정책 재확인

이 항목들이 채워지기 전에는 “보안 완료”가 아니라 “로컬 보안 release candidate”라고 표현한다.

## 14. 면접에서 설명하는 30초 답변

> TownPet은 JWT 대신 JDBC session과 CSRF를 사용하고, 역할 검사와 resource ownership을 함께 적용했습니다. 파일은 private MinIO에 직접 업로드하되 서버 finalize에서 owner·MIME·magic byte·checksum을 다시 검사합니다. 익명 telemetry와 로그인·guest endpoint에는 요청 상한을 두고, production image는 non-root로 실행합니다. 다만 메모리 limiter는 단일 인스턴스 범위이므로 실제 다중 인스턴스 공개 운영에서는 edge 또는 Redis 기반 limiter로 확장해야 한다는 한계를 명시했습니다.
