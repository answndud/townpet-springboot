## 결론

현재 프로젝트는 **Spring Boot + React/Vite 기반 포트폴리오 프로젝트로 제출하기에는 충분히 높은 완성도**입니다.

다만 “TownPet 기능을 모두 완벽하게 복제했고, 실제 프로덕션 배포도 끝난 production-ready 프로젝트”라고 말하기에는 아직 외부 운영 검증이 남아 있습니다.

- **로컬 포트폴리오 완성도:** 90/100
- **실제 공개 배포 준비도:** 76/100
- **백엔드 기술 포트폴리오 가치:** 9/10
- **추가 기능보다 정리·운영 검증이 우선인 상태**

현재 작업 트리는 깨끗하고, 최근 기준으로 다음 검증이 완료되어 있습니다.

- parity matrix: `104개 중 95 verified, 9 excluded, pending 0`
- Backend: `./gradlew clean check migrationTest` 성공
- Frontend: typecheck, Vitest 35개, build 성공
- Browser E2E: Desktop/Mobile 포함 54개 성공
- Docker Compose, PostgreSQL migration, backup/restore local rehearsal 확인
- pagination, responsive UI, mobile feed 레이아웃 문제 수정 완료

## 파트별 평가

| 영역 | 점수 | 평가 |
|---|---:|---|
| Backend 아키텍처 | 9.2/10 | Spring Modulith 경계, JPA/jOOQ 분리, Flyway, Spring Security, 이벤트 기반 후속 처리 구성이 좋음 |
| Backend 기능·패리티 | 8.8/10 | 주요 사용자 흐름과 커뮤니티 기능이 연결되어 있고 parity 기준선도 완료됨 |
| Frontend 아키텍처·UX | 8.8/10 | React/Vite, cursor pagination, 반응형 화면, E2E 검증이 갖춰짐 |
| 인증·권한·보안 | 8.3/10 | Session JDBC, CSRF, RBAC, resource ownership, private media 보호가 구현됨 |
| PostgreSQL·데이터 무결성 | 9.0/10 | 제약조건, optimistic locking, bulk update, atomic view count, migration test가 강점 |
| 테스트·CI | 8.8/10 | Backend/Frontend/E2E/Paritiy/Smoke가 모두 존재함 |
| 성능 설계·측정 | 8.0/10 | EXPLAIN, 인덱스, keyset, 동시성 검증이 있으나 대부분 로컬 기준 |
| 배포·운영·복구 | 6.0/10 | Compose, Caddy, backup script는 있으나 실제 VPS·DNS·TLS·외부 백업은 미검증 |
| 문서·면접 대비 | 8.7/10 | engineering story, technical notes, performance report가 잘 갖춰짐 |

가중치를 적용하면 전체 개발 품질은 대략 **8.6/10 수준**입니다.

## 잘된 부분

### 1. 단순 CRUD를 넘어선 백엔드 설계

다음 요소들은 일반적인 개인 포트폴리오보다 확실히 높은 수준입니다.

- Spring Modulith 기반 모듈 경계
- 모듈 간 JPA 연관관계 직접 노출 방지
- JPA와 jOOQ의 역할 분리
- PostgreSQL을 source of truth로 사용
- Flyway append-only migration
- Session JDBC 기반 인증
- CSRF 보호
- 권한과 resource ownership의 이중 검사
- 이벤트 publication registry와 idempotent consumer
- private media의 presigned URL 접근
- 조회수 동시성 처리와 atomic upsert
- keyset cursor 기반 피드 조회
- 대량 공개범위 변경을 bulk update로 개선

이 부분은 면접에서 “왜 이렇게 구현했는가”를 설명하기 좋습니다.

### 2. 기능 검증 수준

현재는 단순히 API가 컴파일되는 수준이 아닙니다.

- 정상 사용자 흐름
- 로그인·로그아웃
- 권한별 접근
- 게시글·댓글·북마크·알림
- 관리자 기능
- 미디어 소유권
- 페이지네이션
- 모바일 반응형
- 브라우저 시나리오

까지 실제로 확인되어 있습니다.

