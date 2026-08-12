import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ApiError, hospitalReviewApi, type HospitalReview } from "./api/client";
import { useAbortableRequest } from "./hooks/useAbortableRequest";

export default function HospitalReviewPage() {
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [submittedQuery, setSubmittedQuery] = useState("");
  const [form, setForm] = useState({ hospitalName: "", address: "", rating: "5", body: "" });
  const [pendingAction, setPendingAction] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const { data: items, error: requestError, loading, retry } = useAbortableRequest<HospitalReview[]>((signal) => hospitalReviewApi.list(submittedQuery, signal), [submittedQuery]);
  const error = requestError ? "병원 후기를 불러오지 못했습니다." : actionError;

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (pendingAction) return;
    setPendingAction("create"); setActionError(null); setNotice(null);
    try { await hospitalReviewApi.create({ ...form, hospitalName: form.hospitalName.trim(), address: form.address.trim(), body: form.body.trim(), rating: Number(form.rating) }); setForm({ hospitalName: "", address: "", rating: "5", body: "" }); setNotice("후기를 등록했습니다."); retry(); }
    catch (requestError) { if (requestError instanceof ApiError && requestError.status === 401) navigate("/login?next=/hospital-reviews"); else setActionError(requestError instanceof ApiError && requestError.status === 409 ? "같은 병원에는 한 번만 후기를 작성할 수 있습니다." : "후기를 등록하지 못했습니다."); }
    finally { setPendingAction(null); }
  }
  async function flag(id: string) {
    if (pendingAction) return;
    setPendingAction(`flag:${id}`); setActionError(null); setNotice(null);
    try { await hospitalReviewApi.flag(id, { reason: "후기 검토 요청" }); setNotice("검토 요청을 접수했습니다."); }
    catch (requestError) { if (requestError instanceof ApiError && requestError.status === 401) navigate("/login?next=/hospital-reviews"); else setActionError(requestError instanceof ApiError && requestError.status === 409 ? "이미 검토 요청된 후기입니다." : "검토 요청을 접수하지 못했습니다."); }
    finally { setPendingAction(null); }
  }
  return <main className="page localcare-page"><section className="localcare-hero"><p className="eyebrow">HOSPITAL REVIEWS</p><h1>동물병원 후기</h1><p>병원명과 주소를 함께 확인하고 반려생활 경험을 나눠 보세요.</p></section><form className="search-panel" onSubmit={(event) => { event.preventDefault(); setSubmittedQuery(query.trim()); }}><label><span>병원 검색</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="병원명을 입력해 주세요" /></label><button className="button button-primary" type="submit">검색</button></form>{error ? <p role="alert">{error}</p> : null}{notice ? <p role="status">{notice}</p> : null}<section className="localcare-grid" aria-label="병원 후기 목록" aria-busy={loading}>{(items ?? []).map((item) => <article className="surface-card localcare-card" key={item.id}><span className="publication-chip publication-chip-primary">{"★".repeat(item.rating)}</span><h2>{item.hospitalName}</h2><small>{item.address}</small><p>{item.body}</p><button className="button button-soft" disabled={Boolean(pendingAction)} type="button" onClick={() => void flag(item.id)}>{pendingAction === `flag:${item.id}` ? "접수 중..." : "검토 요청"}</button></article>)}</section>{!items?.length && !error && !loading ? <p className="surface-card">검색 결과가 없습니다.</p> : null}<form className="surface-card publication-fields" onSubmit={submit}><h2>후기 작성</h2><label>병원명<input required value={form.hospitalName} onChange={e => setForm({...form,hospitalName:e.target.value})}/></label><label>주소<input required value={form.address} onChange={e => setForm({...form,address:e.target.value})}/></label><label>평점<select value={form.rating} onChange={e => setForm({...form,rating:e.target.value})}>{[1,2,3,4,5].map(n => <option key={n}>{n}</option>)}</select></label><label>후기<textarea required value={form.body} onChange={e => setForm({...form,body:e.target.value})}/></label><button className="button button-primary" disabled={pendingAction === "create"} type="submit">{pendingAction === "create" ? "등록 중..." : "후기 등록"}</button></form></main>;
}
