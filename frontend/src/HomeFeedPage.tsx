import { ANIMAL_BOARD_OPTIONS } from "./features/member/AnimalBoardCatalog";
import { Link, useSearchParams } from "react-router-dom";
import { publicationApi, type FeedItem, type Publication } from "./api/client";
import { useAuth } from "./auth/AuthContext";
import CursorPagination from "./components/CursorPagination";
import { useCursorPagination } from "./hooks/useCursorPagination";

type PopularItem = Pick<Publication, "id" | "title" | "body" | "createdAt"> & {
  recommendationCount?: number;
  rank?: number;
};

const HOME_FEED_TYPE_LABELS: Record<string, string> = {
  FREE_BOARD: "자유게시판",
  QA_QUESTION: "질문·답변",
  PET_SHOWCASE: "반려동물 자랑",
  PRODUCT_REVIEW: "용품 후기",
  MARKETPLACE: "동네 거래",
  ADOPTION: "입양",
  LOST_FOUND: "분실·목격",
  HOSPITAL_REVIEW: "동물병원 후기",
  GATHERING: "동네 모임",
  CARE_REQUEST: "이웃 돌봄",
  VOLUNTEER: "봉사 기회",
  LOCAL_GUIDE: "지역 가이드",
  WELFARE: "복지 안내",
  CARE: "케어 가이드",
};

function homeFeedLabel(item: FeedItem) {
  return HOME_FEED_TYPE_LABELS[item.type] ?? (item.kind === "PUBLICATION" ? "게시글" : "반려생활 소식");
}

const FEED_DATE_FORMATTER = new Intl.DateTimeFormat("ko-KR", {
  month: "long",
  day: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});

function formatFeedDate(value: string) {
  return FEED_DATE_FORMATTER.format(new Date(value));
}

function HotFeedList({ items, loading, error, page, hasNext, onPageChange }: { items: PopularItem[]; loading: boolean; error: string | null; page: number; hasNext: boolean; onPageChange: (page: number) => void }) {
  return (
    <section className="surface-card feed-list" aria-label="HOT 글 목록" aria-busy={loading}>
      {error ? <p className="form-error feed-error" role="alert">{error}</p> : null}
      {loading ? <p className="feed-empty" role="status">HOT 글을 불러오는 중...</p> : null}
      {!loading && !error && items.length === 0 ? <p className="feed-empty">HOT 글 없음</p> : null}
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
      {!loading && !error && items.length > 0 ? <CursorPagination page={page} hasPrevious={page > 1} hasNext={hasNext} disabled={loading} onPageChange={onPageChange} /> : null}
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
  const allFeed = useCursorPagination<FeedItem>({
    enabled: !popularView,
    page,
    pageSize: 20,
    queryKey,
    fetchPage: (cursor, signal) => publicationApi.feed({ cursor, query, searchField, signal }),
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
    <main className="page feed-page home-page">
      <div className="home-layout"><section className="home-feed">
      <header className="feed-hero">
        <div>
          <p className="feed-location" aria-label="현재 위치">
            <span>커뮤니티</span>
            <span aria-hidden="true">/</span>
            <strong>{popularView ? "HOT" : "전체글"}</strong>
          </p>
          <h1>{popularView ? "HOT 글" : "전체글"}</h1>
        </div>
        {canWrite ? <Link className="button button-write" to={writeHref}><span className="button-write-icon" aria-hidden="true">＋</span><span>글쓰기</span></Link> : null}
      </header>

      <nav className="community-board-tabs feed-home-tabs" aria-label="피드 보기 전환">

        <button className={!popularView ? "active" : ""} aria-current={!popularView ? "page" : undefined} type="button" onClick={() => setSearchParams({ view: "all", ...(query ? { q: query } : {}), ...(searchField !== "ALL" ? { searchField } : {}) })}>전체</button>
        <button className={popularView ? "active" : ""} aria-current={popularView ? "page" : undefined} type="button" onClick={() => setSearchParams({ view: "popular", ...(query ? { q: query } : {}), ...(searchField !== "ALL" ? { searchField } : {}) })}>HOT</button>
      </nav>

      <form aria-label="피드 게시글 검색" className="search-panel community-bottom-search feed-bottom-search" onSubmit={(event) => {
        event.preventDefault();
        const form = new FormData(event.currentTarget);
        const value = String(form.get("q") ?? "").trim();
        const field = String(form.get("searchField") ?? "ALL");
        setSearchParams({ view: popularView ? "popular" : "all", ...(value ? { q: value } : {}), ...(field !== "ALL" ? { searchField: field } : {}) });
      }}>
        <label><span className="search-label">검색 위치</span><select aria-label="검색 위치" name="searchField" defaultValue={searchField}><option value="ALL">제목+내용</option><option value="TITLE">제목</option><option value="BODY">내용</option></select></label>
        <label><span className="search-label">피드 게시글 검색</span><input aria-label="검색어" name="q" defaultValue={query} /></label>
        <button className="button button-soft" type="submit">검색</button>
      </form>

      {popularView ? <HotFeedList items={popularItems} loading={popularLoading} error={popularError} page={page} hasNext={popularFeed.hasNext} onPageChange={setPage} /> : error ? <p className="form-error feed-error" role="alert">{error}</p> : loading ? (
        <section className="surface-card feed-list" aria-busy="true" aria-label="전체글 목록">
          <p className="feed-empty" role="status">전체글을 불러오는 중...</p>
        </section>
      ) : items.length === 0 ? (
        <section className="surface-card feed-empty">
          <h2>{query ? "검색 결과 없음" : "게시글 없음"}</h2>
          {canWrite ? <Link className="button button-write" to={writeHref}><span className="button-write-icon" aria-hidden="true">＋</span><span>글쓰기</span></Link> : null}
        </section>
      ) : (
        <section className="surface-card feed-list" aria-label="게시글 목록">
          {items.map((item) => (
            <article className="feed-item" key={item.id}>
              <div className="feed-item-chips">
                <span className="publication-chip publication-chip-primary">{homeFeedLabel(item)}</span>
              </div>
              <Link className="feed-item-title" to={item.href || `/posts/${item.id}`}><h2>{item.title}</h2></Link>
              <div className="feed-item-meta">
                <span>TownPet 회원</span>
                <span aria-hidden="true">·</span>
                <time dateTime={item.createdAt}>{formatFeedDate(item.createdAt)}</time>
              </div>
            </article>
          ))}
          <CursorPagination page={page} hasPrevious={page > 1} hasNext={allFeed.hasNext} disabled={allFeed.loading} onPageChange={setPage} />
        </section>
      )}


      </section><aside className="home-sidebar">
        <section className="sidebar-section"><p className="eyebrow">동물별 게시판</p><nav className="animal-discovery" aria-label="동물별 이야기">{ANIMAL_BOARD_OPTIONS.map(option => <Link key={option.code} to={`/animals/${option.code.toLowerCase()}`}>{option.label}<span aria-hidden="true">↗</span></Link>)}</nav></section>
        <section className="sidebar-guide"><h2>지역 가이드</h2><Link to="/guides">지역 가이드 보기 →</Link></section>
      </aside></div>
    </main>
  );
}