특히 최근 mobile feed에서 발생한 `title width: 0px` 문제를 E2E와 화면 확인으로 찾아 수정한 것은 좋은 사례입니다. 단순히 테스트 통과만 확인한 것이 아니라 실제 사용자 화면 문제를 해결했습니다.

## 아직 부족하거나 확인이 필요한 부분

### 1. 문서와 실제 구현의 불일치

가장 먼저 정리해야 하는 것은 코드가 아니라 문서입니다.

현재 [docs/TRD.md](/Users/alex/project/townpet-springboot/docs/TRD.md)에는 실제 구현과 다른 내용이 일부 남아 있습니다.

예를 들면:

- Cloudflare R2 중심 설계와 실제 MinIO 구현의 차이
- Grafana Alloy 등 미래 운영 구성
- OpenAPI/TypeScript generated client를 암시하는 내용
- 실제로는 사용하지 않는 Terraform/Ansible 목표
- SSR 기반 metadata 목표와 현재 Vite static serving 구조의 차이

반면 [AGENTS.md](/Users/alex/project/townpet-springboot/AGENTS.md)와 [ADR.md](/Users/alex/project/townpet-springboot/ADR.md)는 현재 방향에 맞게 “OpenAPI 파일과 generated client를 사용하지 않는다”고 되어 있습니다.

즉, 현재 가장 필요한 것은 새로운 기능이 아니라:

- TRD를 실제 구현 기준으로 수정
- 아직 구현하지 않은 목표는 `deferred`로 이동
- 현재 범위에서 제외한 PRD 요구사항은 명시적으로 제외
- 이전 리뷰 문서의 오래된 테스트 수·모바일 범위 수정

입니다.

### 2. PRD의 일부 요구사항은 아직 구현되지 않음

현재 parity matrix의 `excluded 9개`는 pending이 아니므로 개발 누락으로 볼 필요는 없습니다. 다만 PRD 전체 기준으로는 다음이 아직 완성되지 않았거나 범위 밖입니다.

- SEO/OG/canonical/sitemap/robots
- OPERATOR·ADMIN까지 포함한 세분화된 staff 권한
- MFA와 관리자 재인증
- 알림 backlog/reprocess
- projection rebuild/reconcile
- lost-found 장기 reminder
- 공유용 flyer 생성
- media orphan reconciliation
- 고급 WAL/PITR/HA 복구
- 실제 운영 모니터링과 SLA 측정

특히 [docs/seo-og-public-community-plan.md](/Users/alex/project/townpet-springboot/docs/seo-og-public-community-plan.md)는 현재 계획 문서에 가깝고 실제 구현은 아직 아닙니다.

따라서 지금은 다음 중 하나를 명확히 해야 합니다.

1. “포트폴리오 sandbox 범위에서는 제외한다”고 ADR/PRD에 기록
2. 실제 public community 서비스로 볼 경우 구현

현재 프로젝트 목적에는 1번이 더 적합합니다.

### 3. 실제 배포 운영은 아직 검증 전

[docs/report/release-readiness.md](/Users/alex/project/townpet-springboot/docs/report/release-readiness.md)와 [docs/runbooks/external-production-checklist.md](/Users/alex/project/townpet-springboot/docs/runbooks/external-production-checklist.md)에 적힌 것처럼 다음은 아직 실제 환경에서 확인하지 않았습니다.

- Hetzner VPS에서의 실제 workload
- DNS/TLS/secure cookie 브라우저 흐름
- MinIO public media domain과 CORS
- 실제 SMTP provider의 발송·SPF·DKIM·deliverability
- 외부 저장소를 포함한 backup retention
- 장애 후 실제 restore 시간
- 실제 VPS resource 사용량
- rollback과 secret rotation

따라서 지금 단계에서 “배포 가능”이라고 말하기보다는:

> 로컬 release candidate는 완성되었고, 외부 인프라 검증만 남아 있다.

라고 표현하는 것이 정확합니다.

### 4. 보안 하드닝

현재 애플리케이션 보안은 좋은 편이지만 운영 전에는 다음을 개선하는 것이 좋습니다.

