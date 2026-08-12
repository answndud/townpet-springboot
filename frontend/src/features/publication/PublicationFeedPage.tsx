import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
  ApiError,
  publicationApi,
  type Publication,
} from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import { isAbortError } from "../../hooks/useAbortableRequest";

type PublicationFeedPageProps = {
  memberView: boolean;
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

export default function PublicationFeedPage({ memberView }: PublicationFeedPageProps) {
  const navigate = useNavigate();
  const { member, status: authStatus } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const query = searchParams.get("q") ?? "";
  const scope = memberView && searchParams.get("scope") === "LOCAL" ? "LOCAL" : "ALL";
  const [items, setItems] = useState<Publication[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const loadMoreController = useRef<AbortController | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
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
  }, [authStatus, member, memberView, navigate, query, scope]);

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
        signal: controller.signal,
      });
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

  if (loading) {
    return (
      <main className="page feed-page">
        <section className="surface-card" role="status">피드를 불러오는 중...</section>
      </main>
    );
  }

  const canWrite = member?.role !== "MODERATOR";

  return (
    <main className="page feed-page">
      <header className="feed-hero">
        <div>
          <p className="eyebrow">{memberView ? "MY TOWNPET FEED" : "PUBLIC FEED"}</p>
          <h1>{memberView ? "내 동네와 전체 새 글" : "공개 반려생활 피드"}</h1>
          <p>
            {memberView
              ? `${member?.nickname ?? "회원"}님의 대표 동네와 전체 공개 글을 최신순으로 보여드려요.`
              : "로그인 없이 볼 수 있는 전체 공개 글을 최신순으로 확인하세요."}
          </p>
        </div>
        {canWrite ? <Link className="button button-primary" to={memberView ? "/posts/new" : "/guest/posts/new"}>글쓰기</Link> : null}
      </header>

      <form
        className="search-panel"
        onSubmit={(event) => {
          event.preventDefault();
          const value = String(new FormData(event.currentTarget).get("q") ?? "").trim();
          setSearchParams(value ? { q: value } : {});
        }}
      >
        <label>
          <span>게시글 검색</span>
          <input name="q" defaultValue={query} placeholder="제목·내용을 검색해 주세요" />
        </label>
        <button className="button button-soft" type="submit">검색</button>
        {query ? (
          <button className="button button-soft" type="button" onClick={() => setSearchParams({})}>
            초기화
          </button>
        ) : null}
      </form>

      <nav className="feed-view-tabs" aria-label="피드 보기 전환">
        <Link className={memberView ? "active" : ""} to="/feed">내 피드</Link>
        <Link className={!memberView ? "active" : ""} to="/feed/guest">전체 공개</Link>
      </nav>

      {memberView ? (
        <div className="feed-scope-tabs" role="group" aria-label="게시글 범위">
          <button
            className={scope === "ALL" ? "market-filter active" : "market-filter"}
            aria-pressed={scope === "ALL"}
            type="button"
            onClick={() => setSearchParams(query ? { q: query } : {})}
          >
            전체
          </button>
          <button
            className={scope === "LOCAL" ? "market-filter active" : "market-filter"}
            aria-pressed={scope === "LOCAL"}
            type="button"
            onClick={() => setSearchParams(query ? { q: query, scope: "LOCAL" } : { scope: "LOCAL" })}
          >
            내 동네
          </button>
        </div>
      ) : null}

      {error ? <p className="form-error feed-error" role="alert">{error}</p> : null}
      {items.length === 0 ? (
        <section className="surface-card feed-empty">
          <h2>{query ? "검색 결과가 없습니다" : "아직 표시할 글이 없습니다"}</h2>
          <p>{query ? "다른 검색어로 다시 시도해 보세요." : "첫 번째 반려생활 이야기를 나눠 보세요."}</p>
          {canWrite ? <Link className="button button-soft" to={memberView ? "/posts/new" : "/guest/posts/new"}>글 작성하기</Link> : null}
        </section>
      ) : (
        <section className="surface-card feed-list" aria-label="게시글 목록">
          {items.map((publication) => (
            <article className="feed-item" key={publication.id}>
              <div className="feed-item-chips">
                <span className="publication-chip publication-chip-primary">자유게시판</span>
                <span className="publication-chip">
                  {publication.scope === "LOCAL" ? "내 동네" : "전체"}
                </span>
              </div>
              <Link className="feed-item-title" to={`/posts/${publication.id}`}>
                <h2>{publication.title}</h2>
              </Link>
              <p className="feed-item-excerpt">{excerpt(publication.body)}</p>
              <div className="feed-item-meta">
                <span>TownPet 회원</span>
                <span aria-hidden="true">·</span>
                <time dateTime={publication.createdAt}>{formatFeedDate(publication.createdAt)}</time>
              </div>
            </article>
          ))}
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
