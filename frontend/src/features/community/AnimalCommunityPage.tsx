import { FormEvent, useEffect, useState } from "react";
import { Link, NavLink, Navigate, useLocation, useParams, useSearchParams } from "react-router-dom";
import { commonBoardApi, communityApi, type FeedItem } from "../../api/client";
import { ANIMAL_BOARD_GROUPS } from "../member/AnimalBoardCatalog";
import CursorPagination from "../../components/CursorPagination";
import { useCursorPagination } from "../../hooks/useCursorPagination";

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
  ANIMAL_BOARD_GROUPS.flatMap((group) => group.options.map((option) => [option.code, option.label] as const)),
);

function normalizeAnimalCode(raw: string) {
  const code = raw.toUpperCase();
  return code === "ALL" || ANIMAL_LABELS.has(code) ? code : null;
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

function FeedCard({ item, popularView }: { item: FeedItem; popularView: boolean }) {
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
      </div>
      <Link className="feed-item-title" to={href}><h2>{item.title}</h2></Link>
      <div className="feed-item-meta">
        <span>{authorLabel}</span>
        <span aria-hidden="true">·</span>
        {popularView && item.recommendationCount != null ? <><span>추천 {item.recommendationCount}</span><span aria-hidden="true">·</span></> : null}
        <time dateTime={item.createdAt}>{date(item.createdAt)}</time>
      </div>
    </article>
  );
}

