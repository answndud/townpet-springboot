# 공개 커뮤니티 SEO·Open Graph 실행 계획

## 문서 목적

이 문서는 TownPet을 실제 공개형 반려동물 커뮤니티로 운영한다고 가정할 때, 검색 유입과 링크 공유 미리보기를 확보하기 위한 작업 순서와 완료 기준을 정의한다.

현재는 구현하지 않는다. 공개 배포를 결정한 시점에 이 문서를 실행 계획으로 사용한다.

제품 요구사항의 기준은 [`docs/PRD.md`](./PRD.md)의 `FR-PUB-06`이다. 현재 기술 경계는 [`docs/TRD.md`](./TRD.md)에 따라 React 19 + Vite + React Router frontend, Spring Boot modular monolith backend, PostgreSQL이며 Node.js는 production server로 사용하지 않는다.

## 최종 목표

비로그인 사용자가 접근 가능한 공개 게시글 URL이 다음 조건을 만족한다.

- 검색엔진이 게시글 제목·설명·본문 핵심 내용을 안정적으로 발견할 수 있다.
- 카카오톡·네이버·Twitter/X·Slack·Discord 등 링크 미리보기에 게시글별 제목·설명·이미지가 표시된다.
- canonical URL이 중복 URL과 query parameter로 분산되지 않는다.
- 삭제·비공개·제재·로그인 필요 콘텐츠는 색인되지 않는다.
- 로그인, 작성, 댓글, 좋아요, 북마크 같은 앱 기능은 기존 CSR 흐름을 유지한다.
- 공개 URL의 SEO 품질을 실제 crawler와 공유 debugger로 검증할 수 있다.

## 현재 상태와 확인된 gap

현재 구현에는 다음 제한이 있다.

| 영역 | 현재 상태 | 영향 |
| --- | --- | --- |
| 기본 HTML | `frontend/index.html`에 공통 title·viewport·theme-color만 존재 | 모든 route가 동일한 초기 metadata를 가짐 |
| 게시글 상세 | `/posts/:publicationId`에서 React가 API를 호출한 뒤 내용을 렌더링 | JS를 실행하지 않는 crawler가 게시글 내용을 읽기 어려움 |
| document title | 일부 route만 React effect로 title 변경 | 초기 HTML·공유 crawler에는 반영되지 않음 |
| Open Graph | 게시글별 `og:*` metadata 없음 | 링크 공유 시 제목·설명·이미지 제어 불가 |
| canonical | 구현 확인되지 않음 | `/posts/:id`, `/posts/:id/guest`, query URL 중복 가능 |
| robots/sitemap | 공개 배포용 정책 확인 필요 | 색인 대상과 제외 대상을 crawler에 명확히 전달하지 못함 |
| 구조화 데이터 | Article/DiscussionForumPosting JSON-LD 없음 | 검색 결과의 콘텐츠 의미 전달이 약함 |
| 이미지 | 게시글 대표 이미지·OG 이미지 정책 없음 | 미리보기 품질·이미지 개인정보 정책을 보장하기 어려움 |

CSR이라고 검색엔진에 반드시 색인되지 않는 것은 아니다. 다만 Google의 JavaScript 처리에 의존하는 방식보다, 공개 URL의 초기 HTML에 핵심 metadata와 본문을 제공하는 방식이 네이버·소셜 crawler까지 포함하면 더 안정적이다.

## 결정 원칙

1. **전체 frontend를 먼저 SSR로 마이그레이션하지 않는다.** 공개 유입에 직접 영향을 주는 게시글 상세와 공개 목록부터 선택적으로 해결한다.
2. **Spring Boot를 source of truth로 유지한다.** 게시글 상태·공개 범위·제재 상태·대표 이미지는 backend 정책과 같은 규칙을 사용한다.
3. **공개 콘텐츠와 개인화 콘텐츠를 분리한다.** `/`의 전체글·인기글은 SEO 후보가 될 수 있지만 내 피드·관심 동물 필터·로그인 화면은 색인 대상이 아니다.
4. **SEO metadata는 보안 경계가 아니다.** 정확한 위치·연락처·비공개 정보·로그인 전용 필드를 description, JSON-LD, OG image에 넣지 않는다.
5. **실제 색인과 공유 preview를 각각 검증한다.** 검색 crawler가 HTML을 읽는지와 카카오/소셜 crawler가 OG metadata를 읽는지는 별도 acceptance로 취급한다.
6. **측정 후 SSR 범위를 결정한다.** Search Console 색인율, crawl error, 공유 preview 성공률, LCP/TTFB와 실제 유입 데이터를 확인하기 전에는 framework 교체를 결정하지 않는다.

