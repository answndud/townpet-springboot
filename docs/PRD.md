# TownPet Springboot 제품 요구사항

## 1. 문서 목적

이 문서는 기존 TownPet을 Java·Spring Boot 기반으로 재아키텍처하면서 사용자에게 유지해야 할 제품 동작과 완료 조건을 정의한다. 구현 방식은 [`TRD.md`](./TRD.md), 중요한 선택의 근거는 [`../ADR.md`](../ADR.md), 실행 순서는 [`../PLAN.md`](../PLAN.md)를 따른다.

## 2. 제품 정의

TownPet은 반려인이 `지역 + 상황` 기준으로 병원, 산책, 실종·목격, 입양, 거래, 돌봄과 커뮤니티 정보를 찾고 공유하는 반려생활 커뮤니티다. 범용 자유게시판만 제공하지 않고 게시 목적에 맞는 구조화 입력, 상태, 안전 안내와 운영 도구를 함께 제공한다.

이번 프로젝트는 새 제품을 만드는 작업이 아니다. 기존 TownPet의 화면·카피·URL·권한·상태 전이와 반응형 경험을 유지하면서 Next.js 서버, Prisma와 NextAuth를 Spring Boot·PostgreSQL 중심 구조로 교체하는 재아키텍처다.

## 3. 제품 기준선

- 기준 저장소: `/Users/alex/project/townpet`
- 기준 Git commit: `7d8f6d0bd22dedd82350c05142823ab2d101574d`
- 기준 규모: 49개 page, 55개 API route, 비테스트 TSX UI source 181개(이 중 `app/src/components` 113개)
- 기준 증거: Prisma schema와 migration, service·query·validation, Vitest·Playwright, 운영·보안 문서와 배포본
- 동등성 원칙: 내부 class, SQL과 HTTP 구현이 아니라 사용자가 관찰하는 의미 결과가 같아야 한다.
- 의도적 차이: 보안, 데이터 무결성, privacy, 접근성 또는 운영성을 위해 기존 동작을 바꿀 때는 ADR과 parity matrix에 이유·영향·검증을 기록한다.

## 4. 사용자와 운영 주체

### 4.1 방문자

- 로그인하지 않고 공개 피드, 검색, 게시물, 가이드와 지역 landing을 탐색한다.
- 정책이 허용하는 게시판에서는 비회원 글·댓글·목격 제보를 작성하고 콘텐츠별 관리 자격으로 수정·삭제한다.

### 4.2 회원

- 이메일과 비밀번호 Credentials로 로그인한다.
- 동네·관심 정보를 설정하고 게시물·댓글·반응·북마크·신고·알림 기능을 사용한다.
- 자기 콘텐츠와 구조화 게시물의 상태를 허용된 범위에서 관리한다.

### 4.3 콘텐츠 작성자

- Publication과 연결된 병원 후기, 산책, 실종·목격, 장터, 돌봄, 입양, 봉사, 모임 등의 구조화 정보를 관리한다.
- 작성자 삭제와 모더레이션 제한이 서로 다른 상태임을 이해할 수 있어야 한다.

### 4.4 Moderator

- 신고를 조사하고 publication·interaction의 노출 제한을 적용·해제한다.
- 거래 완료, 돌봄 완료, 실종 해결 같은 사용자의 business 상태를 임의로 대신 변경하지 않는다.

### 4.5 Operator

- 배포, backup·restore, event 재처리, projection rebuild와 제한된 데이터 repair를 수행한다.
- 콘텐츠 판정과 회원 역할 승격 권한을 갖지 않는다.

### 4.6 Portfolio 방문자

- 공개 환경에서는 계정 없이 허용된 공개 화면을 탐색한다.
- 공개 demo 계정·개인정보·사용자 생성 콘텐츠는 노출하지 않는다.
- 전체 인증·작성·상태 전이는 local·CI synthetic fixture로 검증한다.

## 5. 목표

### G-01 제품 동등성

기존 TownPet의 49개 page와 55개 API가 제공하는 주요 여정, URL, 반응형 UI, 오류·빈 상태, 권한과 상태 의미를 유지한다.

