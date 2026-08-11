import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, careApi, memberApi, type CareRequest } from "../../api/client";

const statusLabel = { OPEN: "모집 중", MATCHED: "매칭됨", CANCELLED: "취소", EXPIRED: "만료" } as const;

export function CareListPage() {
  const [items, setItems] = useState<CareRequest[]>([]);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => { const controller = new AbortController(); careApi.list(controller.signal).then(setItems).catch(() => setError("돌봄 요청을 불러오지 못했습니다.")); return () => controller.abort(); }, []);
  return <main className="page localcare-page"><section className="localcare-hero"><p className="eyebrow">NEIGHBOR CARE</p><h1>이웃 돌봄 요청</h1><p>결제나 지급을 보장하지 않는 참고 reward로 안전하게 요청을 확인하세요.</p><Link className="button button-primary" to="/care/new">돌봄 요청 작성</Link></section>{error ? <p role="alert">{error}</p> : null}<section className="localcare-grid" aria-label="돌봄 요청 목록">{items.map((item) => <article className="surface-card localcare-card" key={item.id}><span className="publication-chip publication-chip-primary">{statusLabel[item.status]}</span><h2>{item.title}</h2><p>{item.description}</p><small>{item.location} · {new Date(item.startsAt).toLocaleString("ko-KR")}</small>{item.rewardHint ? <small>참고 reward: {item.rewardHint}</small> : null}</article>)}{!items.length && !error ? <p className="surface-card localcare-empty">현재 열린 돌봄 요청이 없습니다.</p> : null}</section></main>;
}

export function CareCreatePage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ title: "", description: "", location: "", startsAt: "", endsAt: "", rewardHint: "" });
  const [error, setError] = useState<string | null>(null);
  async function submit(event: FormEvent) { event.preventDefault(); try { await memberApi.current(); await careApi.create({ ...form, startsAt: new Date(form.startsAt).toISOString(), endsAt: new Date(form.endsAt).toISOString() }); navigate("/care"); } catch (requestError) { if (requestError instanceof ApiError && requestError.status === 401) navigate("/login?next=/care/new"); else setError("돌봄 요청 내용을 확인해 주세요."); } }
  return <main className="page localcare-page"><section className="localcare-hero"><p className="eyebrow">CARE REQUEST</p><h1>돌봄 요청 작성</h1></section><form className="surface-card publication-fields" onSubmit={submit}><label>제목<input required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></label><label>설명<textarea required value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label><label>장소<input required value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} /></label><label>시작 시각<input required type="datetime-local" value={form.startsAt} onChange={(e) => setForm({ ...form, startsAt: e.target.value })} /></label><label>종료 시각<input required type="datetime-local" value={form.endsAt} onChange={(e) => setForm({ ...form, endsAt: e.target.value })} /></label><label>참고 reward<input value={form.rewardHint} onChange={(e) => setForm({ ...form, rewardHint: e.target.value })} /></label>{error ? <p role="alert">{error}</p> : null}<button className="button button-primary" type="submit">등록</button></form></main>;
}