## 실행 순서

### P0 - 공개 URL과 색인 정책을 고정한다

**목표:** 어떤 URL이 public indexable resource인지 먼저 확정한다.

- 공개 대상: 게시글 상세, 공개 게시글 목록, 추천 기반 인기글 목록, 공개 community/board detail
- 제외 대상: 내 피드, 북마크, 알림, 프로필의 비공개 활동, 관리자, 작성·수정 form, 로그인·온보딩, guest 관리 password flow
- 상태별 정책: `ACTIVE + 공개 범위 허용`만 indexable; 삭제·hidden·restricted·비공개는 `noindex` 또는 404/410 정책을 사용
- URL 정책: 게시글 canonical은 `/posts/{publicationId}` 하나로 고정하고 `/posts/{id}/guest`는 canonical로 redirect하거나 동일 canonical metadata를 사용
- query 정책: `q`, `scope`, 개인화·tracking parameter는 canonical에서 제거하고 필요하면 `noindex,follow`

파일 후보:

- `docs/PRD.md`
- `docs/TRD.md`
- `ADR.md`
- `frontend/src/App.tsx`
- backend 공개 게시글 controller/service 및 route 정책

검증:

- 공개·비공개·삭제·제재·로그인 필요 URL 매트릭스를 작성한다.
- 각 URL의 expected status, canonical, robots directive, 공개 필드 목록을 표로 고정한다.

완료:

- “색인 가능/불가” 판정이 UI 코드가 아니라 명시적인 제품 정책으로 설명된다.

### P1 - metadata 모델과 공개 SEO contract를 만든다

**목표:** 게시글 상세에서 사용할 metadata를 backend 계약으로 정의한다.

metadata contract 예시:

```text
canonicalUrl
title
description
ogType
ogImageUrl
publishedAt
modifiedAt
authorDisplayName (공개 허용 시에만)
robots
```

- title: 게시글 제목 + `| TownPet` 형식, 길이 제한과 fallback 정의
- description: 본문 앞부분을 정규화하되 정확 위치·연락처·민감정보 제외
- image: 사용자 업로드 원본을 직접 노출하지 않고 공개 허용된 thumbnail/derivative만 사용
- fallback: 대표 이미지가 없으면 안전한 TownPet 기본 이미지 사용
- 상태 필터: backend가 색인 가능 여부와 공개 metadata를 함께 결정
- cache: 공개 metadata 응답의 cache/revalidation 정책과 삭제·제재 시 무효화 정책 정의

파일 후보:

- `src/main/java/com/townpet/publication/`
- `src/main/java/com/townpet/media/`
- `frontend/src/api/client.ts`
- HTTP response DTO와 관련 controller test

검증:

- 공개·비공개·삭제·민감정보 포함 본문 fixture 각각에서 metadata contract를 검사한다.
- backend 응답에 비공개 위치·연락 증거·관리 정보가 포함되지 않는지 확인한다.

완료:

- frontend가 임의로 title/description을 조립하지 않고 backend 정책과 같은 결과를 사용한다.

### P2 - 최소 SEO 기반을 추가한다

**목표:** SSR을 도입하기 전에도 검색 crawler가 이해할 수 있는 기본 자산을 제공한다.

- `robots.txt` 추가
- `sitemap.xml` 또는 sitemap index 추가
- 공개 게시글 canonical URL만 sitemap에 포함
- 게시글 생성·수정·삭제·제재 후 sitemap 대상이 일관되게 갱신되도록 설계
- `lang="ko"`, favicon, theme color, 기본 site metadata 정비
- route별 document title/description은 브라우저 navigation에서도 갱신
- 404/410과 soft 404를 구분
- sitemap에 무한 query URL, 로그인 route, 관리자 route를 넣지 않음