### G-02 Spring 기반 재아키텍처

서버 책임을 domain module, 명시적 transaction, database constraint, contract-first API와 내구성 event로 재구성하고 Next.js·Prisma·NextAuth 서버 의존성을 최종 제거한다.

### G-03 데이터 의미 보존

기존 식별자, 관계, 상태, 시각과 집계 의미를 검증 가능한 ETL·대사로 보존한다. 실제 개인정보와 secret은 Git에 포함하지 않는다.

### G-04 운영 가능한 Portfolio

공개 URL, PostgreSQL, object storage, CI/CD, 관측성, backup·restore, SLO와 장애 기록을 실제로 운영하되 정상 월 비용을 1만 원 이하로 유지한다.

### G-05 설명 가능한 문제 해결 증거

요구사항에서 데이터 모델·API·상태 머신으로 이어진 과정, 성능 전후 수치, 자동화와 실패·복구 증거를 코드·문서·PR로 남긴다.

## 6. 비목표

- 실제 사용자를 모집하는 상용 커뮤니티 출시는 이번 공개 환경의 목표가 아니다.
- 결제, 에스크로, 배송, 정산, 환불과 private chat을 갖춘 commerce platform을 만들지 않는다.
- TownPet을 전문 펫시터 고용·보험·자격 인증 서비스로 표현하지 않는다.
- 단일 VPS 규모에서 근거 없이 microservice, Kubernetes, Kafka, Elasticsearch 또는 Redis를 추가하지 않는다.
- Next.js, Node.js server runtime, Prisma와 NextAuth를 최종 production에 남기지 않는다.
- 기존 UI를 새 디자인으로 전면 교체하지 않는다.
- 공고에 나온 기술 이름을 사용했다는 사실만을 portfolio 성과로 삼지 않는다.
- Kakao·Naver 회원가입·로그인과 social account 연결·해제는 현재 제품 범위에 포함하지 않는다. 실제 필요가 확인될 때 별도 요구사항과 ADR로 도입한다.

## 7. 핵심 사용자 여정

### J-01 공개 탐색

방문자는 홈, 지역 landing, guest feed와 guest search에서 공개 가능한 콘텐츠를 탐색하고 direct URL 새로고침과 공유 링크로 같은 화면에 도달한다.

### J-02 인증과 계정 보안

회원은 Credentials로 로그인하고 이메일 인증과 비밀번호 재설정을 사용한다. 로그아웃·비밀번호 변경·재설정·제재 시 관련 session이 폐기된다.

### J-03 지역·전체 피드

회원은 `LOCAL / GLOBAL`, 게시 유형, 정렬과 기간 filter를 바꾸며 stable cursor로 피드를 탐색한다. 차단·restriction·publication lifecycle이 모든 목록과 상세에 동일하게 적용된다.

### J-04 게시물 작성과 관리

회원 또는 허용된 GuestPrincipal은 게시 유형에 맞는 구조화 필드를 입력하고 media를 업로드한다. 작성자는 자기 publication을 수정·삭제하고 type별 상태 전이를 수행한다.

### J-05 상호작용

회원은 댓글, 답글, reaction과 bookmark를 사용한다. source row와 화면 summary가 일치하고 반복 요청·동시 요청이 중복 수치를 만들지 않는다.

### J-06 실종·목격

작성자는 실종 또는 목격·보호 Alert를 등록하고 공유 text·전단을 만든다. 회원·비회원은 공개 근사 위치 또는 보호자 전용 정확 위치의 SightingReport를 제출한다. 작성자는 해결 결과 또는 종료 사유를 기록하고 필요하면 재개한다.

### J-07 장터

작성자는 판매·대여·나눔 조건과 상태를 게시한다. TownPet은 listing 정보와 댓글 조율만 제공하며 거래·결제 당사자가 되지 않는다.

### J-08 돌봄

작성자는 이웃 돌봄 요청을 게시하고 지원자 한 명을 선택해 진행·완료한다. 양측 feedback은 공개 평점이 아닌 제한된 안전·운영 정보다.

