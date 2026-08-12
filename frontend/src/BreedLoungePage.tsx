import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError, apiFetch, catalogApi, type Breed, type FeedPage, type Member } from "./api/client";
import { useAuth } from "./auth/AuthContext";
import { formatDateTime } from "./utils/date";

export default function BreedLoungePage() {
  const { breedCode = "" } = useParams();
  const [breed, setBreed] = useState<Breed | null>(null);
  const [feed, setFeed] = useState<FeedPage | null>(null);
  const [viewerRole, setViewerRole] = useState<Member["role"] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { member } = useAuth();
  useEffect(() => {
    const controller = new AbortController();
    Promise.all([
      catalogApi.breed(breedCode, controller.signal),
      apiFetch<FeedPage>(`/api/lounges/breeds/${encodeURIComponent(breedCode)}/posts?audience=GLOBAL&scope=ALL`, { signal: controller.signal }),
    ]).then(([loadedBreed, loadedFeed]) => { setBreed(loadedBreed); setFeed(loadedFeed); }).catch((requestError: unknown) => {
      if (requestError instanceof DOMException && requestError.name === "AbortError") return;
      if (controller.signal.aborted) return;
      setError(requestError instanceof ApiError && requestError.status === 404 ? "품종 lounge를 찾을 수 없습니다." : "품종 정보를 불러오지 못했습니다.");
    });
    return () => controller.abort();
  }, [breedCode]);
  useEffect(() => setViewerRole(member?.role ?? null), [member?.role]);
  if (error) return <main className="page placeholder-page"><section className="surface-card"><p role="alert">{error}</p><Link className="button button-soft" to="/feed/guest">전체 피드</Link></section></main>;
  if (!breed || !feed) return <main className="page placeholder-page"><section className="surface-card" role="status">품종 정보를 불러오는 중...</section></main>;
  return <main className="page feed-page"><section className="feed-hero"><div><p className="eyebrow">{breed.species} LOUNGE</p><h1>{breed.name}</h1><p>{breed.description}</p></div>{viewerRole !== "MODERATOR" ? <Link className="button button-primary" to="/posts/new">글쓰기</Link> : null}</section><section className="surface-card feed-list" aria-label={`${breed.name} 게시글`}>{feed.items.map((item) => <article className="feed-item" key={item.id}><Link className="feed-item-title" to={`/posts/${item.id}`}><h2>{item.title}</h2></Link><p className="feed-item-excerpt">{item.body}</p><small>{formatDateTime(item.createdAt)}</small></article>)}{!feed.items.length ? <p>아직 등록된 게시글이 없습니다.</p> : null}</section></main>;
}
