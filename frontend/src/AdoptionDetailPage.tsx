import { ContentVisual } from "./components/CommunityVisual";
import { ANIMAL_BOARD_OPTIONS } from "./features/member/AnimalBoardCatalog";
import { useEffect } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError, apiFetch } from "./api/client";
import { useAbortableRequest } from "./hooks/useAbortableRequest";
import { formatDateOnly } from "./utils/date";
import { setDynamicSeo } from "./utils/seo";

type Adoption = { id: string; title: string; description: string; species: string; breed: string | null; status: string; createdAt: string };
const speciesLabels: Record<string, string> = { ...Object.fromEntries(ANIMAL_BOARD_OPTIONS.map(({ code, label }) => [code, label])), RABBIT: "토끼", FISH: "물고기" };
export default function AdoptionDetailPage() {
  const { adoptionId = "" } = useParams();
  const { data: item, error: requestError, loading, retry } = useAbortableRequest<Adoption>((signal) => apiFetch<Adoption>(`/api/v1/adoptions/${encodeURIComponent(adoptionId)}`, { signal }), [adoptionId]);
  const error = requestError instanceof ApiError && requestError.status === 404 ? "입양 정보를 찾을 수 없습니다." : requestError ? "입양 정보를 불러오지 못했습니다." : null;
  useEffect(() => {
    if (item) setDynamicSeo({ title: item.title, description: item.description, canonicalPath: `/adoptions/${item.id}` });
    else if (error) setDynamicSeo({ title: "페이지를 찾을 수 없습니다", description: error, canonicalPath: `/adoptions/${adoptionId}`, indexable: false });
  }, [adoptionId, error, item]);
  if (error) return <main className="page placeholder-page"><section className="surface-card"><h1>입양 정보를 확인할 수 없어요</h1><p role="alert">{error}</p><button className="button button-soft" type="button" onClick={retry}>다시 시도</button><Link className="button button-soft" to="/boards/adoption">목록으로</Link></section></main>;
  if (!item || loading) return <main className="page placeholder-page"><section className="surface-card" role="status">입양 정보를 불러오는 중...</section></main>;
  return <main className="page marketplace-page"><div className="marketplace-detail-nav"><Link className="publication-text-link" to="/boards/adoption">← 입양 목록</Link></div><article className="surface-card marketplace-detail-card"><ContentVisual kind="paw" /><div className="marketplace-card-meta"><span className="publication-chip publication-chip-primary">{speciesLabels[item.species.toUpperCase()] ?? item.species}</span><span className="publication-chip">{({OPEN:"가족을 찾는 중",RESERVED:"상담 중",CLOSED:"상담 종료",ADOPTED:"입양 완료"} as Record<string,string>)[item.status] ?? item.status}</span></div><h1>{item.title}</h1><p className="marketplace-description">{item.description}</p><p className="marketplace-detail-meta">{item.breed ?? "품종 정보 없음"} · 등록 {formatDateOnly(item.createdAt)}</p><p className="field-help">안전한 입양 상담은 운영 정책과 보호기관 안내를 함께 확인해 주세요.</p></article></main>;
}