### J-09 신고와 운영

사용자는 콘텐츠·댓글을 신고한다. Moderator는 report evidence를 검토하고 reversible visibility restriction을 적용한다. Operator는 별도 권한으로 event·projection·backup·repair를 관리한다.

### J-10 Showcase 체험

방문자는 공개된 MEMBER demo 계정으로 일반 로그인, 작성, 수정, 상태 전이를 사용하고 제한된 MODERATOR 계정으로 demo 콘텐츠만 검토한다. ADMIN·OPERATOR credential은 공개되지 않는다.

## 8. 기능 요구사항

### 8.1 Identity·Member·Relationship

- FR-ID-01: Browser 인증은 opaque HttpOnly session cookie를 사용하고 CSRF를 검증해야 한다.
- FR-ID-02: 회원 로그인은 검증된 이메일과 adaptive hash로 보호된 비밀번호 Credentials를 사용해야 한다.
- FR-ID-03: 이메일 인증과 비밀번호 재설정 요청은 계정 존재 여부를 노출하지 않고 만료되는 일회성 token을 사용해야 한다.
- FR-ID-04: 로그아웃, 비밀번호 변경·재설정과 관리자 조치는 관련 session을 즉시 폐기해야 한다.
- FR-ID-05: Guest write는 GuestPrincipal과 콘텐츠 관리 자격을 사용하고 IP·fingerprint는 abuse signal일 뿐 소유권 근거가 아니어야 한다.
- FR-ID-06: Follow·block 관계는 feed, search, detail과 interaction authorization에 일관되게 반영돼야 한다.
- FR-ID-07: Staff 역할은 MEMBER, MODERATOR, OPERATOR, ADMIN 책임을 분리하고 위험 작업에 재인증·MFA·사유·audit를 요구해야 한다.

### 8.2 Publication·Catalog

- FR-PUB-01: 모든 게시물은 공통 Publication과 선택적 구조화 aggregate로 표현돼야 한다.
- FR-PUB-02: Publication lifecycle과 moderator VisibilityRestriction은 분리돼야 하며 작성자 삭제가 restriction을 해제해서는 안 된다.
- FR-PUB-03: `LOCAL / GLOBAL`, neighborhood, community, post type과 animal tag 의미가 기존과 동등해야 한다.
- FR-PUB-04: 게시 유형별 필수 필드, 길이, 금액, 시간과 안전 정책은 UI가 아닌 server에서도 강제돼야 한다.
- FR-PUB-05: 다른 module의 entity를 직접 연결하지 않고 stable identifier와 공개 use case로 참조해야 한다.
- FR-PUB-06: 공개 URL의 title, description, Open Graph와 canonical metadata가 기존 검색·공유 의도를 유지해야 한다.

### 8.3 Engagement

- FR-ENG-01: 일반 Comment와 SightingReport는 서로 다른 resource와 lifecycle이어야 한다.
- FR-ENG-02: 한 사용자는 같은 대상에 reaction·bookmark source row를 중복 생성할 수 없어야 한다.
- FR-ENG-03: Comment, Reaction과 Bookmark 원장 및 EngagementSummary가 동시 요청 후에도 일치해야 한다.
- FR-ENG-04: View count는 privacy-preserving viewer key와 time bucket으로 중복 집계돼야 한다.
- FR-ENG-05: 차단·제재·restriction·deleted lifecycle은 read model과 interaction command에 같은 결과를 내야 한다.

### 8.4 LostFound

