import { useEffect } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
  publicationApi,
  type FeedItem,
  type PopularFeedItem,
} from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import CursorPagination from "../../components/CursorPagination";
import { useCursorPagination } from "../../hooks/useCursorPagination";
import { ANIMAL_BOARD_GROUPS } from "../member/AnimalBoardCatalog";

type PublicationFeedPageProps = {
  memberView: boolean;
  homeView?: boolean;
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

const FEED_KIND_LABELS: Record<string, string> = {
  PUBLICATION: "게시글",
  MARKETPLACE: "동네 거래",
  ADOPTION: "입양",
  LOST_FOUND: "분실·목격",
  HOSPITAL_REVIEW: "동물병원 후기",
  GATHERING: "동네 모임",
  CARE_REQUEST: "이웃 돌봄",
  VOLUNTEER: "봉사 기회",
  RESOURCE: "생활 가이드",
};

const FEED_TYPE_LABELS: Record<string, string> = {
  FREE_BOARD: "자유게시판",
  QA_QUESTION: "질문·답변",
  PET_SHOWCASE: "반려동물 자랑",
  PRODUCT_REVIEW: "용품 후기",
  LOCAL_GUIDE: "지역 가이드",
  WELFARE: "복지 안내",
  CARE: "케어 가이드",
};

const FEED_STATUS_LABELS: Record<string, string> = {
  AVAILABLE: "판매 중",
  RESERVED: "예약 중",
  OPEN: "모집 중",
  FULL: "모집 마감",
};

function feedLabel(item: FeedItem) {
  return FEED_TYPE_LABELS[item.type ?? ""] ?? FEED_KIND_LABELS[item.kind ?? ""] ?? "반려생활 소식";
}

function PopularFeedList({ items, loading, error, page, hasNext, totalPages, onPageChange }: { items: PopularFeedItem[]; loading: boolean; error: string | null; page: number; hasNext: boolean; totalPages: number; onPageChange: (page: number) => void }) {
  return (
    <section className="surface-card feed-list" aria-label="인기글 목록" aria-busy={loading}>
      {error ? <p className="form-error feed-error" role="alert">{error}</p> : null}
      {loading ? <p className="feed-empty" role="status">인기글을 불러오는 중...</p> : null}
      {!loading && !error && items.length === 0 ? <p className="feed-empty">아직 추천을 받은 인기글이 없습니다.</p> : null}
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

export default function PublicationFeedPage({ memberView, homeView = false }: PublicationFeedPageProps) {
  const navigate = useNavigate();
  const { member, status: authStatus } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const query = searchParams.get("q") ?? "";
  const type = searchParams.get("type") ?? "";
  const popularView = homeView && searchParams.get("view") === "popular";
  const scope = memberView && searchParams.get("scope") === "LOCAL" ? "LOCAL" : "ALL";
  const page = Math.max(1, Number(searchParams.get("page") ?? "1") || 1);
  const queryKey = `${memberView ? "member" : "guest"}|${query}|${scope}|${type}|${popularView}`;
  const feed = useCursorPagination<FeedItem>({
    enabled: !(homeView && popularView) && (!memberView || authStatus === "authenticated"),
    page,
    pageSize: 20,
    queryKey,
    fetchPage: (cursor, signal) => publicationApi.feed({ audience: memberView ? "VIEWER" : "GLOBAL", cursor, query, scope, type: type || undefined, signal }),
  });
  const popularFeed = useCursorPagination<PopularFeedItem>({
    enabled: homeView && popularView,
    page,
    pageSize: 20,
    queryKey,
    fetchPage: (cursor, signal) => publicationApi.popular({ cursor, query, signal }),
  });
  const items = feed.items;
  const loading = memberView && authStatus !== "authenticated" ? authStatus === "loading" : feed.loading;
  const error = feed.error;
  const popularItems = popularFeed.items;
  const popularLoading = popularFeed.loading;
  const popularError = popularFeed.error;
  const setPage = (nextPage: number) => {
    const next = new URLSearchParams(searchParams);
    if (nextPage <= 1) next.delete("page"); else next.set("page", String(nextPage));
    setSearchParams(next);
  };

  useEffect(() => {
    if (memberView && authStatus === "anonymous") navigate("/login?next=/feed", { replace: true });
  }, [authStatus, memberView, navigate]);

  if (loading) {
    return (
      <main className="page feed-page">
        <section className="surface-card" role="status">피드를 불러오는 중...</section>
      </main>
    );
  }

  const canWrite = member?.role !== "MODERATOR";
  const writeHref = member?.role === "MEMBER" ? "/posts/new" : "/guest/posts/new";
  const pageTitle = homeView ? (popularView ? "인기글" : "전체글") : memberView ? "내 피드" : "전체글";
  const pageDescription = homeView
    ? popularView
      ? "추천 수가 높은 공개 게시글을 모았습니다. 추천 수가 같으면 최신 글이 먼저 보입니다."
      : "TownPet의 모든 게시글을 최신순으로 확인하세요."
    : memberView
      ? `${member?.nickname ?? "회원"}님의 내 피드를 최신순으로 보여드려요.`
      : "로그인 없이 볼 수 있는 게시글을 최신순으로 확인하세요.";

  return (
    <main className="page feed-page">
      <header className="feed-hero">
        <div>
          <p className="feed-location" aria-label="현재 위치">
            <span>{homeView ? "커뮤니티" : "피드"}</span>
            <span aria-hidden="true">/</span>
            <strong>{homeView ? (popularView ? "HOT" : "전체글") : memberView ? "내 피드" : "전체글"}</strong>
          </p>
          <h1>{pageTitle}</h1>
          {!homeView ? <p>{pageDescription}</p> : null}
        </div>
        {canWrite ? <Link className="button button-primary" to={writeHref}>글쓰기</Link> : null}
      </header>

      {!homeView || !popularView ? (
        <form
          className="search-panel"
          onSubmit={(event) => {
            event.preventDefault();
            const value = String(new FormData(event.currentTarget).get("q") ?? "").trim();
            setSearchParams({ ...(value ? { q: value } : {}), ...(type ? { type } : {}), ...(popularView ? { view: "popular" } : {}) });
          }}
        >
          <label>
            <span>게시글 검색</span>
            <input name="q" defaultValue={query} placeholder="제목·내용을 검색해 주세요" />
          </label>
          <button className="button button-soft" type="submit">검색</button>
          {query ? (
            <button className="button button-soft" type="button" onClick={() => setSearchParams(type ? { type } : {})}>
              초기화
            </button>
          ) : null}
        </form>
      ) : null}

      {homeView ? (
        <div className="feed-view-tabs" role="group" aria-label="글 보기 전환">
          <button className={!popularView ? "active" : ""} type="button" aria-pressed={!popularView} onClick={() => setSearchParams({ ...(query ? { q: query } : {}), ...(type ? { type } : {}) })}>전체글</button>
          <button className={popularView ? "active" : ""} type="button" aria-pressed={popularView} onClick={() => setSearchParams({ view: "popular" })}>인기글</button>
        </div>
      ) : memberView ? (
        <nav className="feed-view-tabs" aria-label="피드 보기 전환">
          <Link className="active" to="/feed">내 피드</Link>
          <Link to="/">전체글</Link>
        </nav>
      ) : null}

      {memberView ? (
        <div className="feed-scope-tabs" role="group" aria-label="게시글 범위">
          <button
            className={scope === "ALL" ? "market-filter active" : "market-filter"}
            aria-pressed={scope === "ALL"}
            type="button"
            onClick={() => setSearchParams({ ...(query ? { q: query } : {}), ...(type ? { type } : {}) })}
          >
            전체
          </button>
          <button
            className={scope === "LOCAL" ? "market-filter active" : "market-filter"}
            aria-pressed={scope === "LOCAL"}
            type="button"
            onClick={() => setSearchParams({ ...(query ? { q: query } : {}), ...(type ? { type } : {}), scope: "LOCAL" })}
          >
            내 동네
          </button>
        </div>
      ) : null}

      {popularView ? <PopularFeedList items={popularItems} loading={popularLoading} error={popularError} page={page} hasNext={popularFeed.hasNext} totalPages={popularFeed.totalPages} onPageChange={setPage} /> : error ? <p className="form-error feed-error" role="alert">{error}</p> : items.length === 0 ? (
        <section className="surface-card feed-empty">
          <h2>{query ? "검색 결과가 없습니다" : "아직 표시할 글이 없습니다"}</h2>
          <p>{query ? "다른 검색어로 다시 시도해 보세요." : "첫 번째 반려생활 이야기를 나눠 보세요."}</p>
          {canWrite ? <Link className="button button-soft" to={writeHref}>글 작성하기</Link> : null}
        </section>
      ) : (
        <section className="surface-card feed-list" aria-label="게시글 목록">
          {items.map((item) => {
            const itemHref = item.href || `/posts/${item.id}`;
            const itemType = item.type ?? "";
            const itemStatus = item.status ?? "ACTIVE";
            return (
              <article className="feed-item" key={`${item.kind}:${item.id}`}>
                <div className="feed-item-chips">
                <span className="publication-chip publication-chip-primary">{feedLabel(item)}</span>
                {itemStatus !== "ACTIVE" && !itemType.startsWith("LOCAL_") && itemType !== "WELFARE" && itemType !== "CARE" ? (
                  <span className="publication-chip">{FEED_STATUS_LABELS[itemStatus] ?? itemStatus}</span>
                ) : null}
                {item.animalInterestCode ? (
                  <span className="publication-chip">
                    {ANIMAL_BOARD_GROUPS.flatMap((group) => group.options).find((option) => option.code === item.animalInterestCode)?.label ?? "동물 게시판"}
                  </span>
                ) : null}
                <span className="publication-chip">
                  {item.scope === "LOCAL" ? "내 동네" : "전체"}
                </span>
                </div>
                <Link className="feed-item-title" to={itemHref}>
                  <h2>{item.title}</h2>
                </Link>
                <div className="feed-item-meta">
                  <span>{item.authorId ? "TownPet 회원" : "TownPet 운영팀"}</span>
                  <span aria-hidden="true">·</span>
                  <time dateTime={item.createdAt}>{formatFeedDate(item.createdAt)}</time>
                </div>
              </article>
            );
          })}
          <CursorPagination page={page} hasNext={feed.hasNext} totalPages={feed.totalPages} disabled={feed.loading} onPageChange={setPage} />
        </section>
      )}
    </main>
  );
}
