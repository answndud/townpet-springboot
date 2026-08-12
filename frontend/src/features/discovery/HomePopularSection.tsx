import { Link } from "react-router-dom";
import { apiFetch } from "../../api/client";
import { useAbortableRequest } from "../../hooks/useAbortableRequest";
import { formatDateTime } from "../../utils/date";

export type PopularPublication = {
  id: string;
  title: string;
  body: string;
  createdAt: string;
  recommendationCount?: number;
};

type PopularFeedResponse = {
  items?: PopularPublication[];
};

function excerpt(body: string) {
  const compact = body.replace(/\s+/g, " ").trim();
  return compact.length > 92 ? `${compact.slice(0, 92)}…` : compact;
}

export default function HomePopularSection() {
  const { data, error, loading } = useAbortableRequest<PopularFeedResponse>(
    (signal) => apiFetch<PopularFeedResponse>("/api/v1/feed/popular", { signal }),
    [],
  );
  const items = (data?.items ?? []).slice(0, 5);

  return (
    <section className="home-community" aria-labelledby="home-popular-title">
      <header className="home-community-header">
        <div>
          <p className="eyebrow">COMMUNITY PICKS</p>
          <h2 id="home-popular-title">인기글</h2>
          <p>추천을 많이 받은 공개 게시글을 한눈에 확인해 보세요.</p>
        </div>
        <Link className="publication-text-link" to="/best">
          인기글 전체 보기
        </Link>
      </header>

      {error ? (
        <p className="home-community-empty" role="alert">
          인기글을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.
        </p>
      ) : loading ? (
        <p className="home-community-empty" role="status">
          인기글을 불러오는 중입니다…
        </p>
      ) : items.length ? (
        <ol className="home-popular-list">
          {items.map((item, index) => (
            <li className="home-popular-item" key={item.id}>
              <span className="home-popular-rank" aria-label={`${index + 1}위`}>
                {index + 1}
              </span>
              <div className="home-popular-copy">
                <Link to={`/posts/${item.id}`}>
                  <h3>{item.title}</h3>
                </Link>
                <p>{excerpt(item.body)}</p>
              </div>
              <div className="home-popular-meta">
                {item.recommendationCount !== undefined ? <span>추천 {item.recommendationCount}</span> : null}
                <time dateTime={item.createdAt}>{formatDateTime(item.createdAt)}</time>
              </div>
            </li>
          ))}
        </ol>
      ) : (
        <p className="home-community-empty">아직 추천을 받은 인기글이 없습니다.</p>
      )}
    </section>
  );
}