- FR-LF-01: Alert는 `LOST / FOUND`, 동물 정보, 마지막 확인 시간·위치와 lifecycle을 가져야 한다.
- FR-LF-02: 공개 위치는 약 250m 근사 point·안전 label이고 정확 위치 evidence는 공개 API·검색·공유에 포함되지 않아야 한다.
- FR-LF-03: SightingReport는 목격 시간, 위치, 설명, reporter, media와 `PUBLIC_APPROXIMATE / OWNER_ONLY_EXACT` 공개 범위를 가져야 한다.
- FR-LF-04: Owner-only 위치·사진은 Alert 작성자와 case 권한이 있는 reviewer만 열람하고 조회를 audit해야 한다.
- FR-LF-05: `RESOLVED`에는 resolution outcome, `CLOSED`에는 close reason, 재개에는 reopen reason이 필요해야 한다.
- FR-LF-06: 종료된 Alert는 active feed·반경 검색에서 빠지고 신규 SightingReport를 받지 않아야 한다.
- FR-LF-07: 7일·14일·30일 active 확인 알림을 제공하되 Alert를 자동 종료해서는 안 된다.
- FR-LF-08: 기존 공유 text, Kakao 공유 entry와 SVG·PNG 전단 flow를 유지해야 한다.

### 8.5 Marketplace

- FR-MKT-01: Marketplace는 결제 없는 `SELL / RENT / SHARE` classified listing이어야 한다.
- FR-MKT-02: Sale, Rental, Share 조건은 잘못된 nullable 조합을 생성할 수 없는 유형별 schema여야 한다.
- FR-MKT-03: `AVAILABLE → RESERVED|COMPLETED|CANCELLED`, `RESERVED → AVAILABLE|COMPLETED|CANCELLED`만 작성자 정상 전이로 허용해야 한다.
- FR-MKT-04: 예약 이후 거래 조건을 변경할 수 없고 수정하려면 먼저 reopen해야 한다.
- FR-MKT-05: 생체 판매, 유통기한 경과 식품과 동물 의약품은 versioned deterministic policy로 차단해야 한다.
- FR-MKT-06: 모호한 위험 표현은 자동 삭제하지 않고 warning·AbuseSignal·review 대상으로 처리해야 한다.

### 8.6 Care·Gathering·Welfare·LocalGuide

- FR-AUX-01: Care는 Request·Application·Assignment를 구분하고 요청당 active assignment를 하나만 허용해야 한다.
- FR-AUX-02: Care reward는 참고 정보이며 결제·지급 보증으로 표현하지 않아야 한다.
- FR-AUX-03: Meetup 참가 정원과 중복 참가를 database constraint·conditional update로 보호해야 한다.
- FR-AUX-04: 병원 후기, 장소, 산책, 입양, 봉사, 품종 lounge와 공동구매의 기존 구조화 필드·filter·화면을 유지해야 한다.
- FR-AUX-05: Care 이하 저우선순위 domain은 별도 제품 확장 없이 parity matrix와 기존 test behavior를 우선해야 한다.

### 8.7 Discovery·Notification

- FR-DIS-01: Search는 title·body·구조화 field를 검색하고 typo·부분 일치와 기존 filter를 지원해야 한다.
- FR-DIS-02: Feed는 versioned ranking, stable cursor와 viewer-safe visibility를 제공해야 한다.
- FR-DIS-03: SearchDocument·FeedDocument가 지연되더라도 hidden·deleted·blocked resource를 노출해서는 안 된다.
- FR-DIS-04: Projection freshness를 측정하고 원장에서 재구축할 수 있어야 한다.
- FR-NOT-01: 알림은 원장 transaction 이후 durable event로 생성하고 중복 delivery를 방지해야 한다.
- FR-NOT-02: 읽음 상태, filter, retry와 unread count가 기존 알림 화면과 동등해야 한다.

### 8.8 TrustSafety·Operations

- FR-TS-01: Report는 접수·검토·해결 lifecycle, evidence reference와 moderator action audit를 가져야 한다.
- FR-TS-02: 자동화는 reversible hide만 수행하고 영구 삭제·중징계는 사람 검토 없이 실행하지 않아야 한다.
- FR-TS-03: Authorization은 deny-by-default RBAC와 resource attribute policy를 결합해야 한다.
- FR-TS-04: private resource ID를 다른 사용자 ID로 교체해도 존재·내용이 노출되지 않아야 한다.
- FR-OPS-01: Event backlog·실패를 조회하고 idempotent하게 재처리할 수 있어야 한다.
- FR-OPS-02: Search, Feed와 Engagement projection을 원장에서 rebuild·reconcile할 수 있어야 한다.
- FR-OPS-03: Backup, restore, deployment, demo reset과 repair는 dry-run 또는 scoped confirmation, audit와 metric을 제공해야 한다.

