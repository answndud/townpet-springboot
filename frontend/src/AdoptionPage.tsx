import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { apiFetch } from "./api/client";

type Adoption = { id: string; title: string; description: string; species: string; breed: string | null; status: string; createdAt: string };

export default function AdoptionPage() {
  const [items, setItems] = useState<Adoption[]>([]);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => { apiFetch<Adoption[]>("/api/boards/adoption/posts").then(setItems).catch(() => setError("입양 정보를 불러오지 못했습니다.")); }, []);
  return <main className="page marketplace-page"><section className="marketplace-hero"><div><p className="eyebrow">ADOPTION</p><h1>입양을 기다리는 반려동물</h1><p>합성 demo 정보로 보호동물의 상태와 특징을 확인하세요.</p></div></section>{error ? <p role="alert">{error}</p> : null}<section className="marketplace-grid" aria-label="입양 목록">{items.map((item) => <article className="surface-card marketplace-card" key={item.id}><div className="marketplace-card-meta"><span className="publication-chip publication-chip-primary">{item.species}</span><span className="publication-chip">{item.status}</span></div><h2>{item.title}</h2><p>{item.description}</p><small>{item.breed ?? "품종 정보 없음"} · {new Date(item.createdAt).toLocaleDateString("ko-KR")}</small></article>)}{!items.length && !error ? <p>현재 입양 목록이 없습니다.</p> : null}</section><Link className="publication-text-link" to="/">홈으로</Link></main>;
}
