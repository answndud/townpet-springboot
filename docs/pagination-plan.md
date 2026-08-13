# 게시글 목록 번호형 페이지네이션 설계·구현 기록

## 목적

게시글 목록의 `더 보기` 방식을 제거하고, 현재 보고 있는 위치를 URL로 공유·복원할 수 있는 번호형 페이지 이동으로 전환한다. 모바일 최적화는 이번 범위에서 제외하고 데스크톱 목록의 탐색성과 누락 방지를 우선한다.

## 적용 범위

- `/`의 전체글과 HOT 목록
- `/best`의 독립 인기글 목록
- `/feed`, `/feed/guest`의 게시글 목록
- 동물별 게시판과 공통 게시판 목록
- 검색어, 검색 범위, 게시판, 회원/비회원 범위가 유지되는 페이지 이동

## URL 규칙

기존 route는 변경하지 않고 query parameter만 추가한다.

```text
/?view=all&page=2&q=산책&searchField=TITLE
/boards/adoption?page=3&q=입양
/feed/guest?page=2&q=강아지
```

첫 페이지는 기존 URL과의 호환을 위해 `page=1`을 생략한다. 검색 조건이나 게시판 조건이 바뀌면 페이지를 1로 되돌린다.

## 백엔드 계약

### 일반 피드

기존 cursor 계약을 유지한다.

```json
{
  "items": [],
  "page": {
    "nextCursor": "...",
    "hasNext": true
  }
}
```

일반 피드는 `createdAt DESC, id DESC` 정렬을 사용한다. 서버는 `limit + 1`건을 조회해 다음 페이지 존재 여부를 판단하고, 마지막으로 반환한 항목을 다음 cursor로 만든다.

### HOT 피드

기존 `/api/v1/feed/popular`도 같은 `page` 메타데이터를 반환하도록 확장했다.

```text
GET /api/v1/feed/popular?limit=20&cursor=...&query=...&searchField=TITLE
```

정렬 기준은 다음과 같다.

1. 추천 수 내림차순
2. 작성 시각 내림차순
3. 게시글 ID 내림차순

HOT cursor에는 `v1|추천수|작성시각|게시글ID`가 URL-safe Base64로 들어간다. 추천 수가 바뀌면 순위가 변할 수 있으므로 cursor는 영구적인 snapshot이 아니다. 대신 한 요청 안에서 추천 수·시각·ID의 복합 정렬 경계를 사용해 정상적인 페이지 이동 중 중복과 누락을 줄인다. 향후 완전한 snapshot이 필요해지면 ranking version 또는 집계 시각을 별도 ADR로 결정한다.

## 프론트엔드 동작

`frontend/src/hooks/useCursorPagination.ts`가 필터 조합별로 페이지 데이터와 다음 cursor를 메모리에 캐시한다.

- 1페이지 요청 cursor는 없음
- 2페이지는 1페이지 응답의 `nextCursor` 사용
- 3페이지를 직접 열면 필요한 앞 페이지를 순서대로 요청해 cursor chain 확보
- 같은 검색 조건에서 이전 페이지로 돌아가면 캐시 재사용
- 검색·게시판·범위가 바뀌면 다른 cache key로 분리
- 요청 중 route/filter가 바뀌면 AbortController로 이전 요청 취소

`CursorPagination`은 이전·현재·다음 번호와 이전/다음 버튼을 제공한다. 서버가 전체 페이지 수를 계산하지 않는 cursor 모델이므로 마지막 페이지 번호를 미리 표시하지 않고, 현재 응답의 `hasNext`로 다음 번호 노출 여부를 결정한다.

## 누락·중복 방지 규칙

- 일반 피드와 HOT 모두 정렬 tie-breaker를 포함한다.
- 서버는 `limit + 1`건을 조회해 `hasNext`를 계산한다.
- HOT 페이지의 화면 순위는 URL page와 목록 index로 계산해 2페이지가 다시 1위부터 표시되지 않게 한다.
- 조건이 바뀌면 `page`를 1로 초기화한다.
- 직접 `page=N` 접근 시 앞 페이지 cursor를 확보한 뒤 대상 페이지를 표시한다.
- 결과가 대상 page보다 먼저 끝나면 “요청한 페이지를 찾을 수 없습니다” 상태를 표시한다.

## 구현 순서

1. HOT API에 `cursor`, `limit`, `page.nextCursor`, `page.hasNext` 추가
2. HOT 정렬 경계를 포함하는 versioned cursor 구현
3. 프론트 공통 cursor pagination hook과 번호형 UI 추가
4. 전체글·HOT·`/best`·`/feed`·게시판 목록의 `더 보기` 제거 및 페이지 이동 연결
5. 검색/필터 변경 시 page 초기화와 URL 보존 확인
6. typecheck, frontend test, build budget, backend compile/test 실행

## 검증 기준

- `page=1`에서 첫 페이지가 렌더링된다.
- 다음 버튼이 cursor를 사용해 두 번째 페이지를 요청한다.
- 페이지 이동 후 목록이 누적되지 않고 해당 페이지 항목만 표시된다.
- 검색 조건을 유지한 채 페이지를 이동한다.
- HOT도 일반 피드와 동일하게 번호형 이동을 제공한다.
- 동물/공통 게시판도 동일하게 동작한다.
- `corepack pnpm typecheck`
- `corepack pnpm test -- --run`
- `corepack pnpm build`
- `./gradlew compileJava`
- `./gradlew test --tests com.townpet.publication.PublicationControllerTest`

## 한계와 다음 개선 후보

- 현재는 cursor 기반이라 전체 페이지 수와 마지막 페이지를 미리 알 수 없다.
- HOT 추천 수가 실시간으로 변하면 서로 다른 시점의 페이지 요청 사이에서 순위가 움직일 수 있다.
- 서버가 페이지별 cursor를 서명하지 않으므로 외부에서 변형된 cursor는 형식 검증 후 DB 조건에 사용된다. 공개 운영 단계에서는 HMAC 서명 또는 서버 저장형 cursor를 검토한다.
- 페이지 cache는 브라우저 메모리 수명 동안만 유지된다. 새로고침 후 `page=3`은 cursor chain을 다시 조회한다.
