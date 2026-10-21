import { Link } from "react-router-dom";
import { catalogApi, type Neighborhood } from "./api/client";
import { useAbortableRequest } from "./hooks/useAbortableRequest";

export default function NeighborhoodMapPage() {
  const { data: loadedItems, error: requestError } = useAbortableRequest<Neighborhood[]>((signal) => catalogApi.neighborhoods(signal), []);
  const items = loadedItems ?? [];
  const error = requestError ? "지역 목록을 불러오지 못했습니다." : null;
  return <main className="page localcare-page"><section className="localcare-hero"><h1>지역</h1></section>{error ? <p role="alert">{error}</p> : null}<section className="localcare-grid" aria-label="지역 목록">{items.map((item) => <Link className="surface-card localcare-card" to={`/towns/${item.slug}`} key={item.id}><span className="publication-chip publication-chip-primary">지역</span><h2>{item.name}</h2><p>게시판·입양·거래·가이드</p><small>{item.slug}</small></Link>)}{!items.length && !error ? <p className="surface-card localcare-empty">지역 없음</p> : null}</section></main>;
}