- MinIO root credential 대신 애플리케이션 전용 access key/policy 사용
- Backend Docker 컨테이너 non-root 실행
- Docker base image digest 고정
- 외부 rate limit 또는 VPS/Caddy 수준 rate limit
- CSP, HSTS, frame 정책의 실제 브라우저 검증
- SMTP secret과 media secret rotation 절차 검증
- production profile에서 필수 환경변수 누락 시 fail-fast 확인

이 부분은 기능 추가보다 실제 서비스 운영 감각을 보여주는 개선입니다.

### 5. 테스트 품질의 작은 개선점

현재 테스트 양은 충분합니다. 테스트를 더 많이 만드는 것은 권하지 않습니다.

다만 다음 두 가지는 가치가 있습니다.

- JaCoCo 최소 coverage threshold 설정
- Mobile E2E를 CI에서도 실행할지 결정

현재 local에서는 mobile E2E까지 54개가 통과했지만 CI는 Chromium 중심입니다. 모바일을 제품 지원 범위로 주장한다면 CI에도 mobile project를 넣거나, 반대로 문서에서 mobile CI 범위를 명확히 제한해야 합니다.

또한 일부 Frontend 코드는 다음 개선 여지가 있습니다.

- API response 타입을 `as` 캐스팅으로 보정하는 부분 제거
- 큰 페이지 컴포넌트 분리
- 실제로 접근되지 않는 `PlaceholderPage` route 정리
- API client와 feature 내부 타입 중복 축소

이는 기능상 blocker는 아니고 유지보수성 개선입니다.

## 추가 작업 우선순위

### 지금 바로 해야 하는 것

기능 개발을 더 늘리기보다는 다음 정리 작업이 우선입니다.

1. [docs/TRD.md](/Users/alex/project/townpet-springboot/docs/TRD.md)와 실제 구현의 차이 정리
2. PRD의 미구현 항목을 `excluded` 또는 `deferred`로 명확히 표시
3. [docs/frontend_review.md](/Users/alex/project/townpet-springboot/docs/frontend_review.md)의 오래된 테스트 수와 모바일 범위 수정
4. 공개 배포 시 이메일·미디어 기능을 실제로 켤 것인지 확정
5. 배포 전 external checklist를 최종 기준으로 고정

### 실제 VPS에 배포하기 전 해야 하는 것

- MinIO 전용 application user/policy
- non-root backend image
- 실제 SMTP 연동
- DNS/TLS/CORS 브라우저 검증
- 외부 backup 저장소와 retention
- restore/rollback rehearsal
- VPS workload 및 resource 측정
- production secret rotation 확인

### 선택적으로 추가할 수 있는 기능

아래는 포트폴리오의 핵심 완성도에 필수는 아닙니다.

- SEO/OG metadata
- OPERATOR/ADMIN/MFA
- projection rebuild
- orphan media reconciliation
- 고급 observability
- Redis/Kafka

특히 Redis와 Kafka는 지금 추가하지 않는 것이 맞습니다. 현재 성능 측정 결과만으로는 도입 근거가 부족하고, 도입하면 운영 복잡도와 설명해야 할 범위만 커집니다.

## 최종 판단

현재 프로젝트는 **개인 개발자용 Spring Boot 백엔드 포트폴리오로 제출해도 충분히 경쟁력 있습니다.**

특히 다음을 강조할 수 있습니다.

> 처음부터 모든 기술을 넣은 것이 아니라, PostgreSQL의 쿼리 병목과 동시성 문제를 측정하고, keyset pagination·bulk update·복합 인덱스·atomic upsert로 개선했다. 이후 실제 필요성이 확인될 때만 Redis나 Kafka를 도입하도록 보류했다.

다만 “완벽히 끝났다”고 선언하려면 기능을 더 만드는 것보다 다음 두 가지가 필요합니다.

1. **문서의 목표와 실제 구현을 일치시키는 작업**
2. **실제 VPS·SMTP·MinIO·백업 환경 검증**

따라서 지금 상태는:

> **로컬 개발과 포트폴리오 제출 기준으로는 완료 단계**  
> **공개 운영과 production-ready 기준으로는 외부 인프라 검증 전 단계**

라고 평가하는 것이 가장 정확합니다.