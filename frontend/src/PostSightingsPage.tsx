import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError, lostFoundApi, type LostFoundAlert, type LostFoundSighting } from "./api/client";

export default function PostSightingsPage() {
  const { publicationId = "" } = useParams();
  const [alert, setAlert] = useState<LostFoundAlert | null>(null);
  const [items, setItems] = useState<LostFoundSighting[]>([]);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => { Promise.all([lostFoundApi.detail(publicationId), lostFoundApi.sightings(publicationId)]).then(([loadedAlert, sightings]) => { setAlert(loadedAlert); setItems(sightings); }).catch((requestError: unknown) => setError(requestError instanceof ApiError && requestError.status === 404 ? "게시글 또는 목격 기록을 찾을 수 없습니다." : "목격 기록을 불러오지 못했습니다.")); }, [publicationId]);
  if (error) return <main className="page placeholder-page"><section className="surface-card"><p role="alert">{error}</p><Link className="button button-soft" to="/lost-found">분실·목격 목록</Link></section></main>;
  if (!alert) return <main className="page placeholder-page"><section className="surface-card" role="status">목격 기록을 불러오는 중...</section></main>;
  return <main className="page notification-page"><section className="localcare-hero"><p className="eyebrow">SIGHTINGS</p><h1>{alert.title} 목격 기록</h1><p>{alert.description}</p></section><section className="notification-list" aria-label="목격 기록">{items.map((item) => <article className="surface-card notification-item" key={item.id}><h2>{item.description}</h2><p>위치: {item.approximateLocation.latitude.toFixed(3)}, {item.approximateLocation.longitude.toFixed(3)}</p><small>{new Date(item.seenAt).toLocaleString("ko-KR")}</small></article>)}{!items.length ? <p>등록된 목격 기록이 없습니다.</p> : null}</section><Link className="publication-text-link" to={`/lost-found/${alert.id}`}>분실·목격 상세로</Link></main>;
}
