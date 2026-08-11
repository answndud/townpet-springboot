import { FormEvent, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { publicationApi, type Publication } from "../../api/client";

export default function SearchPage({ guest = false }: { guest?: boolean }) {
  const [params, setParams] = useSearchParams(); const initial = params.get("q") ?? ""; const [query, setQuery] = useState(initial); const [items, setItems] = useState<Publication[]>([]); const [error, setError] = useState<string | null>(null);
  async function submit(event: FormEvent) { event.preventDefault(); setParams(query.trim() ? { q: query.trim() } : {}); setError(null); try { const page = await publicationApi.feed({ audience: guest ? "GLOBAL" : "VIEWER", query }); setItems(page.items); } catch { setError("검색하지 못했습니다."); } }
  return <main className="page feed-page"><header className="feed-hero"><div><p className="eyebrow">SEARCH</p><h1>반려생활 정보 검색</h1><p>게시글 제목과 본문에서 필요한 정보를 찾아보세요.</p></div></header><form className="localcare-search surface-card" onSubmit={submit}><label>검색어<input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="산책, 병원, 분실..." /></label><button className="button button-primary" type="submit">검색</button></form>{error ? <p role="alert">{error}</p> : null}<section className="surface-card feed-list" aria-label="검색 결과">{items.map((item) => <article className="feed-item" key={item.id}><Link className="feed-item-title" to={`/posts/${item.id}`}><h2>{item.title}</h2></Link><p className="feed-item-excerpt">{item.body}</p></article>)}{!items.length && !error ? <p>검색 결과가 없습니다.</p> : null}</section></main>;
}
