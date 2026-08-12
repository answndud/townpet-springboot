import { FormEvent, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError, careApi, type CareApplication, type CareAssignment, type CareRequest } from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import { useAbortableRequest } from "../../hooks/useAbortableRequest";

const statusLabel = { OPEN: "모집 중", MATCHED: "매칭됨", CANCELLED: "취소", EXPIRED: "만료" } as const;

export function CareListPage() {
  const { member } = useAuth();
  const { data: items, error: requestError, loading } = useAbortableRequest<CareRequest[]>((signal) => careApi.list(signal), []);
  const error = requestError ? "돌봄 요청을 불러오지 못했습니다." : null;
  const requests = items ?? [];
  return <main className="page localcare-page"><section className="localcare-hero"><p className="eyebrow">NEIGHBOR CARE</p><h1>이웃 돌봄 요청</h1><p>결제나 지급을 보장하지 않는 참고 reward로 안전하게 요청을 확인하세요.</p>{member?.role !== "MODERATOR" ? <Link className="button button-primary" to="/care/new">돌봄 요청 작성</Link> : null}</section>{error ? <p role="alert">{error}</p> : null}<section className="localcare-grid" aria-label="돌봄 요청 목록" aria-busy={loading}>{requests.map((item) => <Link className="surface-card localcare-card" to={`/care/${item.id}`} key={item.id}><span className="publication-chip publication-chip-primary">{statusLabel[item.status]}</span><h2>{item.title}</h2><p>{item.description}</p><small>{item.location} · {new Date(item.startsAt).toLocaleString("ko-KR")}</small>{item.rewardHint ? <small>참고 reward: {item.rewardHint}</small> : null}</Link>)}{!requests.length && !error && !loading ? <p className="surface-card localcare-empty">현재 열린 돌봄 요청이 없습니다.</p> : null}</section></main>;
}

