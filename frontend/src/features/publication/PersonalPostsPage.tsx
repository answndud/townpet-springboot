import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, memberApi, publicationApi, type Publication } from "../../api/client";

export default function PersonalPostsPage({ saved = false }: { saved?: boolean }) {
  const navigate = useNavigate(); const [items, setItems] = useState<Publication[]>([]); const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    const controller = new AbortController();
    const load = saved ? memberApi.bookmarks(controller.signal).then((ids) => Promise.all(ids.map((id) => publicationApi.detail(id, controller.signal)))) : memberApi.myPosts(controller.signal);
    load.then(setItems).catch((e: unknown) => { if (e instanceof ApiError && e.status === 401) navigate(`/login?next=${saved ? "/saved" : "/my-posts"}`, { replace: true }); else setError("게시글을 불러오지 못했습니다."); });
    return () => controller.abort();
  }, [navigate, saved]);
  return <main className="page feed-page"><header className="feed-hero"><div><p className="eyebrow">{saved ? "SAVED POSTS" : "MY POSTS"}</p><h1>{saved ? "저장한 글" : "내가 쓴 글"}</h1><p>{saved ? "나중에 다시 보고 싶은 반려생활 정보를 모아 두었습니다." : "내가 작성한 글과 기록을 확인하세요."}</p></div><Link className="button button-primary" to="/posts/new">글쓰기</Link></header>{error ? <p role="alert">{error}</p> : null}{!items.length && !error ? <p className="surface-card feed-empty">아직 게시글이 없습니다.</p> : <section className="surface-card feed-list" aria-label={saved ? "저장한 게시글 목록" : "내 게시글 목록"}>{items.map((item) => <article className="feed-item" key={item.id}><Link className="feed-item-title" to={`/posts/${item.id}`}><h2>{item.title}</h2></Link><p className="feed-item-excerpt">{item.body}</p><time dateTime={item.createdAt}>{new Date(item.createdAt).toLocaleString("ko-KR")}</time></article>)}</section>}</main>;
}
