import { FormEvent, useEffect, useRef, useState } from "react";
import { Link, NavLink, useParams, useSearchParams } from "react-router-dom";
import { ApiError, commonBoardApi, communityApi, type FeedItem } from "../../api/client";
import { ANIMAL_INTEREST_GROUPS } from "../member/AnimalInterestMenu";
import { useAuth } from "../../auth/AuthContext";

export const ANIMAL_BOARD_TABS = [
  ["all", "전체"],
  ["free", "자유"],
  ["questions", "질문·답변"],
  ["showcase", "반려동물 자랑"],
  ["product-reviews", "용품 후기"],
] as const;

export const COMMON_BOARD_TABS = [
  ["all", "전체"],
  ["adoption", "입양"],
  ["lost-found", "분실·목격"],
  ["hospital-reviews", "병원 후기"],
  ["gatherings", "모임"],
  ["marketplace", "거래"],
  ["care", "돌봄"],
  ["volunteer", "봉사"],
] as const;

export const COMMUNITY_BOARDS = ANIMAL_BOARD_TABS;

const TYPE_LABELS: Record<string, string> = {
  FREE_BOARD: "자유",
  QA_QUESTION: "질문·답변",
  PET_SHOWCASE: "반려동물 자랑",
  PRODUCT_REVIEW: "용품 후기",
  ADOPTION: "입양",
  LOST_FOUND: "분실·목격",
  HOSPITAL_REVIEW: "병원 후기",
  GATHERING: "모임",
  MARKETPLACE: "거래",
  CARE_REQUEST: "돌봄",
  VOLUNTEER: "봉사",
};

const ANIMAL_LABELS = new Map(
  ANIMAL_INTEREST_GROUPS.flatMap((group) => group.options.map((option) => [option.code, option.label] as const)),
);

function normalizeAnimalCode(raw: string) {
  const code = raw.toUpperCase();
  return code === "ALL" || ANIMAL_LABELS.has(code) ? code : null;
}

function excerpt(value: string) {
  return value.length > 180 ? `${value.slice(0, 180)}…` : value;
}