export function CareDetailPage() {
  const { requestId = "" } = useParams();
  const navigate = useNavigate();
  const { member } = useAuth();
  const { data, error: requestError, loading, retry } = useAbortableRequest<{ request: CareRequest; applications: CareApplication[]; assignment: CareAssignment | null }>(async (signal) => {
    const request = await careApi.detail(requestId, signal);
    const [assignment, applications] = await Promise.all([
      careApi.assignment(requestId, signal).catch(() => null),
      member?.role === "MEMBER" && member.id === request.requesterMemberId
        ? careApi.applications(requestId, signal).catch(() => [])
        : Promise.resolve([] as CareApplication[]),
    ]);
    return { request, assignment, applications };
  }, [requestId, member?.id, member?.role]);
  const [message, setMessage] = useState("");
  const [pendingAction, setPendingAction] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const error = requestError instanceof ApiError && requestError.status === 404 ? "돌봄 요청을 찾을 수 없습니다." : requestError ? "돌봄 요청을 불러오지 못했습니다." : actionError;
  const request = data?.request ?? null;
  const applications = data?.applications ?? [];
  const assignment = data?.assignment ?? null;

  async function apply() {
    if (!message.trim() || pendingAction) return;
    setPendingAction("apply"); setActionError(null); setNotice(null);
    try { await careApi.apply(requestId, message.trim()); setMessage(""); setNotice("지원이 등록되었습니다."); }
    catch (requestError) { setActionError(requestError instanceof ApiError && requestError.status === 401 ? "로그인 후 지원할 수 있습니다." : "지원할 수 없습니다."); }
    finally { setPendingAction(null); }
  }
  async function accept(application: CareApplication) {
    if (pendingAction) return;
    setPendingAction(`accept:${application.id}`); setActionError(null); setNotice(null);
    try { await careApi.accept(requestId, application.id, application.version); setNotice("돌봄 제공자를 수락했습니다."); retry(); }
    catch { setActionError("지원자를 수락하지 못했습니다."); }
    finally { setPendingAction(null); }
  }
  async function transition(status: CareAssignment["status"]) {
    if (!assignment || pendingAction) return;
    setPendingAction(status); setActionError(null); setNotice(null);
    try { await careApi.transition(assignment.id, status, assignment.version); setNotice("돌봄 상태를 변경했습니다."); retry(); }
    catch { setActionError("돌봄 상태를 변경하지 못했습니다."); }
    finally { setPendingAction(null); }
  }
  async function leaveFeedback() {
    if (!assignment || pendingAction) return;
    const body = window.prompt("돌봄 후기를 입력해 주세요.");
    if (!body?.trim()) return;
    setPendingAction("feedback"); setActionError(null); setNotice(null);
    try { await careApi.feedback(assignment.id, body.trim()); setNotice("후기를 등록했습니다."); }
    catch { setActionError("후기를 등록하지 못했습니다."); }
    finally { setPendingAction(null); }
  }

  if (error && !request) return <main className="page placeholder-page"><section className="surface-card"><p role="alert">{error}</p><Link className="button button-soft" to="/care">돌봄 목록</Link></section></main>;
  if (!request || loading) return <main className="page placeholder-page"><section className="surface-card" role="status">돌봄 요청을 불러오는 중...</section></main>;
  const owner = member?.id === request.requesterMemberId;
  return <main className="page localcare-page"><Link className="publication-text-link" to="/care">← 돌봄 목록</Link><article className="surface-card localcare-detail"><span className="publication-chip publication-chip-primary">{statusLabel[request.status]}</span><h1>{request.title}</h1><p className="localcare-summary">{request.description}</p><div className="localcare-content"><p>장소: {request.location}</p><p>시간: {new Date(request.startsAt).toLocaleString("ko-KR")} ~ {new Date(request.endsAt).toLocaleString("ko-KR")}</p>{request.rewardHint ? <p>참고 reward: {request.rewardHint}</p> : null}</div>{notice ? <p role="status">{notice}</p> : null}{error ? <p role="alert">{error}</p> : null}{member?.role === "MEMBER" && !owner && request.status === "OPEN" ? <section className="surface-card"><label>지원 메시지<textarea value={message} onChange={(event) => setMessage(event.target.value)} /></label><button className="button button-primary" disabled={pendingAction === "apply"} type="button" onClick={() => void apply()}>{pendingAction === "apply" ? "지원 중..." : "지원하기"}</button></section> : null}{owner && member?.role === "MEMBER" ? <section><h2>지원자 {applications.length}명</h2>{applications.map((application) => <article className="surface-card notification-item" key={application.id}><span className="publication-chip">{application.status}</span><p>{application.message}</p>{application.status === "PENDING" ? <button className="button button-primary" disabled={Boolean(pendingAction)} type="button" onClick={() => void accept(application)}>{pendingAction === `accept:${application.id}` ? "수락 중..." : "수락"}</button> : null}</article>)}</section> : null}{member?.role === "MEMBER" && assignment ? <section className="surface-card care-assignment"><h2>돌봄 진행</h2><span className="publication-chip publication-chip-primary">{assignment.status}</span>{assignment.status === "MATCHED" ? <button className="button button-primary" disabled={Boolean(pendingAction)} onClick={() => void transition("IN_PROGRESS")}>진행 시작</button> : null}{assignment.status === "IN_PROGRESS" ? <button className="button button-primary" disabled={Boolean(pendingAction)} onClick={() => void transition("COMPLETED")}>완료 처리</button> : null}{assignment.status === "COMPLETED" ? <button className="button button-soft" disabled={Boolean(pendingAction)} onClick={() => void leaveFeedback()}>후기 남기기</button> : null}</section> : null}</article></main>;
}

export function CareCreatePage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ title: "", description: "", location: "", startsAt: "", endsAt: "", rewardHint: "" });
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  async function submit(event: FormEvent) { event.preventDefault(); if (saving) return; setSaving(true); setError(null); try { await careApi.create({ ...form, startsAt: new Date(form.startsAt).toISOString(), endsAt: new Date(form.endsAt).toISOString() }); navigate("/care"); } catch (requestError) { if (requestError instanceof ApiError && requestError.status === 401) navigate("/login?next=/care/new"); else setError("돌봄 요청 내용을 확인해 주세요."); } finally { setSaving(false); } }
  return <main className="page localcare-page"><section className="localcare-hero"><p className="eyebrow">CARE REQUEST</p><h1>돌봄 요청 작성</h1></section><form className="surface-card publication-fields" onSubmit={submit}><label>제목<input required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></label><label>설명<textarea required value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label><label>장소<input required value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} /></label><label>시작 시각<input required type="datetime-local" value={form.startsAt} onChange={(e) => setForm({ ...form, startsAt: e.target.value })} /></label><label>종료 시각<input required type="datetime-local" value={form.endsAt} onChange={(e) => setForm({ ...form, endsAt: e.target.value })} /></label><label>참고 reward<input value={form.rewardHint} onChange={(e) => setForm({ ...form, rewardHint: e.target.value })} /></label>{error ? <p role="alert">{error}</p> : null}<button className="button button-primary" disabled={saving} type="submit">{saving ? "등록 중..." : "등록"}</button></form></main>;
}