파일 후보:

- `frontend/index.html`
- `frontend/src/App.tsx`
- Spring Boot static/resource handler 또는 공개 SEO controller
- `frontend/src/utils/seo.ts`
- `frontend/src/*.test.tsx`

검증:

- `curl -i https://public-host/robots.txt`
- `curl -i https://public-host/sitemap.xml`
- sitemap XML schema, URL count, canonical host, excluded route를 검사하는 script
- route navigation 후 `document.title`과 canonical link 테스트

완료:

- crawler가 robots와 sitemap을 읽고, 각 공개 URL의 canonical 목록을 얻을 수 있다.

### P3 - 공개 게시글 상세의 초기 HTML을 서버 제공 방식으로 바꾼다

**목표:** 소셜 crawler와 JS 미실행 crawler가 게시글별 metadata와 핵심 내용을 바로 읽게 한다.

권장 1차안은 전체 SPA를 바꾸지 않고, Vite가 빌드한 실제 `dist/index.html`을 Spring Boot가 게시글별 metadata로 보강해 반환하는 것이다.

```text
GET /posts/{id}
  → 공개 상태 확인
  → title/description/canonical/OG/JSON-LD가 포함된 초기 HTML 반환
  → React bundle이 client takeover 또는 hydration
```

#### Spring Boot 렌더링 전략

별도로 수동 관리하는 SEO shell은 Vite의 해시 asset 파일명과 어긋날 수 있으므로 만들지 않는다. `frontend/index.html`을 canonical template source로 삼고, 그 안에 metadata placeholder를 둔다. Vite build가 만든 `frontend/dist/index.html`에는 최신 JS/CSS 해시 파일명이 이미 반영되므로 Spring Boot는 이 빌드 산출물을 package에 포함하고 애플리케이션 시작 시 한 번 읽어 메모리에 캐시한다.

```html
<title>__SEO_TITLE__</title>
<meta name="description" content="__SEO_DESCRIPTION__">
<link rel="canonical" href="__SEO_CANONICAL__">
<meta property="og:title" content="__OG_TITLE__">
<meta property="og:description" content="__OG_DESCRIPTION__">
<meta property="og:url" content="__OG_URL__">
<meta property="og:image" content="__OG_IMAGE__">
```

- Vite build가 끝난 `dist/index.html`을 Spring Boot 배포 artifact와 함께 묶는다. 별도 shell과 asset 목록을 수동 동기화하지 않는다.
- 애플리케이션 시작 시 `dist/index.html` 존재 여부, 필수 placeholder 존재 여부, 참조된 hashed asset 존재 여부를 검증하고 실패하면 기동하지 않는다.
- 요청마다 `frontend/dist/index.html`을 디스크에서 읽지 않는다. immutable template을 startup cache로 사용한다.
- 정규식으로 HTML을 수정하지 않고, 충돌하지 않는 고유 placeholder를 exact replacement한다.
- `<title>`, `content`, `href` 값은 HTML attribute escaping을 적용한다.
- JSON-LD는 문자열 연결이 아니라 Jackson serialization으로 생성한다.
- 게시글 metadata는 backend 공개 정책에서 생성하고 frontend가 임의로 조합하지 않는다.
- Vite의 전체 `index.html`을 Thymeleaf template으로 해석하지 않는다. 현재 범위에서는 명시적 placeholder renderer로 충분하며, React bundle과 정적 asset 경로는 Vite 산출물의 값을 그대로 사용한다.

Thymeleaf는 다음 조건이 생길 때만 별도 ADR로 도입을 재평가한다.

- 게시글·게시판·입양·분실 제보 등 서버 렌더링 페이지 종류가 늘어난다.
- 공개 상태에 따른 HTML fragment와 조건부 layout 분기가 많아진다.
- 서버가 metadata뿐 아니라 본문·breadcrumb·댓글 요약까지 렌더링해야 한다.
- template 운영 편의성이 단순 placeholder renderer의 낮은 복잡도보다 중요해진다.