### 8.9 Media

- FR-MED-01: Client는 presigned URL로 object storage에 직접 업로드하고 server finalize 전에는 publication에 연결할 수 없어야 한다.
- FR-MED-02: MIME, magic byte, byte·pixel·count, owner, expiration과 lifecycle을 검증해야 한다.
- FR-MED-03: 원본·thumbnail·derivative metadata와 object orphan을 대사·정리할 수 있어야 한다.
- FR-MED-04: Owner-only sighting media는 public URL과 public CDN cache에 노출되지 않아야 한다.

## 9. Showcase 요구사항

- DEMO-01: 공개 가입과 공개 demo 계정 로그인을 제공하지 않는다.
- DEMO-02: production database에 migration demo identity·content가 남아 있지 않아야 한다.
- DEMO-03: local·CI에서는 최소 3개 MEMBER와 제한된 MODERATOR synthetic fixture로 인증·작성·권한 흐름을 검증한다.
- DEMO-04: production bootstrap은 web 노출 전에 scoped sanitize와 private operator bootstrap을 실행한다.
- DEMO-05: ADMIN, OPERATOR와 emergency credential은 공개하지 않는다.
- DEMO-06: 공개 화면은 빈 상태를 정상적으로 표시하고 실제 개인정보 입력을 유도하지 않는다.
- DEMO-07: 공개 기능을 다시 interactive로 전환할 때 별도 공개가입·moderation·data retention decision을 만든다.

## 10. 비기능 요구사항

### 10.1 제품 품질

- NFR-Q-01: Page·API·data·권한·responsive·accessibility·SEO 행을 가진 parity matrix가 있어야 한다.
- NFR-Q-02: Legacy와 Spring에 같은 logical fixture를 적용하는 differential test가 있어야 한다.
- NFR-Q-03: Dynamic value normalization과 visual mask 변경은 review 대상이어야 한다.
- NFR-Q-04: Legacy adapter, Next.js, Prisma 또는 임시 flag가 남은 domain은 완료로 표시할 수 없다.

### 10.2 성능·SLO

- NFR-SLO-01: 공개 핵심 여정 30일 rolling 가용성은 99.5% 이상이어야 한다.
- NFR-SLO-02: 핵심 API server-side 성공률은 99.5% 이상이어야 한다.
- NFR-SLO-03: Controlled load에서 read API p95는 300ms, write API p95는 500ms 이하여야 한다.
- NFR-SLO-04: 한국 mobile p75 기준 LCP 2.5초, INP 200ms, CLS 0.1 이하를 목표로 한다.
- NFR-SLO-05: Projection 반영 p95는 30초, 정상 상태 oldest backlog는 60초 이하여야 한다.
- NFR-SLO-06: 사용자 영향 장애를 최초 실패 뒤 5분 안에 탐지하는 것을 목표로 한다.

### 10.3 복구·비용

- NFR-REC-01: PostgreSQL RPO는 5분, 장애 인지 후 RTO는 60분이다.
- NFR-REC-02: WAL archive, logical·physical backup과 실제 restore drill을 수행해야 한다.
- NFR-REC-03: 정상 월 production 비용은 trial·credit 제외 1만 원 이하여야 한다.
- NFR-REC-04: 단일 VPS의 장애 지점을 숨기지 않고 restore·rebuild·rollback evidence로 보완해야 한다.

### 10.4 보안·Privacy

- NFR-SEC-01: Secret, password, cookie, token, 관리 credential과 정확 위치를 Git·log·trace·metric에 기록하지 않아야 한다.
- NFR-SEC-02: Session fixation, CSRF, IDOR, privilege escalation, upload abuse와 rate-limit bypass test가 있어야 한다.
- NFR-SEC-03: Public read model에는 private field가 type·query 수준에서 포함되지 않아야 한다.
- NFR-SEC-04: 실제 community launch는 개인정보·국외 이전·약관·moderation readiness 결정 없이는 허용하지 않는다.

