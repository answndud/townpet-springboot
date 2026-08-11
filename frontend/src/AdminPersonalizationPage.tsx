import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, apiFetch } from "./api/client";

type Candidate = { publicationId: string; title: string; createdAt: string };
type Ranking = { strategy: string; candidates: Candidate[] };

export default function AdminPersonalizationPage() {
  const navigate = useNavigate();
  const [ranking, setRanking] = useState<Ranking | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => { apiFetch<Ranking>("/api/admin/personalization").then(setRanking).catch((requestError: unknown) => { if (requestError instanceof ApiError && [401, 403].includes(requestError.status)) navigate("/", { replace: true }); else setError("personalization 정보를 불러오지 못했습니다."); }); }, [navigate]);
  if (error) return <main className="page placeholder-page"><section className="surface-card"><p role="alert">{error}</p></section></main>;
  if (!ranking) return <main className="page placeholder-page"><section className="surface-card" role="status">ranking 정보를 불러오는 중...</section></main>;
  return <main className="page notification-page"><section className="localcare-hero"><p className="eyebrow">PERSONALIZATION</p><h1>피드 ranking evidence</h1><p>현재 공개 인기 feed가 사용하는 ranking 전략과 후보를 확인합니다.</p><span className="publication-chip publication-chip-primary">{ranking.strategy}</span></section><section className="notification-list" aria-label="ranking 후보">{ranking.candidates.map((item) => <Link className="surface-card notification-item" to={`/posts/${item.publicationId}`} key={item.publicationId}><h2>{item.title}</h2><small>{new Date(item.createdAt).toLocaleString("ko-KR")}</small></Link>)}{!ranking.candidates.length ? <p>ranking 후보가 없습니다.</p> : null}</section><Link className="publication-text-link" to="/admin">운영 콘솔로</Link></main>;
}