다음 방식은 사용하지 않는다.

- 별도 SEO shell과 Vite의 `dist/index.html`을 각각 관리해 hashed asset 파일명을 수동 동기화하는 방식
- 요청마다 `frontend/dist/index.html`을 파일 I/O로 읽는 방식
- `replaceAll` 정규식으로 사용자 제목·본문을 직접 주입하는 방식
- HTML escaping 없이 title/description을 연결하는 방식
- JSON-LD를 수동 문자열 연결로 생성하는 방식

template 원문은 immutable startup cache로 관리하고, 공개 metadata는 `updatedAt`, ETag 또는 짧은 CDN TTL을 사용한다. 수정·삭제·제재 시 metadata cache를 무효화하며, 소셜 서비스의 preview cache가 남는 지연은 운영 runbook에 기록한다.

#### 공개 상태와 HTTP status

서버가 HTML shell을 반환하더라도 게시글의 공개 가능 여부를 먼저 판정한다. 검색엔진에 “존재하지 않는 글”을 200으로 내려주는 soft 404를 만들지 않는다.

- 존재하지 않거나 영구 삭제된 게시글: `404 Not Found` 또는 정책상 `410 Gone`
- 비공개·로그인 필요·권한 없는 게시글: 외부 공개 route에서는 존재 여부를 드러내지 않는 `404 Not Found`
- 일시적인 moderation 상태: 공개 정책에 맞는 `404` 또는 `noindex`를 명시하고, 본문·OG·JSON-LD는 노출하지 않음
- `noindex`는 검색 제외 지시일 뿐 status code 대체재가 아니므로, 리소스 부재·비공개 판정과 분리해 적용

이 판정은 React가 실행되기 전에 Spring Boot controller/service에서 수행하며, API 응답과 HTML 응답이 서로 다른 공개 상태를 보지 않도록 하나의 backend policy를 공유한다.

#### React metadata ownership

서버가 주입한 게시글 metadata를 React 초기 부팅이 일반 제목으로 덮어쓰지 않도록 metadata 소유권을 하나의 모듈로 통합한다.

- `/posts/:id` 직접 진입 시 AppShell의 generic title effect가 서버의 게시글 title, description, canonical, OG를 덮어쓰지 않는다.
- 게시글 API 응답이 도착하기 전에는 기본 metadata로 되돌리지 않는다.
- 게시글 API 응답 후와 SPA 게시글 이동 시에만 중앙 `SeoMetadata` 로직이 실제 공개 데이터로 갱신한다.
- 직접 진입과 목록에서 상세로 이동하는 두 경로를 모두 테스트해 metadata 깜빡임과 stale 값이 없는지 확인한다.

#### 빌드·배포 일관성

HTML과 hashed asset이 서로 다른 배포에서 섞이지 않도록 frontend build 산출물과 backend artifact의 릴리스 단위를 고정한다.

- build 단계에서 `frontend/dist/index.html`과 모든 referenced asset을 같은 artifact에 수집한다.
- startup validation에서 placeholder 누락, asset 누락, 빈 기본 metadata를 거부한다.
- 배포는 새 HTML과 asset을 함께 올린 뒤 전환하고, 이전 asset을 CDN cache 기간 동안 즉시 삭제하지 않는다.
- `curl`로 직접 진입한 HTML의 asset URL이 실제로 200인지 배포 smoke에서 검사한다.

#### crawler, WAF, rate limit

SEO endpoint는 User-Agent만으로 인증·권한을 우회시키지 않는다. 공개 게시글은 일반 비로그인 공개 정책으로 접근시키고, User-Agent는 관측과 rate-limit 분류에만 사용한다.

