import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, volunteerApi, type VolunteerOpportunity } from "./api/client";
import { useAbortableRequest } from "./hooks/useAbortableRequest";
import { formatDateTime } from "./utils/date";
import { useAuth } from "./auth/AuthContext";

export default function VolunteerPage() {
  const navigate = useNavigate();
  const { member } = useAuth();
  const [form, setForm] = useState({ title: "", description: "", organization: "", location: "", startsAt: "", capacity: "5" });
  const { data: items, error: requestError, loading, retry } = useAbortableRequest<VolunteerOpportunity[]>((signal) => volunteerApi.list(signal), []);
  const [pendingId, setPendingId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const minStartsAt = new Date(Date.now() - new Date().getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
  const error = requestError ? "봉사 기회를 불러오지 못했습니다." : actionError;
  async function apply(item: VolunteerOpportunity) {
    if (pendingId) return;
    const message = window.prompt("신청 메시지");
    if (!message?.trim()) return;
    setPendingId(item.id); setActionError(null); setNotice(null);
    try { await volunteerApi.apply(item.id, message.trim()); setNotice("봉사 신청이 접수되었습니다."); }
    catch (requestError) { setActionError(requestError instanceof ApiError && requestError.status === 401 ? "로그인이 필요합니다." : "중복 신청 또는 신청 불가"); }
    finally { setPendingId(null); }
  }
  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pendingId) return;
    setPendingId("create"); setActionError(null); setNotice(null);
    try {
      await volunteerApi.create({ ...form, startsAt: new Date(form.startsAt).toISOString(), capacity: Number(form.capacity) });
      setForm({ title: "", description: "", organization: "", location: "", startsAt: "", capacity: "5" });
      setNotice("봉사 기회를 등록했습니다.");
      retry();
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) navigate("/login?next=/volunteer");
      else setActionError("봉사 기회 정보 오류");
    } finally { setPendingId(null); }
  }
  return <main className="page localcare-page"><section className="localcare-hero"><h1>봉사 기회</h1></section>{error ? <p role="alert">{error} <button className="button button-soft" type="button" onClick={retry}>다시 불러오기</button></p> : null}{notice ? <p role="status">{notice}</p> : null}<section className="localcare-grid" aria-label="봉사 기회 목록" aria-busy={loading}>{(items ?? []).map((item) => <article className="surface-card localcare-card" key={item.id}><span className="publication-chip publication-chip-primary">{item.status}</span><h2>{item.title}</h2><p>{item.description}</p><small>{item.organization} · {item.location}</small><small>{formatDateTime(item.startsAt)} · 정원 {item.capacity}명</small>{member?.role === "MEMBER" ? <button className="button button-soft" disabled={Boolean(pendingId)} type="button" onClick={() => void apply(item)}>{pendingId === item.id ? "신청 중..." : "신청"}</button> : member ? null : <Link className="button button-soft" to="/login?next=/volunteer">로그인</Link>}</article>)}</section>{!items?.length && !error && !loading ? <p className="surface-card">예정된 기회 없음</p> : null}{member?.role === "MEMBER" ? <form className="surface-card publication-fields" onSubmit={create}><h2>봉사 기회 등록</h2><label>제목<input required maxLength={120} value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} /></label><label>설명<textarea required maxLength={5000} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label><label>기관명<input required maxLength={160} value={form.organization} onChange={(event) => setForm({ ...form, organization: event.target.value })} /></label><label>장소<input required maxLength={200} value={form.location} onChange={(event) => setForm({ ...form, location: event.target.value })} /></label><label>일시<input required min={minStartsAt} type="datetime-local" value={form.startsAt} onChange={(event) => setForm({ ...form, startsAt: event.target.value })} /></label><label>정원<input required min="1" max="100" type="number" value={form.capacity} onChange={(event) => setForm({ ...form, capacity: event.target.value })} /></label><button className="button button-primary" disabled={pendingId === "create"} type="submit">{pendingId === "create" ? "등록 중..." : "등록"}</button></form> : null}</main>;
}
