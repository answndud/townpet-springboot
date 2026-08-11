import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { apiFetch, type Publication } from "./api/client";

type BestItem = Pick<Publication, "id" | "title" | "body" | "createdAt">;

export default function BestPage() {
  const [items, setItems] = useState<BestItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => { apiFetch<{ items: BestItem[] }>("/api/v1/feed/popular").then((page) => setItems(page.items)).catch(() => setError("인기 게시글을 불러오지 못했습니다.")); }, []);
  return <main className="page feed-page"><section className="feed-hero"><div><p className="eyebrow">BEST OF TOWNPET</p><h1>인기 게시글</h1><p>공개 조회와 최신성을 기준으로 많이 읽힌 글을 모았습니다.</p></div></section>{error ? <p role="alert">{error}</p> : null}<section className="surface-card feed-list" aria-label="인기 게시글 목록">{items.map((item) => <article className="feed-item" key={item.id}><Link className="feed-item-title" to={`/posts/${item.id}`}><h2>{item.title}</h2></Link><p className="feed-item-excerpt">{item.body}</p><small>{new Date(item.createdAt).toLocaleString("ko-KR")}</small></article>)}{!items.length && !error ? <p>아직 인기 게시글이 없습니다.</p> : null}</section></main>;
}
