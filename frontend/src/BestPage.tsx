import { Link } from "react-router-dom";
import { apiFetch } from "./api/client";
import type { PopularPublication } from "./features/discovery/HomePopularSection";
import { useAbortableRequest } from "./hooks/useAbortableRequest";
import { formatDateTime } from "./utils/date";

type BestItem = Pick<PopularPublication, "id" | "title" | "body" | "createdAt"> & {
  recommendationCount?: number;
  rank?: number;
};

export default function BestPage() {
  const { data: page, error: requestError, loading } = useAbortableRequest<{ items?: BestItem[] }>((signal) => apiFetch<{ items?: BestItem[] }>("/api/v1/feed/popular", { signal }), []);
  const items = page?.items ?? [];
  const error = requestError ? "인기 게시글을 불러오지 못했습니다." : null;
  return <main className="page feed-page"><section className="feed-hero"><div><p className="eyebrow">BEST OF TOWNPET</p><h1>인기 게시글</h1><p>추천 수가 높은 공개 게시글을 모았습니다. 추천 수가 같으면 최신 글이 먼저 보입니다.</p></div></section>{error ? <p role="alert">{error}</p> : null}<section className="surface-card feed-list" aria-label="인기 게시글 목록" aria-busy={loading}>{items.map((item, index) => <article className="feed-item best-feed-item" key={item.id}><span className="feed-item-rank" aria-label={`${item.rank ?? index + 1}위`}>{item.rank ?? index + 1}</span><div className="best-feed-copy"><Link className="feed-item-title" to={`/posts/${item.id}`}><h2>{item.title}</h2></Link><div className="feed-item-meta">{item.recommendationCount !== undefined ? <small>추천 {item.recommendationCount}</small> : null}<small>{formatDateTime(item.createdAt)}</small></div></div></article>)}{!items.length && !error && !loading ? <p>아직 추천을 받은 인기글이 없습니다.</p> : null}</section></main>;
}