- `Googlebot`, `Naverbot`, `facebookexternalhit`, `Twitterbot`, `Slackbot`, `Discordbot`, `kakaotalk-scrap`은 필요하면 별도 관측·완화 정책을 적용하되 allowlist를 authorization 근거로 삼지 않는다.
- User-Agent spoofing을 전제로 IP 검증·WAF bot policy·캐시 정책을 별도로 둔다.
- 비정상 요청은 `429 Too Many Requests`와 `Retry-After`로 응답하며, 정상 crawler와 일반 사용자의 공개 GET을 구분해 과도하게 차단하지 않는다.
- `GET`뿐 아니라 crawler가 사용하는 `HEAD` 응답의 status와 cache policy도 확인한다.
- CSRF, 관리자 권한, 비공개 게시글 접근을 crawler 예외로 풀지 않는다.

구현 선택지는 다음 우선순위로 비교한다.

1. Spring Boot가 게시글 상세용 HTML shell과 metadata를 제공하고 기존 React가 takeover
2. 정적 build 전용 prerender로 공개 fixture/변경 대상 페이지 생성
3. 공개 route만 별도 SSR runtime으로 분리
4. 전체 frontend framework를 SSR framework로 마이그레이션

첫 번째 선택이 현재 기술 경계와 가장 잘 맞는다. 단, React Router fallback과 Spring Boot route 충돌, API 응답과 HTML 응답의 cache 정책을 먼저 검증한다.

파일 후보:

- `frontend/index.html`
- `frontend/vite.config.ts`
- `frontend/package.json`
- `src/main/java/com/townpet/publication/`
- `src/main/resources/templates/` 또는 명시한 HTML renderer 위치
- `src/main/java/com/townpet/common/web/` 또는 SEO renderer를 둘 명시적 위치
- `frontend/src/features/publication/PublicationDetailPage.tsx`
- `deploy/caddy/` 또는 reverse proxy route 설정
- 관련 controller/integration/e2e test

검증:

- `curl -s https://public-host/posts/{public-id}` 결과에 게시글 title, description, canonical, `og:*`, JSON-LD, 핵심 heading이 있는지 검사한다.
- JavaScript를 비활성화한 브라우저 또는 HTML-only crawler로 핵심 내용이 보이는지 확인한다.
- 로그인 필요·삭제·비공개 게시글이 HTML에 노출되지 않고 expected `404/410`을 반환하는지 확인한다.
- build 후 HTML이 참조하는 hashed JS/CSS가 모두 존재하고, startup validation이 잘못된 artifact를 거부하는지 확인한다.
- 직접 진입과 SPA 이동 후 React가 서버 metadata를 generic metadata로 덮어쓰지 않는지 확인한다.
- 주요 crawler User-Agent의 `GET`/`HEAD`, 일반 사용자, spoofed User-Agent의 status·rate limit·권한 결과를 비교한다.

완료:

- 공개 게시글 상세 URL의 첫 응답만으로 검색·공유 crawler가 게시글 identity를 파악할 수 있다.
- template은 startup에 한 번 로드되고 요청마다 디스크 파일을 읽지 않는다.
- 사용자 제공 값은 HTML/JSON-LD 문맥에 맞게 escape/serialize된다.

### P4 - Open Graph와 공유 이미지 pipeline을 구현한다

**목표:** 링크 공유 시 게시글별 preview가 안정적으로 표시된다.

- `og:title`, `og:description`, `og:url`, `og:type`, `og:site_name`, `og:locale`
- `twitter:card`, `twitter:title`, `twitter:description`, `twitter:image`
- `og:image:width`, `og:image:height`, `og:image:alt`
- 기본 fallback image와 게시글 대표 thumbnail 선택 규칙
- 이미지 포맷·용량·최소 해상도·캐시 TTL 결정
- 이미지에 정확 위치·개인 연락처·민감한 동물 정보가 포함되지 않도록 검수
- 게시글 삭제·비공개 전환 시 preview cache가 오래 남는 한계를 runbook에 기록
- 필요하면 서버에서 게시글 metadata를 바탕으로 안전한 dynamic OG image 생성

파일 후보:

- `src/main/java/com/townpet/media/`
- `src/main/java/com/townpet/publication/`
- `deploy/` object storage/CDN 설정
- `docs/runbooks/` 공유 preview cache 갱신 절차

검증:

- 카카오톡 링크 미리보기 debugger
- Facebook Sharing Debugger
- LinkedIn Post Inspector
- Twitter/X card validator 또는 대체 HTML fetch 검증
- 각 도구에서 제목·설명·이미지·canonical URL이 기대값인지 확인

완료:

- 대표 이미지가 있는 글과 없는 글 모두에서 preview가 깨지지 않고, 비공개 정보가 노출되지 않는다.

### P5 - 구조화 데이터와 검색 전용 콘텐츠 품질을 추가한다

**목표:** 검색엔진이 페이지 의미와 게시글 구조를 해석할 수 있게 한다.

- 게시글 상세에 `Article` 또는 실제 표현에 맞는 `DiscussionForumPosting` JSON-LD 검토
- `headline`, `description`, `datePublished`, `dateModified`, `author`, `mainEntityOfPage`, `image`
- 댓글은 실제 공개 댓글만 포함하고 비공개·삭제·제재 댓글은 제외
- 게시판 목록은 BreadcrumbList 등 필요한 최소 구조화 데이터만 사용
- 허위 rating, 허위 organization, 검색 순위 조작용 keyword stuffing은 사용하지 않음
- 구조화 데이터와 화면에 실제 표시되는 값이 항상 일치하도록 test

검증:

- Google Rich Results Test
- Schema Markup Validator
- 실제 HTML과 JSON-LD의 title/date/author/image 일치 검사

완료:

- 구조화 데이터가 유효하고 화면 콘텐츠와 불일치하지 않는다.

### P6 - 색인 등록·관측·운영 절차를 만든다

**목표:** metadata를 추가한 뒤 실제 검색 노출을 확인하고 회귀를 감지한다.

- Google Search Console property와 sitemap 등록
- 네이버 서치어드바이저 사이트 등록과 sitemap/RSS 정책 확인
- Bing Webmaster Tools 등 필요 채널 등록
- crawler 4xx/5xx, soft 404, blocked resource, canonical mismatch, 색인 제외 사유 관측
- 공개 게시글 생성·수정·삭제·제재의 색인 상태 runbook
- `robots.txt` 변경과 canonical host 변경에 대한 배포 전 검증
- Search Console과 서버 access log에서 Googlebot/Naverbot 요청을 개인정보 없이 집계
- crawl budget을 고려해 sitemap 분할·lastmod 정확도·pagination 정책을 운영

파일 후보:

- `docs/runbooks/seo-indexing.md`
- `docs/report/release-readiness.md`
- `deploy/caddy/`
- 관측·로그 설정

검증:

- 새 공개 게시글 URL inspection
- 삭제·비공개 URL의 재크롤링 결과
- 매 배포 smoke에서 robots/sitemap/canonical/OG를 확인

완료:

- SEO 회귀를 사용자가 신고하기 전에 배포 gate와 운영 dashboard에서 발견할 수 있다.

### P7 - 실제 데이터로 SSR 범위를 재평가한다

**목표:** 실제 공개 운영 결과를 근거로 선택적 SSR을 유지할지 전체 frontend 전환을 검토한다.

다음 데이터를 최소 2~4주 수집한다.

- 공개 게시글 색인 성공률과 색인 지연
- 검색 유입 세션·게시글 상세 유입률·이탈률
- 공유 링크 preview 성공률과 공유 후 클릭률
- HTML TTFB, FCP/LCP, hydration 완료시간
- Spring Boot 렌더링 비용, cache hit율, DB read 부하
- 게시글 생성·수정·삭제 후 metadata 반영 지연

전체 SSR framework 도입은 아래 조건이 모두 충분히 확인될 때만 검토한다.

- 선택적 shell/prerender로 해결되지 않는 색인 문제가 재현된다.
- 공개 상세 route의 초기 응답·LCP가 실제 유입을 제한한다.
- SSR runtime 운영 비용과 장애 대응 책임을 수용할 수 있다.
- 인증·CSRF·API contract·배포 구조를 유지하거나 변경할 ADR이 승인됐다.
- SSR 서버가 Node production server 금지라는 현재 TRD를 변경할 정당한 근거가 있다.