function date(value: string) {
  return new Intl.DateTimeFormat("ko-KR", { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" }).format(new Date(value));
}

function writePath(mode: "animal" | "common", animalCode: string, board: string): string | null {
  if (mode === "animal") {
    const params = new URLSearchParams();
    if (animalCode !== "ALL") params.set("animal", animalCode.toLowerCase());
    if (board !== "all" && board !== "free") params.set("board", board);
    const query = params.toString();
    return `/posts/new${query ? `?${query}` : ""}`;
  }
  const paths: Record<string, string | null> = {
    all: null,
    adoption: "/adoptions/new",
    "lost-found": "/lost-found/new",
    "hospital-reviews": "/hospital-reviews",
    gatherings: "/gatherings/new",
    marketplace: "/marketplace/new",
    care: "/care/new",
    volunteer: "/volunteer",
  };
  const base = paths[board] ?? null;
  return base;
}

function FeedCard({ item }: { item: FeedItem }) {
  const href = item.href || `/posts/${item.id}`;
  const authorLabel = item.authorId
    ? "TownPet 회원"
    : item.kind === "PUBLICATION"
      ? "익명 이웃"
      : "TownPet 운영팀";
  return (
    <article className="feed-item">
      <div className="feed-item-chips">
        <span className="publication-chip publication-chip-primary">{TYPE_LABELS[item.type] ?? item.type}</span>
        {item.scope === "LOCAL" ? <span className="publication-chip">내 동네</span> : <span className="publication-chip">전체</span>}
      </div>
      <Link className="feed-item-title" to={href}><h2>{item.title}</h2></Link>
      <p className="feed-item-excerpt">{excerpt(item.body)}</p>
      <div className="feed-item-meta">
        <span>{authorLabel}</span>
        <span aria-hidden="true">·</span>
        <time dateTime={item.createdAt}>{date(item.createdAt)}</time>
      </div>
    </article>
  );
}

export function CommonBoardPage() {
  return <CommunityBoardPage mode="common" />;
}

export default function AnimalCommunityPage() {
  return <CommunityBoardPage mode="animal" />;
}

function CommunityBoardPage({ mode }: { mode: "animal" | "common" }) {
  const { animalCode: rawAnimalCode = "all", boardCode: rawBoardCode = "all" } = useParams();
  const animalCode = mode === "animal" ? normalizeAnimalCode(rawAnimalCode) : "ALL";
  const boardTabs = mode === "animal" ? ANIMAL_BOARD_TABS : COMMON_BOARD_TABS;
  const boardCode = boardTabs.some(([code]) => code === rawBoardCode) ? rawBoardCode : null;
  const { member } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const query = searchParams.get("q") ?? "";
  const scope = member && searchParams.get("scope") === "LOCAL" ? "LOCAL" : "ALL";
  const [searchDraft, setSearchDraft] = useState(query);
  const [items, setItems] = useState<FeedItem[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [retryVersion, setRetryVersion] = useState(0);
  const controllerRef = useRef<AbortController | null>(null);

  useEffect(() => {
    setSearchDraft(query);
  }, [query]);

  useEffect(() => {
    if (!boardCode || (mode === "animal" && !animalCode)) return;
    controllerRef.current?.abort();
    const controller = new AbortController();
    controllerRef.current = controller;
    setLoading(true);
    setError(null);
    const pageRequest =
      mode === "common"
        ? commonBoardApi.feed(boardCode, { query, scope, signal: controller.signal })
        : communityApi.feed((animalCode ?? "ALL").toLowerCase(), boardCode, { query, scope, signal: controller.signal });
    pageRequest
      .then((page) => {
        setItems(page.items);
        setNextCursor(page.page.nextCursor);
        setHasNext(page.page.hasNext);
      })
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        setError(requestError instanceof ApiError && requestError.status === 404 ? "게시판을 찾을 수 없습니다." : "게시글을 불러오지 못했습니다.");
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [animalCode, boardCode, mode, query, retryVersion, scope]);

  const resolvedAnimalCode = animalCode ?? "ALL";
  const animalLabel = resolvedAnimalCode === "ALL" ? "전체 동물 게시판" : `${ANIMAL_LABELS.get(resolvedAnimalCode) ?? resolvedAnimalCode} 게시판`;
  const pageLabel = mode === "common" ? "공통게시판" : animalLabel;
  const boardLabel = boardTabs.find(([code]) => code === boardCode)?.[1] ?? "게시판";
  const writeHref = writePath(mode, animalCode ?? "ALL", boardCode ?? "all");

  async function loadMore() {
    if (!boardCode || !nextCursor || loadingMore || (mode === "animal" && !animalCode)) return;
    setLoadingMore(true);
    try {
      const page =
        mode === "common"
          ? await commonBoardApi.feed(boardCode, { query, scope, cursor: nextCursor })
          : await communityApi.feed((animalCode ?? "ALL").toLowerCase(), boardCode, { query, scope, cursor: nextCursor });
      setItems((current) => [...current, ...page.items]);
      setNextCursor(page.page.nextCursor);
      setHasNext(page.page.hasNext);
    } catch {
      setError("다음 게시글을 불러오지 못했습니다.");
    } finally {
      setLoadingMore(false);
    }
  }

  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const next = searchDraft.trim();
    setSearchParams({ ...(next ? { q: next } : {}), ...(scope === "LOCAL" ? { scope: "LOCAL" } : {}) });
  }

  if ((mode === "animal" && !animalCode) || !boardCode) {
    const fallbackHref = mode === "common" ? "/boards/all" : "/animals/all";
    return <main className="page community-page"><section className="surface-card community-state"><h1>페이지를 찾을 수 없습니다</h1><p>{mode === "common" ? "공통게시판 주소" : "동물 게시판 주소"}를 확인해 주세요.</p><Link className="button button-soft" to={fallbackHref}>{mode === "common" ? "공통게시판으로 이동" : "전체 동물 게시판으로 이동"}</Link></section></main>;
  }

  return (
    <main className="page community-page">
      <header className="community-hero">
        <div>
          <p className="eyebrow">{mode === "common" ? "COMMON BOARDS" : "ANIMAL BOARDS"}</p>
          <h1>{mode === "common" ? pageLabel : animalLabel}</h1>
          <p>{mode === "common" ? "모든 동물 가족이 함께 보는 생활 도메인 게시판입니다." : "동물별로 필요한 질문과 정보를 한곳에서 찾아보세요."}</p>
        </div>
        {writeHref ? <Link className="button button-primary" to={writeHref}>글 작성하기</Link> : <span className="field-help">게시판을 선택해 글을 작성하세요.</span>}
      </header>

      <nav className="community-board-tabs" aria-label={`${pageLabel} 메뉴`}>
        {boardTabs.map(([code, label]) => (
          <NavLink key={code} to={mode === "common" ? `/boards/${code}` : `/animals/${(animalCode ?? "ALL").toLowerCase()}${code === "all" ? "" : `/${code}`}`} className={({ isActive }) => isActive ? "active" : ""} end={code === "all"}>
            {label}
          </NavLink>
        ))}
      </nav>

      <form className="search-panel" onSubmit={submitSearch}>
        <label>{mode === "common" ? "공통게시판에서 검색" : "이 동물 게시판에서 검색"}<input value={searchDraft} onChange={(event) => setSearchDraft(event.target.value)} placeholder="제목이나 내용" /></label>
        <button className="button button-soft" type="submit">검색</button>
      </form>

      {member ? (
        <div className="feed-scope-tabs" role="group" aria-label="게시글 범위">
          <button className={scope === "ALL" ? "market-filter active" : "market-filter"} type="button" aria-pressed={scope === "ALL"} onClick={() => setSearchParams({ ...(query ? { q: query } : {}) })}>전체</button>
          <button className={scope === "LOCAL" ? "market-filter active" : "market-filter"} type="button" aria-pressed={scope === "LOCAL"} onClick={() => setSearchParams({ ...(query ? { q: query } : {}), scope: "LOCAL" })}>내 동네</button>
        </div>
      ) : null}

      {loading ? <section className="surface-card community-state" role="status">게시글을 불러오는 중...</section> : null}
      {!loading && error ? <section className="surface-card community-state"><h2>{error}</h2><button className="button button-soft" type="button" onClick={() => setRetryVersion((current) => current + 1)}>다시 시도</button></section> : null}
      {!loading && !error && !items.length ? <section className="surface-card community-state"><h2>{query ? "검색 결과가 없습니다" : `${boardLabel} 게시판이 비어 있습니다`}</h2><p>{query ? "다른 검색어로 다시 시도해 보세요." : `${mode === "common" ? pageLabel : animalLabel}의 첫 번째 글을 남겨 보세요.`}</p>{writeHref ? <Link className="button button-soft" to={writeHref}>글 작성하기</Link> : null}</section> : null}
      {!loading && !error && items.length ? (
        <section className="surface-card feed-list" aria-label={`${pageLabel} ${boardLabel} 게시글 목록`}>
          {items.map((item) => <FeedCard key={`${item.kind}:${item.id}`} item={item} />)}
          {hasNext ? <div className="feed-load-more"><button className="button button-soft" type="button" onClick={() => void loadMore()} disabled={loadingMore}>{loadingMore ? "불러오는 중..." : "더 보기"}</button></div> : null}
        </section>
      ) : null}
    </main>
  );
}
