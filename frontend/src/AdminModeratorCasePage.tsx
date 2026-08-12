import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { adminModerationApi, ApiError, apiFetch, getCsrfToken } from "./api/client";

type CaseItem = { id: string; caseType: string; targetType: string; targetId: string | null; subject: string; detail: string | null; status: string; createdAt: string; resolvedAt: string | null };
const labels: Record<string, string> = { "care-feedbacks": "돌봄 feedback", "hospital-review-flags": "병원 review flag", "moderation/direct": "직접 moderation" };

export default function AdminModeratorCasePage() {
  const { queue = "care-feedbacks" } = useParams();
  const navigate = useNavigate();
  const [items, setItems] = useState<CaseItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [memberId, setMemberId] = useState("");
  const [reason, setReason] = useState("");
  useEffect(() => { apiFetch<CaseItem[]>(`/api/admin/${queue}`).then(setItems).catch((e: unknown) => { if (e instanceof ApiError && [401, 403].includes(e.status)) navigate("/", { replace: true }); else setError("운영 case를 불러오지 못했습니다."); }); }, [navigate, queue]);
  const review = async (id: string, status: "REVIEWED" | "DISMISSED") => { await getCsrfToken(); const updated = await apiFetch<CaseItem>(`/api/admin/${queue}/${id}`, { method: "PATCH", headers: { "content-type": "application/json" }, body: JSON.stringify({ status }) }); setItems((current) => current.map((item) => item.id === id ? updated : item)); };
  const memberAction = async (action: "sanction" | "hide-content" | "restore-content") => { try { await adminModerationApi.memberAction(action, memberId.trim(), reason.trim() || "운영자 판단"); setError("회원 운영 조치를 완료했습니다."); } catch (e) { setError(e instanceof ApiError && e.status === 404 ? "회원을 찾을 수 없습니다." : "회원 조치를 처리하지 못했습니다."); } };
  return <main className="page notification-page"><section className="localcare-hero"><p className="eyebrow">MODERATOR CASE QUEUE</p><h1>{labels[queue] ?? "운영 case"}</h1><p>운영 판단이 필요한 제한된 case를 검토합니다.</p></section>{error ? <p role="alert">{error}</p> : null}<section className="surface-card publication-fields"><h2>회원 운영 조치</h2><label>회원 ID<input value={memberId} onChange={(e) => setMemberId(e.target.value)} placeholder="UUID" /></label><label>사유<input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="조치 근거" /></label><div className="profile-actions"><button className="button button-danger" type="button" disabled={!memberId.trim()} onClick={() => void memberAction("sanction")}>계정 제재</button><button className="button button-soft" type="button" disabled={!memberId.trim()} onClick={() => void memberAction("hide-content")}>콘텐츠 숨김</button><button className="button button-soft" type="button" disabled={!memberId.trim()} onClick={() => void memberAction("restore-content")}>콘텐츠 복구</button></div></section><section className="notification-list">{items.map((item) => <article className="surface-card notification-item" key={item.id}><span className="publication-chip">{item.status}</span><h2>{item.subject}</h2><p>{item.detail ?? "상세 설명 없음"}</p><small>{item.targetType} · {item.targetId ?? "대상 없음"} · {new Date(item.createdAt).toLocaleString("ko-KR")}</small>{item.status === "OPEN" ? <div className="profile-actions"><button className="button button-primary" onClick={() => void review(item.id, "REVIEWED")}>검토 완료</button><button className="button button-soft" onClick={() => void review(item.id, "DISMISSED")}>기각</button></div> : null}</article>)}{!items.length ? <p>현재 대기 중인 case가 없습니다.</p> : null}</section><Link className="publication-text-link" to="/admin">운영 콘솔로</Link></main>;
}
