import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { apiFetch, publicationApi, type Publication } from "./api/client";
import { useAuth } from "./auth/AuthContext";
import { isAbortError } from "./hooks/useAbortableRequest";

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

type PopularResponse = { items?: PopularItem[] };

const FEED_DATE_FORMATTER = new Intl.DateTimeFormat("ko-KR", {
  month: "long",
  day: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});

function formatFeedDate(value: string) {
  return FEED_DATE_FORMATTER.format(new Date(value));
}

function HotFeedList({ items, loading, error }: { items: PopularItem[]; loading: boolean; error: string | null }) {
  return (
    <section className="surface-card feed-list" aria-label="HOT 글 목록" aria-busy={loading}>
      {error ? <p className="form-error feed-error" role="alert">{error}</p> : null}
      {loading ? <p className="feed-empty" role="status">HOT 글을 불러오는 중...</p> : null}
      {!loading && !error && items.length === 0 ? <p className="feed-empty">아직 추천을 받은 HOT 글이 없습니다.</p> : null}
      {!loading && !error
        ? items.map((item, index) => (
            <article className="feed-item best-feed-item" key={item.id}>
              <span className="feed-item-rank" aria-label={`${item.rank ?? index + 1}위`}>{item.rank ?? index + 1}</span>
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
  const [items, setItems] = useState<HomeFeedItem[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [popularItems, setPopularItems] = useState<PopularItem[]>([]);
  const [popularLoading, setPopularLoading] = useState(false);
  const [popularError, setPopularError] = useState<string | null>(null);
  const loadMoreController = useRef<AbortController | null>(null);

  useEffect(() => {
    if (!popularView) {
      setPopularLoading(false);
      setPopularError(null);
      return;
    }

    const controller = new AbortController();
    let active = true;
    setPopularLoading(true);
    setPopularError(null);
    const search = new URLSearchParams();
    if (query) search.set("query", query);
    if (searchField !== "ALL") search.set("searchField", searchField);
    const suffix = search.toString() ? `?${search}` : "";
    apiFetch<PopularResponse>(`/api/v1/feed/popular${suffix}`, { signal: controller.signal })
      .then((page) => { if (active) setPopularItems(page.items ?? []); })
      .catch((requestError: unknown) => {
        if (active && !isAbortError(requestError)) setPopularError("인기글을 불러오지 못했습니다.");
      })
      .finally(() => { if (active) setPopularLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [popularView, query, searchField]);

  useEffect(() => {
    if (popularView) {
      setLoading(false);
      return;
    }

    const controller = new AbortController();
    let active = true;
    setLoading(true);
    setError(null);
    publicationApi.feed({ audience: "GLOBAL", query, searchField, scope: "ALL", signal: controller.signal })
      .then((page) => {
        if (!active) return;
        setItems(page.items);
        setNextCursor(page.page.nextCursor);
        setHasNext(page.page.hasNext);
      })
      .catch((requestError: unknown) => {
        if (active && !isAbortError(requestError)) setError("전체글을 불러오지 못했습니다.");
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [popularView, query, searchField]);

  async function loadMore() {
    if (!nextCursor || loadingMore || popularView) return;
    loadMoreController.current?.abort();
    const controller = new AbortController();
    loadMoreController.current = controller;
    setLoadingMore(true);
    setError(null);
    try {
      const page = await publicationApi.feed({ audience: "GLOBAL", cursor: nextCursor, query, searchField, scope: "ALL", signal: controller.signal });
      setItems((current) => {
        const existingIds = new Set(current.map((item) => item.id));
        return [...current, ...page.items.filter((item) => !existingIds.has(item.id))];
      });
      setNextCursor(page.page.nextCursor);
      setHasNext(page.page.hasNext);
    } catch (requestError) {
      if (!isAbortError(requestError)) setError("다음 글을 불러오지 못했습니다.");
    } finally {
      if (loadMoreController.current === controller) setLoadingMore(false);
    }
  }

  useEffect(() => () => loadMoreController.current?.abort(), []);

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

      {popularView ? <HotFeedList items={popularItems} loading={popularLoading} error={popularError} /> : error ? <p className="form-error feed-error" role="alert">{error}</p> : loading ? (
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
          {hasNext ? <div className="feed-load-more"><button className="button button-soft" type="button" onClick={loadMore} disabled={loadingMore}>{loadingMore ? "불러오는 중..." : "더 보기"}</button></div> : null}
        </section>
      )}
    </main>
  );
}
