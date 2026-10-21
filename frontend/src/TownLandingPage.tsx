import { Link, useParams } from "react-router-dom";
import { ApiError, catalogApi, type Neighborhood } from "./api/client";
import { useAbortableRequest } from "./hooks/useAbortableRequest";

export default function TownLandingPage() {
  const { townSlug = "" } = useParams();
  const { data: town, error: requestError } = useAbortableRequest<Neighborhood>((signal) => catalogApi.neighborhood(townSlug, signal), [townSlug]);
  const error = requestError instanceof ApiError && requestError.status === 404 ? "지역을 찾을 수 없습니다." : requestError ? "지역 정보를 불러오지 못했습니다." : null;
  if (error) return <main className="page placeholder-page"><section className="surface-card"><p role="alert">{error}</p><Link className="button button-soft" to="/">홈으로</Link></section></main>;
  if (!town) return <main className="page placeholder-page"><section className="surface-card" role="status">지역 정보를 불러오는 중...</section></main>;
  return <main className="page page-home"><section className="hero-section"><h1>{town.name}</h1><div className="hero-actions"><Link className="button button-primary" to="/?view=all">게시판</Link><Link className="button button-soft" to="/guides">가이드</Link></div></section><section className="preview-grid"><Link className="surface-card" to="/boards/adoption"><h2>입양 정보</h2></Link><Link className="surface-card" to="/marketplace"><h2>동네 거래</h2></Link></section></main>;
}
