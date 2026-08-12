import { useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, apiFetch, type Breed } from "./api/client";
import { useAbortableRequest } from "./hooks/useAbortableRequest";
export default function AdminBreedsPage() {
  const navigate = useNavigate();
  const { data: items, error: requestError, loading } = useAbortableRequest<Breed[]>((signal) => apiFetch<Breed[]>("/api/admin/breeds", { signal }), []);
  useEffect(() => { if (requestError instanceof ApiError && [401, 403].includes(requestError.status)) navigate("/", { replace: true }); }, [navigate, requestError]);
  const error = requestError ? "품종 catalog를 불러오지 못했습니다." : null;
  return <main className="page localcare-page"><section className="localcare-hero"><p className="eyebrow">BREED CATALOG</p><h1>품종 catalog</h1><p>TownPet에서 제공하는 품종 기준 정보를 확인합니다.</p></section>{error ? <p role="alert">{error}</p> : null}<section className="localcare-grid" aria-label="품종 목록" aria-busy={loading}>{(items ?? []).map((item) => <article className="surface-card localcare-card" key={item.code}><span className="publication-chip publication-chip-primary">{item.species}</span><h2>{item.name}</h2><p>{item.description}</p><Link className="publication-text-link" to={`/lounges/breeds/${item.code}`}>lounge 보기</Link></article>)}{!items?.length && !error && !loading ? <p>등록된 품종이 없습니다.</p> : null}</section><Link className="publication-text-link" to="/admin">운영 콘솔로</Link></main>;
}
