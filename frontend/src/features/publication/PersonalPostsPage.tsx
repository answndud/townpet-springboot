import { useEffect, useMemo } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ApiError, memberApi, publicationApi, type Publication } from "../../api/client";
import { useAbortableRequest } from "../../hooks/useAbortableRequest";
import { formatDateTime } from "../../utils/date";

export default function PersonalPostsPage({ saved = false }: { saved?: boolean }) {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const query = searchParams.get("q") ?? "";
  const searchFieldParam = searchParams.get("searchField");
  const searchField = searchFieldParam === "TITLE" || searchFieldParam === "BODY" ? searchFieldParam : "ALL";
  const { data: items, error: requestError, loading } = useAbortableRequest<Publication[]>((signal) => saved ? memberApi.bookmarks(signal).then((ids) => Promise.all(ids.map((id) => publicationApi.detail(id, signal)))) : memberApi.myPosts(signal), [saved]);
  const error = requestError instanceof ApiError && requestError.status === 401 ? "로그인이 필요합니다." : requestError ? "게시글을 불러오지 못했습니다." : null;
  useEffect(() => {
    if (requestError instanceof ApiError && requestError.status === 401) navigate(`/login?next=${saved ? "/saved" : "/my-posts"}`, { replace: true });
  }, [navigate, requestError, saved]);
  const filteredItems = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase();
    if (!normalizedQuery) return items ?? [];
    return (items ?? []).filter((item) => {
      const value = searchField === "TITLE" ? item.title : searchField === "BODY" ? item.body : `${item.title} ${item.body}`;
      return value.toLocaleLowerCase().includes(normalizedQuery);
    });
  }, [items, query, searchField]);
  return (
    <main className="page feed-page">
      <header className="feed-hero">
        <div>
          <p className="feed-location" aria-label="현재 위치">
            <span>프로필</span>
            <span aria-hidden="true">/</span>
            <strong>{saved ? "북마크" : "내 작성글"}</strong>
          </p>
          <h1>{saved ? "저장한 글" : "내가 쓴 글"}</h1>
          <p>{saved ? "나중에 다시 보고 싶은 반려생활 정보를 모아 두었습니다." : "내가 작성한 글과 기록을 확인하세요."}</p>
        </div>
        <Link className="button button-write" to="/posts/new"><span className="button-write-icon" aria-hidden="true">＋</span><span>글쓰기</span></Link>
      </header>
      {error ? <p role="alert">{error}</p> : null}
      {loading ? <p className="surface-card" role="status">게시글을 불러오는 중...</p> : null}
      {!loading && !filteredItems.length && !error ? (
        <section className="surface-card feed-empty">
          <h2>{query ? "조건에 맞는 게시글이 없습니다" : "아직 게시글이 없습니다"}</h2>
          <p>{query ? "다른 검색어로 다시 시도해 보세요." : "첫 번째 반려생활 이야기를 나눠 보세요."}</p>
          <Link className="button button-write" to="/posts/new"><span className="button-write-icon" aria-hidden="true">＋</span><span>글쓰기</span></Link>
        </section>
      ) : null}
      {!loading && filteredItems.length ? <section className="surface-card feed-list" aria-label={saved ? "저장한 게시글 목록" : "내 게시글 목록"} aria-live="polite">{filteredItems.map((item) => <article className="feed-item" key={item.id}><Link className="feed-item-title" to={`/posts/${item.id}`}><h2>{item.title}</h2></Link><time dateTime={item.createdAt}>{formatDateTime(item.createdAt)}</time></article>)}</section> : null}
      <form aria-label="게시글 검색" className="search-panel community-bottom-search feed-bottom-search" onSubmit={(event) => {
        event.preventDefault();
        const form = new FormData(event.currentTarget);
        const nextQuery = String(form.get("q") ?? "").trim();
        const nextField = String(form.get("searchField") ?? "ALL");
        setSearchParams({ ...(nextQuery ? { q: nextQuery } : {}), ...(nextField !== "ALL" ? { searchField: nextField } : {}) });
      }}>
        <label><span className="search-label">검색 위치</span><select aria-label="검색 위치" name="searchField" defaultValue={searchField}><option value="ALL">제목+내용</option><option value="TITLE">제목</option><option value="BODY">내용</option></select></label>
        <label><span className="search-label">게시글 검색</span><input aria-label="검색어" name="q" defaultValue={query} placeholder="제목이나 내용" /></label>
        <button className="button button-soft" type="submit">검색</button>
      </form>
    </main>
  );
}
