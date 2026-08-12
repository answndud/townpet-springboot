import { FormEvent, useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { publicationApi, type Publication } from "../../api/client";
import { isAbortError } from "../../hooks/useAbortableRequest";

export default function SearchPage({ guest = false }: { guest?: boolean }) {
  const [params, setParams] = useSearchParams();
  const urlQuery = params.get("q") ?? "";
  const urlFrom = params.get("from") ?? "";
  const urlTo = params.get("to") ?? "";
  const [query, setQuery] = useState(urlQuery);
  const [from, setFrom] = useState(urlFrom);
  const [to, setTo] = useState(urlTo);
  const [items, setItems] = useState<Publication[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setQuery(urlQuery);
    setFrom(urlFrom);
    setTo(urlTo);
    const controller = new AbortController();
    if (!urlQuery && !urlFrom && !urlTo) {
      setItems([]);
      setError(null);
      setLoading(false);
      return () => controller.abort();
    }
    setError(null);
    setLoading(true);
    publicationApi.feed({ audience: guest ? "GLOBAL" : "VIEWER", query: urlQuery, from: urlFrom, to: urlTo, signal: controller.signal })
      .then((page) => setItems(page.items))
      .catch((requestError: unknown) => {
        if (isAbortError(requestError)) return;
        if (!controller.signal.aborted) setError("검색하지 못했습니다.");
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [guest, urlFrom, urlQuery, urlTo]);

  function submit(event: FormEvent) {
    event.preventDefault();
    const next = Object.fromEntries(Object.entries({ q: query.trim(), from, to }).filter(([, value]) => value));
    setParams(next);
  }
  return <main className="page feed-page"><header className="feed-hero"><div><p className="eyebrow">SEARCH</p><h1>반려생활 정보 검색</h1><p>게시글 제목·본문과 기간으로 필요한 정보를 찾아보세요.</p></div></header><form className="localcare-search surface-card" onSubmit={submit}><label>검색어<input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="산책, 병원, 분실..." /></label><label>시작일<input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></label><label>종료일<input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></label><button className="button button-primary" type="submit">검색</button></form>{error ? <p role="alert">{error}</p> : null}<section className="surface-card feed-list" aria-label="검색 결과" aria-busy={loading}>{loading ? <p role="status">검색 중...</p> : null}{!loading ? items.map((item) => <article className="feed-item" key={item.id}><Link className="feed-item-title" to={`/posts/${item.id}`}><h2>{item.title}</h2></Link><p className="feed-item-excerpt">{item.body}</p></article>) : null}{!loading && !items.length && !error ? <p>검색 결과가 없습니다.</p> : null}</section></main>;
}
