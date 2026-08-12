import { useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, apiFetch } from "./api/client";
import { useAbortableRequest } from "./hooks/useAbortableRequest";
import { formatDateTime } from "./utils/date";
type Candidate = { publicationId: string; title: string; createdAt: string };
type Ranking = { strategy: string; candidates: Candidate[] };
const strategyLabels: Record<string, string> = { PUBLIC_VIEW_COUNT_RECENCY: "공개 조회·최신성" };
export default function AdminPersonalizationPage() {
  const navigate = useNavigate();
  const { data: ranking, error: requestError, loading } = useAbortableRequest<Ranking>((signal) => apiFetch<Ranking>("/api/admin/personalization", { signal }), []);
  useEffect(() => { if (requestError instanceof ApiError && [401, 403].includes(requestError.status)) navigate("/", { replace: true }); }, [navigate, requestError]);
  const error = requestError ? "개인화 정보를 불러오지 못했습니다." : null;
  if (error) return <main className="page placeholder-page"><section className="surface-card"><p role="alert">{error}</p></section></main>;
  if (!ranking || loading) return <main className="page placeholder-page"><section className="surface-card" role="status">개인화 정보를 불러오는 중...</section></main>;
  return <main className="page notification-page"><section className="localcare-hero"><p className="eyebrow">PERSONALIZATION</p><h1>피드 개인화 근거</h1><p>현재 공개 인기 피드에 적용된 기준과 후보를 확인합니다.</p><span className="publication-chip publication-chip-primary">{strategyLabels[ranking.strategy] ?? ranking.strategy}</span></section><section className="notification-list" aria-label="개인화 후보">{ranking.candidates.map((item) => <Link className="surface-card notification-item" to={`/posts/${item.publicationId}`} key={item.publicationId}><h2>{item.title}</h2><small>{formatDateTime(item.createdAt)}</small></Link>)}{!ranking.candidates.length ? <p>개인화 후보가 없습니다.</p> : null}</section><Link className="publication-text-link" to="/admin">운영 콘솔로</Link></main>;
}
