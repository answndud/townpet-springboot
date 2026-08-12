import { useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, volunteerApi, type VolunteerOpportunity } from "./api/client";
import { useAbortableRequest } from "./hooks/useAbortableRequest";
import { formatDateTime } from "./utils/date";

export default function VolunteerPage() {
  const { data: items, error: requestError, loading } = useAbortableRequest<VolunteerOpportunity[]>((signal) => volunteerApi.list(signal), []);
  const [pendingId, setPendingId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const error = requestError ? "봉사 기회를 불러오지 못했습니다." : actionError;
  async function apply(item: VolunteerOpportunity) {
    if (pendingId) return;
    const message = window.prompt("신청 메시지를 입력해 주세요.");
    if (!message?.trim()) return;
    setPendingId(item.id); setActionError(null); setNotice(null);
    try { await volunteerApi.apply(item.id, message.trim()); setNotice("봉사 신청이 접수되었습니다."); }
    catch (requestError) { setActionError(requestError instanceof ApiError && requestError.status === 401 ? "로그인 후 신청할 수 있습니다." : "이미 신청했거나 신청할 수 없습니다."); }
    finally { setPendingId(null); }
  }
  return <main className="page localcare-page"><section className="localcare-hero"><p className="eyebrow">VOLUNTEER</p><h1>반려동물 봉사 기회</h1><p>합성 demo 기반 봉사 기회를 확인하고 신청하세요.</p></section>{error ? <p role="alert">{error}</p> : null}{notice ? <p role="status">{notice}</p> : null}<section className="localcare-grid" aria-label="봉사 기회 목록" aria-busy={loading}>{(items ?? []).map((item) => <article className="surface-card localcare-card" key={item.id}><span className="publication-chip publication-chip-primary">{item.status}</span><h2>{item.title}</h2><p>{item.description}</p><small>{item.organization} · {item.location}</small><small>{formatDateTime(item.startsAt)} · 정원 {item.capacity}명</small><button className="button button-soft" disabled={Boolean(pendingId)} type="button" onClick={() => void apply(item)}>{pendingId === item.id ? "신청 중..." : "신청하기"}</button></article>)}</section>{!items?.length && !error && !loading ? <p className="surface-card">현재 봉사 기회가 없습니다.</p> : null}<Link className="publication-text-link" to="/">홈으로</Link></main>;
}
