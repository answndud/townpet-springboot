import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError, publicationApi, type Publication } from "../../api/client";

function formatDate(value: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export default function PublicationDetailPage() {
  const { publicationId = "" } = useParams();
  const [publication, setPublication] = useState<Publication | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    setPublication(null);
    setError(null);
    publicationApi
      .detail(publicationId, controller.signal)
      .then(setPublication)
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        setError(
          requestError instanceof ApiError && requestError.status === 404
            ? "존재하지 않거나 삭제된 게시글입니다."
            : "게시글을 불러오지 못했습니다.",
        );
      });
    return () => controller.abort();
  }, [publicationId]);

  if (error) {
    return (
      <main className="page publication-page publication-state-page">
        <section className="surface-card">
          <p className="eyebrow">게시글 상세</p>
          <h1>글을 열 수 없습니다</h1>
          <p role="alert">{error}</p>
          <Link className="button button-soft" to="/feed/guest">게시판으로</Link>
        </section>
      </main>
    );
  }

  if (!publication) {
    return (
      <main className="page publication-page">
        <section className="surface-card" role="status">게시글을 불러오는 중...</section>
      </main>
    );
  }

  return (
    <main className="page publication-page publication-detail-page">
      <div className="publication-detail-nav">
        <Link className="publication-text-link" to="/feed/guest">목록으로</Link>
        <Link className="button button-soft" to="/posts/new">새 글 작성</Link>
      </div>
      <article className="surface-card publication-detail-card">
        <div className="publication-detail-chips">
          <span className="publication-chip publication-chip-primary">자유게시판</span>
          <span className="publication-chip">
            {publication.scope === "LOCAL" ? "내 동네" : "전체 공개"}
          </span>
        </div>
        <header className="publication-detail-heading">
          <h1>{publication.title}</h1>
          <div className="publication-author-row">
            <span className="publication-avatar" aria-hidden="true">T</span>
            <div>
              <strong>TownPet 회원</strong>
              <p>{formatDate(publication.createdAt)}</p>
            </div>
          </div>
        </header>
        <div className="publication-body">{publication.body}</div>
      </article>
    </main>
  );
}
