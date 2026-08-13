import { Link, useSearchParams } from "react-router-dom";
import { publicationApi, type Publication } from "./api/client";
import { useAuth } from "./auth/AuthContext";
import CursorPagination from "./components/CursorPagination";
import { useCursorPagination } from "./hooks/useCursorPagination";

type PopularItem = Pick<Publication, "id" | "title" | "body" | "createdAt"> & {
  recommendationCount?: number;
  rank?: number;
};

type HomeFeedItem = {
  id: string;
  title: string;
  body: string;
  scope: string;
  createdAt: string;
};

const FEED_DATE_FORMATTER = new Intl.DateTimeFormat("ko-KR", {
  month: "long",
  day: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});

function formatFeedDate(value: string) {
  return FEED_DATE_FORMATTER.format(new Date(value));
}

function HotFeedList({ items, loading, error, page, hasNext, totalPages, onPageChange }: { items: PopularItem[]; loading: boolean; error: string | null; page: number; hasNext: boolean; totalPages: number; onPageChange: (page: number) => void }) {
  return (
    <section className="surface-card feed-list" aria-label="HOT 글 목록" aria-busy={loading}>
      {error ? <p className="form-error feed-error" role="alert">{error}</p> : null}
      {loading ? <p className="feed-empty" role="status">HOT 글을 불러오는 중...</p> : null}
      {!loading && !error && items.length === 0 ? <p className="feed-empty">아직 추천을 받은 HOT 글이 없습니다.</p> : null}
      {!loading && !error
        ? items.map((item, index) => (
            <article className="feed-item best-feed-item" key={item.id}>
              <span className="feed-item-rank" aria-label={`${(page - 1) * 20 + index + 1}위`}>{(page - 1) * 20 + index + 1}</span>
              <div className="best-feed-copy">
                <Link className="feed-item-title" to={`/posts/${item.id}`}>
                  <h2>{item.title}</h2>
                </Link>
                <div className="feed-item-meta">
                  {item.recommendationCount !== undefined ? <small>추천 {item.recommendationCount}</small> : null}
                  <time dateTime={item.createdAt}>{formatFeedDate(item.createdAt)}</time>
                </div>
              </div>
            </article>
          ))
        : null}
      {!loading && !error && items.length > 0 ? <CursorPagination page={page} hasNext={hasNext} totalPages={totalPages} disabled={loading} onPageChange={onPageChange} /> : null}
    </section>
  );
}

