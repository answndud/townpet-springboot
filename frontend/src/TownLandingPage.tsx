import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError, catalogApi, type Neighborhood } from "./api/client";

export default function TownLandingPage() {
  const { townSlug = "" } = useParams();
  const [town, setTown] = useState<Neighborhood | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => { catalogApi.neighborhood(townSlug).then(setTown).catch((requestError: unknown) => setError(requestError instanceof ApiError && requestError.status === 404 ? "지역을 찾을 수 없습니다." : "지역 정보를 불러오지 못했습니다.")); }, [townSlug]);
  if (error) return <main className="page placeholder-page"><section className="surface-card"><p role="alert">{error}</p><Link className="button button-soft" to="/">홈으로</Link></section></main>;
  if (!town) return <main className="page placeholder-page"><section className="surface-card" role="status">지역 정보를 불러오는 중...</section></main>;
  return <main className="page page-home"><section className="hero-section"><p className="eyebrow">TOWNPET LOCAL</p><h1>{town.name} 반려생활</h1><p className="hero-copy">우리 동네의 입양·거래·지역 가이드를 한 곳에서 확인하세요.</p><div className="hero-actions"><Link className="button button-primary" to="/feed/guest">동네 게시판</Link><Link className="button button-soft" to="/guides">지역 가이드</Link></div></section><section className="preview-grid"><Link className="surface-card" to="/boards/adoption"><span className="card-label">ADOPTION</span><h2>입양 정보</h2><p>새 가족을 기다리는 반려동물 정보를 확인하세요.</p></Link><Link className="surface-card" to="/commercial"><span className="card-label">MARKETPLACE</span><h2>동네 거래</h2><p>판매·대여·나눔 정보를 살펴보세요.</p></Link></section></main>;
}