export function CommonBoardPage() {
  const { boardCode } = useParams();
  const location = useLocation();
  if (boardCode === "marketplace") return <Navigate to={`/marketplace${location.search}`} replace />;
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
  const [searchParams, setSearchParams] = useSearchParams();
  const query = searchParams.get("q") ?? "";
  const searchFieldParam = searchParams.get("searchField");
  const searchField = searchFieldParam === "TITLE" || searchFieldParam === "BODY" ? searchFieldParam : "ALL";
  const popularView = mode === "animal" && searchParams.get("view") === "popular";
  const [searchDraft, setSearchDraft] = useState(query);
  const [retryVersion, setRetryVersion] = useState(0);

  useEffect(() => {
    setSearchDraft(query);
  }, [query]);

  const page = Math.max(1, Number(searchParams.get("page") ?? "1") || 1);
  const queryKey = `${mode}|${animalCode}|${boardCode}|${query}|${searchField}|${popularView ? "popular" : "latest"}|${retryVersion}`;
  const feed = useCursorPagination<FeedItem>({
    enabled: Boolean(boardCode && (mode === "common" || animalCode)),
    page,
    pageSize: 20,
    queryKey,
    fetchPage: (cursor, signal) => mode === "common"
      ? commonBoardApi.feed(boardCode ?? "all", { query, searchField, cursor, signal })
      : communityApi.feed((animalCode ?? "ALL").toLowerCase(), boardCode ?? "all", { query, searchField, cursor, signal, sort: popularView ? "POPULAR" : "LATEST" }),
  });
  const items = feed.items;
  const loading = feed.loading;
  const error = feed.error;
  const setPage = (nextPage: number) => {
    const next = new URLSearchParams(searchParams);
    if (nextPage <= 1) next.delete("page"); else next.set("page", String(nextPage));
    setSearchParams(next);
  };

  const resolvedAnimalCode = animalCode ?? "ALL";
  const animalLabel = resolvedAnimalCode === "ALL" ? "전체 동물 게시판" : `${ANIMAL_LABELS.get(resolvedAnimalCode) ?? resolvedAnimalCode} 게시판`;
  const boardLabel = boardTabs.find(([code]) => code === boardCode)?.[1] ?? "게시판";
  const pageLabel = mode === "common"
    ? boardCode === "all" ? "공통게시판" : `${boardLabel} 게시판`
    : animalLabel;
  const writeHref = writePath(mode, animalCode ?? "ALL", boardCode ?? "all");

  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const next = String(form.get("q") ?? "").trim();
    const field = String(form.get("searchField") ?? "ALL");
    setSearchParams({
      ...(popularView ? { view: "popular" } : {}),
      ...(next ? { q: next } : {}),
      ...(field !== "ALL" ? { searchField: field } : {}),
    });
  }

  function togglePopular() {
    const next = new URLSearchParams(searchParams);
    next.delete("page");
    if (popularView) next.delete("view");
    else next.set("view", "popular");
    setSearchParams(next);
  }

  if ((mode === "animal" && !animalCode) || !boardCode) {
    const fallbackHref = mode === "common" ? "/boards/all" : "/animals/all";
    return <main className="page community-page"><section className="surface-card community-state"><h1>페이지를 찾을 수 없습니다</h1><p>{mode === "common" ? "공통게시판" : "동물 게시판"} 주소 오류</p><Link className="button button-soft" to={fallbackHref}>{mode === "common" ? "공통게시판" : "전체 동물 게시판"}</Link></section></main>;
  }

  return (
    <main className="page community-page">
      <header className="community-hero">
        <div>
          <p className="eyebrow">{mode === "common" ? "공통 게시판" : "동물별 게시판"}</p>
          <h1>{pageLabel}</h1>
        </div>
        {writeHref ? <Link className="button button-write" to={writeHref}><span className="button-write-icon" aria-hidden="true">＋</span><span>글쓰기</span></Link> : null}
      </header>

      {mode === "common" && boardCode !== "all" && ["marketplace", "lost-found", "gatherings", "care", "hospital-reviews", "volunteer"].includes(boardCode) ? <Link className="board-domain-link" to={`/${boardCode}`}>{boardLabel} 상세 조건으로 둘러보기 <span aria-hidden="true">→</span></Link> : null}
      <nav className="community-board-tabs" aria-label={`${pageLabel} 메뉴`}>
        {boardTabs.map(([code, label]) => (
          <NavLink key={code} to={mode === "common" ? `/boards/${code}` : `/animals/${(animalCode ?? "ALL").toLowerCase()}${code === "all" ? "" : `/${code}`}${popularView ? "?view=popular" : ""}`} className={({ isActive }) => isActive ? "active" : ""} end={code === "all"}>
            {label}
          </NavLink>
        ))}
      </nav>

      <div className="marketplace-toolbar">
        {mode === "animal" ? <button className={`market-filter${popularView ? " active" : ""}`} type="button" aria-pressed={popularView} onClick={togglePopular}>인기글</button> : <span />}
        <form className="search-panel community-bottom-search" onSubmit={submitSearch}>
        <label><span className="search-label">검색 위치</span><select aria-label="검색 위치" name="searchField" defaultValue={searchField}><option value="ALL">제목+내용</option><option value="TITLE">제목</option><option value="BODY">내용</option></select></label>
        <label><span className="search-label">{mode === "common" ? "공통게시판에서 검색" : "이 동물 게시판에서 검색"}</span><input aria-label={mode === "common" ? "공통게시판에서 검색" : "이 동물 게시판에서 검색"} name="q" value={searchDraft} onChange={(event) => setSearchDraft(event.target.value)} /></label>
        <button className="button button-soft" type="submit">검색</button>
        </form>
      </div>
      {loading ? <section className="surface-card community-state" role="status">게시글을 불러오는 중...</section> : null}
      {!loading && error ? <section className="surface-card community-state"><h2>{error}</h2><button className="button button-soft" type="button" onClick={() => setRetryVersion((current) => current + 1)}>다시 시도</button></section> : null}
      {!loading && !error && !items.length ? <section className="surface-card community-state"><h2>{query ? "검색 결과 없음" : popularView ? "인기글 없음" : `${boardLabel} 게시글 없음`}</h2>{writeHref ? <Link className="button button-write" to={writeHref}><span className="button-write-icon" aria-hidden="true">＋</span><span>글쓰기</span></Link> : null}</section> : null}
      {!loading && !error && items.length ? (
        <section className="surface-card feed-list" aria-label={`${pageLabel} ${boardLabel} 게시글 목록`}>
          {items.map((item) => <FeedCard key={`${item.kind}:${item.id}`} item={item} popularView={popularView} />)}
          <CursorPagination page={page} hasPrevious={page > 1} hasNext={feed.hasNext} disabled={feed.loading} onPageChange={setPage} />
        </section>
      ) : null}


    </main>
  );
}
