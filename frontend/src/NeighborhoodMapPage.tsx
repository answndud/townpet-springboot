import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { catalogApi, type Neighborhood } from "./api/client";

export default function NeighborhoodMapPage() {
  const [items, setItems] = useState<Neighborhood[]>([]);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => { catalogApi.neighborhoods().then(setItems).catch(() => setError("지역 목록을 불러오지 못했습니다.")); }, []);
  return <main className="page localcare-page"><section className="localcare-hero"><p className="eyebrow">NEIGHBORHOOD MAP</p><h1>우리 동네 반려생활 지도</h1><p>TownPet에서 제공하는 지역별 반려생활 정보를 선택해 보세요.</p></section>{error ? <p role="alert">{error}</p> : null}<section className="localcare-grid" aria-label="지역 목록">{items.map((item) => <Link className="surface-card localcare-card" to={`/towns/${item.slug}`} key={item.id}><span className="publication-chip publication-chip-primary">LOCAL</span><h2>{item.name}</h2><p>게시판·입양·거래·지역 가이드</p><small>{item.slug}</small></Link>)}{!items.length && !error ? <p className="surface-card localcare-empty">등록된 지역이 없습니다.</p> : null}</section><Link className="publication-text-link" to="/">홈으로</Link></main>;
}
