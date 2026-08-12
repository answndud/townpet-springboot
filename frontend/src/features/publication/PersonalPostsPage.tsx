import { useEffect, useMemo } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ApiError, memberApi, publicationApi, type Publication } from "../../api/client";
import { useAbortableRequest } from "../../hooks/useAbortableRequest";
import { formatDateTime } from "../../utils/date";

export default function PersonalPostsPage({ saved = false }: { saved?: boolean }) {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const query = searchParams.get("q") ?? "";
  const { data: items, error: requestError, loading } = useAbortableRequest<Publication[]>((signal) => saved ? memberApi.bookmarks(signal).then((ids) => Promise.all(ids.map((id) => publicationApi.detail(id, signal)))) : memberApi.myPosts(signal), [saved]);
  const error = requestError instanceof ApiError && requestError.status === 401 ? "로그인이 필요합니다." : requestError ? "게시글을 불러오지 못했습니다." : null;
  useEffect(() => {
    if (requestError instanceof ApiError && requestError.status === 401) navigate(`/login?next=${saved ? "/saved" : "/my-posts"}`, { replace: true });
  }, [navigate, requestError, saved]);
  const filteredItems = useMemo(() => query.trim() ? (items ?? []).filter((item) => `${item.title} ${item.body}`.toLocaleLowerCase().includes(query.trim().toLocaleLowerCase())) : items ?? [], [items, query]);
  return <main className="page feed-page"><header className="feed-hero"><div><p className="eyebrow">{saved ? "SAVED POSTS" : "MY POSTS"}</p><h1>{saved ? "저장한 글" : "내가 쓴 글"}</h1><p>{saved ? "나중에 다시 보고 싶은 반려생활 정보를 모아 두었습니다." : "내가 작성한 글과 기록을 확인하세요."}</p></div><Link className="button button-primary" to="/posts/new">글쓰기</Link></header><form className="surface-card profile-actions" onSubmit={(event) => { event.preventDefault(); const form = new FormData(event.currentTarget); const next = String(form.get("q") ?? "").trim(); setSearchParams(next ? { q: next } : {}); }}><label>제목·내용 검색<input name="q" defaultValue={query} placeholder="검색어를 입력해 주세요" /></label><button className="button button-soft" type="submit">검색</button>{query ? <button className="button button-soft" type="button" onClick={() => setSearchParams({})}>초기화</button> : null}</form>{error ? <p role="alert">{error}</p> : null}{loading ? <p className="surface-card" role="status">게시글을 불러오는 중...</p> : null}{!loading && !filteredItems.length && !error ? <p className="surface-card feed-empty">{query ? "조건에 맞는 게시글이 없습니다." : "아직 게시글이 없습니다."}</p> : null}{!loading && filteredItems.length ? <section className="surface-card feed-list" aria-label={saved ? "저장한 게시글 목록" : "내 게시글 목록"}>{filteredItems.map((item) => <article className="feed-item" key={item.id}><Link className="feed-item-title" to={`/posts/${item.id}`}><h2>{item.title}</h2></Link><p className="feed-item-excerpt">{item.body}</p><time dateTime={item.createdAt}>{formatDateTime(item.createdAt)}</time></article>)}</section> : null}</main>;
}
