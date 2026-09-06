# TownPet SEO 기준선

이 문서는 SEO 개선이 실제로 유효한지 판단하기 위한 실행 기준이다. 검색 노출을 위한 얇은 페이지를 추가하지 않으며, 공개 가치가 있는 콘텐츠만 색인 대상으로 삼는다.

## 기준 환경

- 기준 URL: `https://townpet.cloud`
- 측정 대상: 배포된 HTTP 응답, JavaScript 실행 후 DOM/head, 모바일 viewport
- 비색인 대상: 관리자·인증·개인 영역, 작성/수정 화면, 검색 결과와 필터 조합, 오류·삭제 콘텐츠

## 필수 통과 기준

### 크롤링과 응답

- `GET /robots.txt`는 `200`, `text/plain`이며 sitemap URL을 포함한다.
- `GET /sitemap.xml`은 `200`, XML content type이며 실제 공개 대표 URL만 포함한다.
- sitemap URL은 모두 HTTPS canonical URL이고 query string, 관리자, 검색, 작성 URL이 없다.
- 존재하지 않는 정적 asset은 `404`이며 SPA HTML을 반환하지 않는다.
- route alias는 대표 URL로 redirect되거나 대표 URL을 canonical로 선언한다.
- 공개 정상 페이지, 비공개 페이지, 존재하지 않는 콘텐츠의 HTTP status와 색인 정책이 서로 구분된다.

### 공개 페이지 신호

- 공개 페이지마다 고유한 `<title>`과 50~160자 수준의 설명이 있다.
- `<link rel="canonical">`이 하나만 존재하고 현재 대표 URL과 일치한다.
- Open Graph title, description, url이 title/description/canonical과 일치한다.
- 검색 가치가 있는 상세 페이지는 화면 제목과 핵심 본문을 crawler가 확인할 수 있다.
- 검색 결과·페이지네이션·필터 조합은 대표 URL을 오염시키지 않는다.

### 구조와 품질

- 문서마다 의미 있는 `h1`이 하나 있고 heading 순서가 논리적이다.
- 내부 링크가 색인 대상 공개 페이지를 연결하며 orphan 페이지가 없다.
- 이미지에는 의미 있는 `alt`가 있고, 정확한 위치·개인정보·토큰이 metadata나 구조화 데이터에 없다.
- 구조화 데이터는 실제 화면 내용과 일치할 때만 사용한다.

### 성능과 모바일

- 360px viewport에서 가로 overflow와 핵심 탐색 차단이 없다.
- production build가 성공하고 초기 JS/CSS 예산을 초과하지 않는다.
- LCP, CLS, INP는 측정한 값과 미측정 상태를 구분한다. 실측하지 않은 값을 통과로 간주하지 않는다.

## 검증 명령

```bash
cd frontend && corepack pnpm typecheck
cd frontend && corepack pnpm test
cd frontend && corepack pnpm build
docker compose --env-file deploy/netcup.env.example -f deploy/compose/netcup.yml config
curl -I https://townpet.cloud/robots.txt
curl -I https://townpet.cloud/sitemap.xml
curl -I https://townpet.cloud/no-such-route
curl -I https://townpet.cloud/assets/not-found.js
```

브라우저가 설치된 환경에서는 다음도 실행한다.

```bash
cd frontend && pnpm measure:browser
```

## 판정

- **통과:** 필수 기준을 모두 만족하고 공개 상세 HTML·canonical·metadata가 실제 콘텐츠와 일치한다.
- **조건부:** 고정 공개 페이지는 통과하지만 동적 상세의 초기 HTML 또는 실제 CWV가 아직 검증되지 않았다.
- **실패:** sitemap/robots 오류, 비공개 영역 색인 가능, 공개 상세의 대표 URL 부재, 잘못된 200/404가 하나라도 남아 있다.

## 현재 감사 기준선

2026-09-06 감사 당시 production은 SPA fallback으로 동적 route와 unknown route를 모두 `200` HTML로 반환했고, 초기 HTML에는 root mount와 기본 title만 있었다. 따라서 위 개선이 배포된 뒤 이 문서의 모든 필수 항목을 다시 측정해야 한다.
