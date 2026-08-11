import { Link, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { ApiError, catalogApi, type Neighborhood } from "./api/client";

const sections: Record<string, { title: string; description: string; href: string }> = {
  adoption: { title: "입양 정보", description: "새 가족을 기다리는 반려동물 정보를 확인하세요.", href: "/boards/adoption" },
  guides: { title: "지역 가이드", description: "병원·산책·복지 정보를 확인하세요.", href: "/guides" },
  marketplace: { title: "동네 거래", description: "판매·대여·나눔 정보를 살펴보세요.", href: "/commercial" },
  community: { title: "동네 게시판", description: "이웃의 반려생활 이야기를 확인하세요.", href: "/feed/guest" },
};

export default function TownSectionPage() {
  const { townSlug = "", sectionSlug = "" } = useParams();
  const [town, setTown] = useState<Neighborhood | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => { catalogApi.neighborhood(townSlug).then(setTown).catch((requestError: unknown) => setError(requestError instanceof ApiError && requestError.status === 404 ? "지역을 찾을 수 없습니다." : "지역 정보를 불러오지 못했습니다.")); }, [townSlug]);
  const section = sections[sectionSlug] ?? sections.community;
  if (error) return <main className="page placeholder-page"><section className="surface-card"><p role="alert">{error}</p><Link className="button button-soft" to="/">홈으로</Link></section></main>;
  if (!town) return <main className="page placeholder-page"><section className="surface-card" role="status">지역 정보를 불러오는 중...</section></main>;
  return <main className="page page-home"><section className="hero-section"><p className="eyebrow">{town.name} · LOCAL SECTION</p><h1>{section.title}</h1><p className="hero-copy">{section.description}</p><div className="hero-actions"><Link className="button button-primary" to={section.href}>섹션 열기</Link><Link className="button button-soft" to={`/towns/${town.slug}`}>지역 홈</Link></div></section></main>;
}