export default function HomeFeedPage() {
  const { member } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const query = searchParams.get("q") ?? "";
  const searchFieldParam = searchParams.get("searchField");
  const searchField = searchFieldParam === "TITLE" || searchFieldParam === "BODY" ? searchFieldParam : "ALL";
  const popularView = searchParams.get("view") !== "all";
  const page = Math.max(1, Number(searchParams.get("page") ?? "1") || 1);
  const queryKey = `${popularView ? "popular" : "all"}|${query}|${searchField}`;
  const allFeed = useCursorPagination<HomeFeedItem>({
    enabled: !popularView,
    page,
    pageSize: 20,
    queryKey,
    fetchPage: (cursor, signal) => publicationApi.feed({ audience: "GLOBAL", cursor, query, searchField, scope: "ALL", signal }) as Promise<{ items: HomeFeedItem[]; page: { nextCursor: string | null; hasNext: boolean } }>,
  });
  const popularFeed = useCursorPagination<PopularItem>({
    enabled: popularView,
    page,
    pageSize: 20,
    queryKey,
    fetchPage: (cursor, signal) => publicationApi.popular({ cursor, query, searchField, signal }),
  });
  const items = allFeed.items;
  const loading = allFeed.loading;
  const error = allFeed.error;
  const popularItems = popularFeed.items;
  const popularLoading = popularFeed.loading;
  const popularError = popularFeed.error;
  const setPage = (nextPage: number) => {
    const next = new URLSearchParams(searchParams);
    if (nextPage <= 1) next.delete("page"); else next.set("page", String(nextPage));
    setSearchParams(next);
  };

  const canWrite = member?.role !== "MODERATOR";
  const writeHref = member?.role === "MEMBER" ? "/posts/new" : "/guest/posts/new";

  return (
    <main className="page feed-page">
      <header className="feed-hero">
        <div>
          <h1>{popularView ? "HOT 글" : "전체글"}</h1>
        </div>
        {canWrite ? <Link className="button button-primary" to={writeHref}>글쓰기</Link> : null}
      </header>

      <div className="feed-toolbar">
        <div className="feed-view-tabs" role="group" aria-label="글 보기 전환">
        <span className="feed-toolbar-label">피드</span>
        <button className={!popularView ? "active" : ""} aria-pressed={!popularView} type="button" onClick={() => setSearchParams({ view: "all", ...(query ? { q: query } : {}), ...(searchField !== "ALL" ? { searchField } : {}) })}>전체글</button>
        <button className={popularView ? "active" : ""} aria-pressed={popularView} type="button" onClick={() => setSearchParams({ view: "popular", ...(query ? { q: query } : {}), ...(searchField !== "ALL" ? { searchField } : {}) })}>HOT</button>
      </div>

        <form
          aria-label="피드 게시글 검색"
          className="search-panel feed-search-inline"
          onSubmit={(event) => {
            event.preventDefault();
            const form = new FormData(event.currentTarget);
            const value = String(form.get("q") ?? "").trim();
            const field = String(form.get("searchField") ?? "ALL");
            setSearchParams({ view: popularView ? "popular" : "all", ...(value ? { q: value } : {}), ...(field !== "ALL" ? { searchField: field } : {}) });
          }}
        >
          <label>
            <select aria-label="검색 위치" name="searchField" defaultValue={searchField}>
              <option value="ALL">제목+내용</option>
              <option value="TITLE">제목</option>
              <option value="BODY">내용</option>
            </select>
          </label>
          <label>
            <input aria-label="검색어" name="q" defaultValue={query} placeholder="검색어 입력" />
          </label>
          <button className="button button-soft" type="submit">검색</button>
          {query ? <button className="button button-soft" type="button" onClick={() => setSearchParams({ view: popularView ? "popular" : "all" })}>초기화</button> : null}
        </form>
      </div>

      {popularView ? <HotFeedList items={popularItems} loading={popularLoading} error={popularError} page={page} hasNext={popularFeed.hasNext} totalPages={popularFeed.totalPages} onPageChange={setPage} /> : error ? <p className="form-error feed-error" role="alert">{error}</p> : loading ? (
        <section className="surface-card feed-list" aria-busy="true" aria-label="전체글 목록">
          <p className="feed-empty" role="status">전체글을 불러오는 중...</p>
        </section>
      ) : items.length === 0 ? (
        <section className="surface-card feed-empty">
          <h2>{query ? "검색 결과가 없습니다" : "아직 표시할 글이 없습니다"}</h2>
          <p>{query ? "다른 검색어로 다시 시도해 보세요." : "첫 번째 반려생활 이야기를 나눠 보세요."}</p>
          {canWrite ? <Link className="button button-soft" to={writeHref}>글 작성하기</Link> : null}
        </section>
      ) : (
        <section className="surface-card feed-list" aria-label="게시글 목록">
          {items.map((item) => (
            <article className="feed-item" key={item.id}>
              <div className="feed-item-chips">
                <span className="publication-chip publication-chip-primary">자유게시판</span>
                <span className="publication-chip">{item.scope === "LOCAL" ? "내 동네" : "전체"}</span>
              </div>
              <Link className="feed-item-title" to={`/posts/${item.id}`}><h2>{item.title}</h2></Link>
              <div className="feed-item-meta">
                <span>TownPet 회원</span>
                <span aria-hidden="true">·</span>
                <time dateTime={item.createdAt}>{formatFeedDate(item.createdAt)}</time>
              </div>
            </article>
          ))}
          <CursorPagination page={page} hasNext={allFeed.hasNext} totalPages={allFeed.totalPages} disabled={allFeed.loading} onPageChange={setPage} />
        </section>
      )}
    </main>
  );
}
