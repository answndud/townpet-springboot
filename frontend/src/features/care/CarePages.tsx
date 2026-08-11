import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError, careApi, memberApi, type CareRequest } from "../../api/client";

const statusLabel = { OPEN: "모집 중", MATCHED: "매칭됨", CANCELLED: "취소", EXPIRED: "만료" } as const;

export function CareListPage() {
  const [items, setItems] = useState<CareRequest[]>([]);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => { const controller = new AbortController(); careApi.list(controller.signal).then(setItems).catch(() => setError("돌봄 요청을 불러오지 못했습니다.")); return () => controller.abort(); }, []);
  async function apply(item: CareRequest) {
    const message = window.prompt("지원 메시지를 입력해 주세요.");
    if (!message?.trim()) return;
    try { await careApi.apply(item.id, message.trim()); window.alert("지원이 등록되었습니다."); } catch (requestError) { setError(requestError instanceof ApiError && requestError.status === 401 ? "로그인 후 지원할 수 있습니다." : "지원하지 못했습니다."); }
  }
  return <main className="page localcare-page"><section className="localcare-hero"><p className="eyebrow">NEIGHBOR CARE</p><h1>이웃 돌봄 요청</h1><p>결제나 지급을 보장하지 않는 참고 reward로 안전하게 요청을 확인하세요.</p><Link className="button button-primary" to="/care/new">돌봄 요청 작성</Link></section>{error ? <p role="alert">{error}</p> : null}<section className="localcare-grid" aria-label="돌봄 요청 목록">{items.map((item) => <Link className="surface-card localcare-card" to={`/care/${item.id}`} key={item.id}><span className="publication-chip publication-chip-primary">{statusLabel[item.status]}</span><h2>{item.title}</h2><p>{item.description}</p><small>{item.location} · {new Date(item.startsAt).toLocaleString("ko-KR")}</small>{item.rewardHint ? <small>참고 reward: {item.rewardHint}</small> : null}</Link>)}{!items.length && !error ? <p className="surface-card localcare-empty">현재 열린 돌봄 요청이 없습니다.</p> : null}</section></main>;
}

export function CareDetailPage() {
  const { requestId = "" } = useParams();
  const [request, setRequest] = useState<CareRequest | null>(null);
  const [applications, setApplications] = useState<import("../../api/client").CareApplication[]>([]);
  const [viewerId, setViewerId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState("");
  useEffect(() => { const controller = new AbortController(); Promise.all([careApi.detail(requestId, controller.signal), memberApi.current(controller.signal).catch(() => null)]).then(([loaded, viewer]) => { setRequest(loaded); setViewerId(viewer?.id ?? null); if (viewer?.id === loaded.requesterMemberId) careApi.applications(requestId, controller.signal).then(setApplications).catch(() => undefined); }).catch(() => setError("돌봄 요청을 불러오지 못했습니다.")); return () => controller.abort(); }, [requestId]);
  if (error) return <main className="page placeholder-page"><section className="surface-card"><p role="alert">{error}</p><Link className="button button-soft" to="/care">돌봄 목록</Link></section></main>;
  if (!request) return <main className="page placeholder-page"><section className="surface-card" role="status">돌봄 요청을 불러오는 중...</section></main>;
  const owner = viewerId === request.requesterMemberId;
  async function apply() { if (!message.trim()) return; try { await careApi.apply(requestId, message.trim()); setMessage(""); setError("지원이 등록되었습니다."); } catch (requestError) { setError(requestError instanceof ApiError && requestError.status === 401 ? "로그인 후 지원할 수 있습니다." : "지원할 수 없습니다."); } }
  async function accept(application: import("../../api/client").CareApplication) { try { await careApi.accept(requestId, application.id, application.version); setError("돌봄 제공자를 수락했습니다."); } catch { setError("지원자를 수락하지 못했습니다."); } }
  return <main className="page localcare-page"><Link className="publication-text-link" to="/care">← 돌봄 목록</Link><article className="surface-card localcare-detail"><span className="publication-chip publication-chip-primary">{statusLabel[request.status]}</span><h1>{request.title}</h1><p className="localcare-summary">{request.description}</p><div className="localcare-content"><p>장소: {request.location}</p><p>시간: {new Date(request.startsAt).toLocaleString("ko-KR")} ~ {new Date(request.endsAt).toLocaleString("ko-KR")}</p>{request.rewardHint ? <p>참고 reward: {request.rewardHint}</p> : null}</div>{!owner && request.status === "OPEN" ? <section className="surface-card"><label>지원 메시지<textarea value={message} onChange={(event) => setMessage(event.target.value)} /></label><button className="button button-primary" type="button" onClick={() => void apply()}>지원하기</button></section> : null}{owner ? <section><h2>지원자 {applications.length}명</h2>{applications.map((application) => <article className="surface-card notification-item" key={application.id}><span className="publication-chip">{application.status}</span><p>{application.message}</p>{application.status === "PENDING" ? <button className="button button-primary" type="button" onClick={() => void accept(application)}>수락</button> : null}</article>)}</section> : null}{error ? <p role="alert">{error}</p> : null}</article></main>;
}

export function CareCreatePage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ title: "", description: "", location: "", startsAt: "", endsAt: "", rewardHint: "" });
  const [error, setError] = useState<string | null>(null);
  async function submit(event: FormEvent) { event.preventDefault(); try { await memberApi.current(); await careApi.create({ ...form, startsAt: new Date(form.startsAt).toISOString(), endsAt: new Date(form.endsAt).toISOString() }); navigate("/care"); } catch (requestError) { if (requestError instanceof ApiError && requestError.status === 401) navigate("/login?next=/care/new"); else setError("돌봄 요청 내용을 확인해 주세요."); } }
  return <main className="page localcare-page"><section className="localcare-hero"><p className="eyebrow">CARE REQUEST</p><h1>돌봄 요청 작성</h1></section><form className="surface-card publication-fields" onSubmit={submit}><label>제목<input required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></label><label>설명<textarea required value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label><label>장소<input required value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} /></label><label>시작 시각<input required type="datetime-local" value={form.startsAt} onChange={(e) => setForm({ ...form, startsAt: e.target.value })} /></label><label>종료 시각<input required type="datetime-local" value={form.endsAt} onChange={(e) => setForm({ ...form, endsAt: e.target.value })} /></label><label>참고 reward<input value={form.rewardHint} onChange={(e) => setForm({ ...form, rewardHint: e.target.value })} /></label>{error ? <p role="alert">{error}</p> : null}<button className="button button-primary" type="submit">등록</button></form></main>;
}
