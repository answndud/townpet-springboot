import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError, apiFetch } from "./api/client";

type Adoption = { id: string; title: string; description: string; species: string; breed: string | null; status: string; createdAt: string };
export default function AdoptionDetailPage() {
  const { adoptionId = "" } = useParams(); const [item, setItem] = useState<Adoption | null>(null); const [error, setError] = useState<string | null>(null);
  useEffect(() => { apiFetch<Adoption>(`/api/v1/adoptions/${encodeURIComponent(adoptionId)}`).then(setItem).catch((e: unknown) => setError(e instanceof ApiError && e.status === 404 ? "입양 정보를 찾을 수 없습니다." : "입양 정보를 불러오지 못했습니다.")); }, [adoptionId]);
  if (error) return <main className="page placeholder-page"><section className="surface-card"><p role="alert">{error}</p><Link className="button button-soft" to="/boards/adoption">목록으로</Link></section></main>;
  if (!item) return <main className="page placeholder-page"><section className="surface-card" role="status">입양 정보를 불러오는 중...</section></main>;
  return <main className="page marketplace-page"><Link className="publication-text-link" to="/boards/adoption">← 입양 목록</Link><article className="surface-card marketplace-detail-card"><div className="marketplace-card-meta"><span className="publication-chip publication-chip-primary">{item.species}</span><span className="publication-chip">{item.status}</span></div><h1>{item.title}</h1><p className="marketplace-description">{item.description}</p><p className="marketplace-detail-meta">{item.breed ?? "품종 정보 없음"} · 등록 {new Date(item.createdAt).toLocaleDateString("ko-KR")}</p><p className="field-help">안전한 입양 상담은 운영 정책과 보호기관 안내를 함께 확인해 주세요.</p></article></main>;
}
