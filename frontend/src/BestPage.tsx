import { Link, useSearchParams } from "react-router-dom";
import { publicationApi, type PopularFeedItem } from "./api/client";
import CursorPagination from "./components/CursorPagination";
import { useCursorPagination } from "./hooks/useCursorPagination";
import { formatDateTime } from "./utils/date";

export default function BestPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Math.max(1, Number(searchParams.get("page") ?? "1") || 1);
  const feed = useCursorPagination<PopularFeedItem>({
    page,
    pageSize: 20,
    queryKey: "best",
    fetchPage: (cursor, signal) => publicationApi.popular({ cursor, signal }),
  });
  const setPage = (nextPage: number) => {
    const next = new URLSearchParams(searchParams);
    if (nextPage <= 1) next.delete("page"); else next.set("page", String(nextPage));
    setSearchParams(next);
  };
  return (
    <main className="page feed-page">
      <section className="feed-hero"><div><p className="eyebrow">BEST OF TOWNPET</p><h1>인기 게시글</h1><p>추천 수가 높은 공개 게시글을 모았습니다. 추천 수가 같으면 최신 글이 먼저 보입니다.</p></div></section>
      {feed.error ? <p role="alert">인기 게시글을 불러오지 못했습니다.</p> : null}
      <section className="surface-card feed-list" aria-label="인기 게시글 목록" aria-busy={feed.loading}>
        {feed.loading ? <p className="feed-empty" role="status">인기 게시글을 불러오는 중...</p> : null}
        {!feed.loading && !feed.error && !feed.items.length ? <p>아직 추천을 받은 인기글이 없습니다.</p> : null}
        {!feed.loading && !feed.error ? feed.items.map((item, index) => (
          <article className="feed-item best-feed-item" key={item.id}>
            <span className="feed-item-rank" aria-label={`${(page - 1) * 20 + index + 1}위`}>{(page - 1) * 20 + index + 1}</span>
            <div className="best-feed-copy"><Link className="feed-item-title" to={`/posts/${item.id}`}><h2>{item.title}</h2></Link><div className="feed-item-meta"><small>추천 {item.recommendationCount}</small><small>{formatDateTime(item.createdAt)}</small></div></div>
          </article>
        )) : null}
        {!feed.loading && !feed.error && feed.items.length ? <CursorPagination page={page} hasNext={feed.hasNext} disabled={feed.loading} onPageChange={setPage} /> : null}
      </section>
    </main>
  );
}