## 공통 테스트 전략

### 자동 테스트

- metadata builder unit test: 제목, description truncation, fallback, 민감정보 제거
- visibility matrix integration test: public/hidden/deleted/private/moderated
- controller test: status, canonical, robots, cache header, HTML escaping
- sitemap test: 공개 URL만 포함, 중복·query 제거, stable ordering
- OG contract test: 필수 property와 절대 URL 검증
- JSON-LD test: 화면 값과 구조화 데이터 일치
- frontend route test: title/canonical 업데이트와 공유 fallback
- e2e: 비로그인 공개 게시글 상세, 삭제 게시글, 인기글, 게시판 query URL

### 배포 전 명령 예시

```bash
./gradlew clean check migrationTest
(cd frontend && corepack pnpm typecheck)
(cd frontend && corepack pnpm test)
(cd frontend && corepack pnpm build)
./scripts/seo-smoke.sh https://public-host
```

`seo-smoke.sh`는 최소한 다음을 검사한다.

```text
GET /robots.txt → 200, 의도한 disallow
GET /sitemap.xml → 200, 유효 XML
GET /posts/{id} → 200, title/description/canonical/og:type/og:url/og:image
GET /posts/{deleted-id} → 404 또는 noindex
GET /posts/{id}?tracking=... → canonical에 query 없음
```

## 릴리스 acceptance checklist

- [ ] 공개·비공개·삭제·제재 URL 정책이 문서와 코드에서 일치한다.
- [ ] 공개 게시글 상세 첫 응답에 게시글별 title/description/canonical이 있다.
- [ ] Spring Boot가 별도 shell이 아니라 Vite가 생성한 동일 `dist/index.html`을 사용한다.
- [ ] startup에서 필수 placeholder와 hashed asset 누락을 감지하고 잘못된 artifact를 거부한다.
- [ ] 존재하지 않음·삭제·비공개·권한 없음이 200 soft 404가 아니라 정책에 맞는 404/410으로 처리된다.
- [ ] React 초기 부팅과 SPA 이동이 서버 주입 metadata를 generic 값으로 덮어쓰지 않는다.
- [ ] OG와 Twitter card metadata가 대표 이미지 유무와 관계없이 유효하다.
- [ ] 정확 위치·연락처·비공개 댓글·관리 정보가 metadata와 이미지에 없다.
- [ ] robots.txt와 sitemap.xml이 공개 host에서 200으로 제공된다.
- [ ] sitemap에 로그인·관리자·개인화 URL이 없다.
- [ ] JSON-LD가 화면 표시 값과 일치한다.
- [ ] 카카오·Facebook·LinkedIn·Twitter/X preview 검증을 완료했다.
- [ ] Google Search Console과 네이버 서치어드바이저에서 sitemap을 등록했다.
- [ ] 생성·수정·삭제·제재 후 metadata와 sitemap 반영 정책이 검증됐다.
- [ ] 배포 gate와 운영 runbook이 있다.
- [ ] crawler User-Agent가 authorization 우회에 사용되지 않으며 GET/HEAD·429 정책이 검증됐다.
- [ ] 실제 유입·색인·성능 데이터 없이 전체 SSR 마이그레이션을 확정하지 않았다.

## 예상 커밋 단위

1. `docs: define public seo indexability policy`
2. `feat: add publication seo metadata contract`
3. `feat: add robots sitemap and canonical metadata`
4. `feat: render public publication metadata from vite build artifact`
5. `feat: add publication open graph previews`
6. `feat: add publication structured data`
7. `docs: add search indexing runbook and release gate`

각 커밋은 backend contract, frontend metadata, rendering, image pipeline, 운영 문서를 섞지 않고 독립적으로 검증한다.

## 비범위

- 지금 즉시 SSR framework를 도입하는 작업
- TanStack Start 또는 Next.js로 frontend 전체를 교체하는 작업
- 검색 순위 조작을 위한 keyword stuffing, fake rating, 자동 생성 콘텐츠
- 로그인 전용 피드·북마크·알림의 색인
- 정확한 분실 위치·연락 증거의 공개 metadata 노출