## 11. 완료 판정

### 11.1 Domain 완료

한 domain은 다음 항목이 모두 충족돼야 완료다.

1. 원본 page·API·schema·policy·test 행이 parity matrix에 연결됐다.
2. Flyway schema와 migration ETL·대사가 통과했다.
3. Spring application/domain, PostgreSQL persistence와 frontend가 한 vertical slice로 동작한다.
4. Authorization·concurrency·error·event·projection test가 통과한다.
5. Legacy/Spring differential 및 필요한 visual·accessibility test가 통과한다.
6. 해당 domain의 legacy adapter, Prisma 접근, Next.js route와 임시 flag가 제거됐다.
7. Runbook, metric, alert와 repair/rebuild 경로가 필요한 경우 준비됐다.

### 11.2 프로젝트 완료

- 49개 page와 55개 legacy API의 모든 parity 행이 `PASS` 또는 승인된 의도적 차이다.
- Production artifact에 Next.js·Prisma·NextAuth server runtime이 없다.
- Fresh clone에서 local PostgreSQL·PostGIS·object storage와 application을 재현할 수 있다.
- Public showcase가 고정 demo account, scoped reset, TLS와 운영 SLO로 동작한다.
- Backup restore, event·projection rebuild, failed deployment rollback을 실제로 수행한 증거가 있다.
- README에서 5분 안에 실행 방법, architecture, 대표 문제 해결, 성능 전후와 공개 URL을 확인할 수 있다.

## 12. 성공 지표

- Parity matrix 완료율 100%
- 핵심 differential scenario pass 100%
- Critical authorization·state·concurrency mutation score 80% 이상
- 변경 line coverage 85%, 변경 branch coverage 80% 이상
- 공개 SLO와 RPO·RTO 목표 충족
- 월 운영비 1만 원 이하
- Restore drill·deployment rollback·projection rebuild 성공 기록 존재
- 성능 최적화 전후의 동일 조건 p50·p95·query plan 비교 자료 존재

## 13. 공고 역량과 Portfolio Evidence

| 공고에서 보는 역량 | TownPet Springboot 증거 |
|---|---|
| Java·Spring·Gradle | Java 25, Spring Boot 4.1, Spring Modulith, Spring Security, JPA·jOOQ, Gradle 9 |
| 확장 가능한 구조 | 17개 bounded context, 공개 module API·event, verified dependency graph |
| 요구사항 분석 | 49 page·55 API parity matrix, PRD, state machine, controller contract |
| 데이터 모델·API | Publication·domain aggregate, constraint, contract-first REST, ProblemDetail |
| 성능 최적화 | Query-count, EXPLAIN ANALYZE, Feed·Search projection, controlled load 전후 수치 |
| 자동화 | Quality gate, migration rehearsal, code generation drift, demo reset, restore drill |
| 운영 배포 | Hetzner, Caddy, immutable image, A/B container, telemetry, SLO, rollback |
| 장애 대응 | RPO 5분·RTO 60분, WAL, offsite backup, rebuild·reconciliation evidence |

## 14. 가정과 오픈 질문

- 가정: 기준 commit 이후 원본 TownPet 변경은 자동 scope에 포함하지 않고 별도 수용 결정을 거친다.
- 가정: 공개 showcase traffic은 단일 CX23으로 처리 가능한 portfolio 규모다.
- 가정: Care, Adoption, Volunteer, Meetup과 LocalGuide의 세부 behavior는 별도 확장보다 legacy parity를 우선한다.
- 오픈 질문: Demo persona별 정확한 fixture·edge scenario는 parity inventory에서 확정한다.
- 오픈 질문: 한국 도시·농촌에서 250m 공개 위치 정밀도의 사용성을 map prototype으로 검증한다.
- 오픈 질문: 실제 community launch 요구가 생기면 개인정보, 국외 이전, retention과 운영 인력을 별도 PRD로 정의한다.
