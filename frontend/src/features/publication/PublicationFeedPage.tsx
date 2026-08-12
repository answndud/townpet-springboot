import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
  ApiError,
  apiFetch,
  publicationApi,
  type FeedItem,
} from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import { isAbortError } from "../../hooks/useAbortableRequest";
import { ANIMAL_INTEREST_GROUPS } from "../member/AnimalInterestMenu";

type PublicationFeedPageProps = {
  memberView: boolean;
  homeView?: boolean;
};

type PopularFeedItem = {
  id: string;
  title: string;
  body: string;
  createdAt: string;
  recommendationCount?: number;
  rank?: number;
};

type PopularFeedResponse = {
  items?: PopularFeedItem[];
};

const FEED_DATE_FORMATTER = new Intl.DateTimeFormat("ko-KR", {
  month: "long",
  day: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});

function excerpt(body: string) {
  const normalized = body.replace(/\s+/g, " ").trim();
  return normalized.length > 120 ? `${normalized.slice(0, 120)}…` : normalized;
}

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

function PopularFeedList({ items, loading, error }: { items: PopularFeedItem[]; loading: boolean; error: string | null }) {
  return (
    <section className="surface-card feed-list" aria-label="인기글 목록" aria-busy={loading}>
      {error ? <p className="form-error feed-error" role="alert">{error}</p> : null}
      {loading ? <p className="feed-empty" role="status">인기글을 불러오는 중...</p> : null}
      {!loading && !error && items.length === 0 ? <p className="feed-empty">아직 추천을 받은 인기글이 없습니다.</p> : null}
      {!loading && !error
        ? items.map((item, index) => (
            <article className="feed-item best-feed-item" key={item.id}>
              <span className="feed-item-rank" aria-label={`${item.rank ?? index + 1}위`}>{item.rank ?? index + 1}</span>
              <div className="best-feed-copy">
                <Link className="feed-item-title" to={`/posts/${item.id}`}>
                  <h2>{item.title}</h2>
                </Link>
                <p className="feed-item-excerpt">{excerpt(item.body)}</p>
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

export default function PublicationFeedPage({ memberView, homeView = false }: PublicationFeedPageProps) {
  const navigate = useNavigate();
  const { member, status: authStatus } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const query = searchParams.get("q") ?? "";
  const type = searchParams.get("type") ?? "";
  const popularView = homeView && searchParams.get("view") === "popular";
  const scope = memberView && searchParams.get("scope") === "LOCAL" ? "LOCAL" : "ALL";
  const [items, setItems] = useState<FeedItem[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [popularItems, setPopularItems] = useState<PopularFeedItem[]>([]);
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
    apiFetch<PopularFeedResponse>("/api/v1/feed/popular", { signal: controller.signal })
      .then((page) => {
        if (active) setPopularItems(page.items ?? []);
      })
      .catch((requestError: unknown) => {
        if (!active || isAbortError(requestError)) return;
        setPopularError("인기글을 불러오지 못했습니다.");
      })
      .finally(() => {
        if (active) setPopularLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [popularView]);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    if (homeView && popularView) {
      setLoading(false);
      return () => controller.abort();
    }
    setLoading(true);
    setError(null);
    if (memberView && authStatus === "loading") return () => controller.abort();
    if (memberView && authStatus === "anonymous") {
      navigate("/login?next=/feed", { replace: true });
      return () => controller.abort();
    }

    Promise.all([
      Promise.resolve(member),
      publicationApi.feed({
        audience: memberView ? "VIEWER" : "GLOBAL",
        query,
        scope,
        type: type || undefined,
        signal: controller.signal,
      }),
    ])
      .then(([currentMember, page]) => {
        if (!active) return;
        if (memberView && !currentMember) return;
        setItems(page.items);
        setNextCursor(page.page.nextCursor);
        setHasNext(page.page.hasNext);
      })
      .catch((requestError: unknown) => {
        if (!active || (requestError instanceof DOMException && requestError.name === "AbortError")) {
          return;
        }
        if (memberView && requestError instanceof ApiError && requestError.status === 401) {
          navigate("/login?next=/feed", { replace: true });
          return;
        }
        setError("피드를 불러오지 못했습니다.");
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [authStatus, homeView, member, memberView, navigate, popularView, query, scope, type]);

  async function loadMore() {
    if (!nextCursor || loadingMore) return;
    loadMoreController.current?.abort();
    const controller = new AbortController();
    loadMoreController.current = controller;
    setLoadingMore(true);
    setError(null);
    try {
      const page = await publicationApi.feed({
        audience: memberView ? "VIEWER" : "GLOBAL",
        cursor: nextCursor,
        query,
        scope,
        type: type || undefined,
        signal: controller.signal,
      });
      setItems((current) => {
        const existingIds = new Set(current.map((item) => `${item.kind}:${item.id}`));
        return [...current, ...page.items.filter((item) => !existingIds.has(`${item.kind}:${item.id}`))];
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
          <p className="eyebrow">{homeView ? "TOWNPET COMMUNITY" : memberView ? "MY TOWNPET FEED" : "TOWNPET COMMUNITY"}</p>
          <h1>{pageTitle}</h1>
          <p>{pageDescription}</p>
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

      {popularView ? <PopularFeedList items={popularItems} loading={popularLoading} error={popularError} /> : error ? <p className="form-error feed-error" role="alert">{error}</p> : items.length === 0 ? (
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
                    {ANIMAL_INTEREST_GROUPS.flatMap((group) => group.options).find((option) => option.code === item.animalInterestCode)?.label ?? "동물 게시판"}
                  </span>
                ) : null}
                <span className="publication-chip">
                  {item.scope === "LOCAL" ? "내 동네" : "전체"}
                </span>
                </div>
                <Link className="feed-item-title" to={itemHref}>
                  <h2>{item.title}</h2>
                </Link>
                <p className="feed-item-excerpt">{excerpt(item.body)}</p>
                <div className="feed-item-meta">
                  <span>{item.authorId ? "TownPet 회원" : "TownPet 운영팀"}</span>
                  <span aria-hidden="true">·</span>
                  <time dateTime={item.createdAt}>{formatFeedDate(item.createdAt)}</time>
                </div>
              </article>
            );
          })}
          {hasNext ? (
            <div className="feed-load-more">
              <button className="button button-soft" type="button" onClick={loadMore} disabled={loadingMore}>
                {loadingMore ? "불러오는 중..." : "더 보기"}
              </button>
            </div>
          ) : null}
        </section>
      )}
    </main>
  );
}
