import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { adminModerationApi, ApiError, apiFetch, getCsrfToken } from "./api/client";
import { useAbortableRequest } from "./hooks/useAbortableRequest";

type CaseItem = { id: string; caseType: string; targetType: string; targetId: string | null; subject: string; detail: string | null; status: string; createdAt: string; resolvedAt: string | null };
const labels: Record<string, string> = { "care-feedbacks": "돌봄 feedback", "hospital-review-flags": "병원 review flag", "moderation/direct": "직접 moderation" };

export default function AdminModeratorCasePage({ initialQueue }: { initialQueue?: string }) {
  const { queue: routeQueue } = useParams();
  const queue = routeQueue ?? initialQueue ?? "care-feedbacks";
  const navigate = useNavigate();
  const { data: items, error: requestError, loading, retry } = useAbortableRequest<CaseItem[]>((signal) => apiFetch<CaseItem[]>(`/api/admin/${encodeURIComponent(queue)}`, { signal }), [queue]);
  const [memberId, setMemberId] = useState("");
  const [reason, setReason] = useState("");
  const [pendingCaseId, setPendingCaseId] = useState<string | null>(null);
  const [pendingMemberAction, setPendingMemberAction] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const error = requestError instanceof ApiError && [401, 403].includes(requestError.status)
    ? "운영자 권한이 필요합니다."
    : requestError
      ? "운영 case를 불러오지 못했습니다."
      : actionError;

  useEffect(() => {
    if (requestError instanceof ApiError && [401, 403].includes(requestError.status)) navigate("/", { replace: true });
  }, [navigate, requestError]);

  const review = async (id: string, status: "REVIEWED" | "DISMISSED") => {
    if (pendingCaseId) return;
    setPendingCaseId(id);
    setActionError(null);
    setNotice(null);
    try {
      await getCsrfToken();
      const updated = await apiFetch<CaseItem>(`/api/admin/${encodeURIComponent(queue)}/${encodeURIComponent(id)}`, { method: "PATCH", headers: { "content-type": "application/json" }, body: JSON.stringify({ status }) });
      void updated;
      retry();
    } catch (requestError) {
      if (requestError instanceof ApiError && [401, 403].includes(requestError.status)) navigate("/", { replace: true });
      else setActionError("운영 case 상태를 변경하지 못했습니다.");
    } finally {
      setPendingCaseId(null);
    }
  };

  const memberAction = async (action: "sanction" | "hide-content" | "restore-content") => {
    if (!memberId.trim() || pendingMemberAction) return;
    setPendingMemberAction(action);
    setActionError(null);
    setNotice(null);
    try {
      await adminModerationApi.memberAction(action, memberId.trim(), reason.trim() || "운영자 판단");
      setNotice("회원 운영 조치를 완료했습니다.");
    } catch (requestError) {
      setActionError(requestError instanceof ApiError && requestError.status === 404 ? "회원을 찾을 수 없습니다." : "회원 조치를 처리하지 못했습니다.");
    } finally {
      setPendingMemberAction(null);
    }
  };

  return <main className="page notification-page"><section className="localcare-hero"><p className="eyebrow">MODERATOR CASE QUEUE</p><h1>{labels[queue] ?? "운영 case"}</h1><p>운영 판단이 필요한 제한된 case를 검토합니다.</p></section>{error ? <p role="alert">{error}</p> : null}{notice ? <p role="status">{notice}</p> : null}<section className="surface-card publication-fields"><h2>회원 운영 조치</h2><label>회원 ID<input value={memberId} onChange={(e) => setMemberId(e.target.value)} placeholder="UUID" /></label><label>사유<input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="조치 근거" /></label><div className="profile-actions"><button className="button button-danger" type="button" disabled={!memberId.trim() || Boolean(pendingMemberAction)} onClick={() => void memberAction("sanction")}>{pendingMemberAction === "sanction" ? "처리 중..." : "계정 제재"}</button><button className="button button-soft" type="button" disabled={!memberId.trim() || Boolean(pendingMemberAction)} onClick={() => void memberAction("hide-content")}>{pendingMemberAction === "hide-content" ? "처리 중..." : "콘텐츠 숨김"}</button><button className="button button-soft" type="button" disabled={!memberId.trim() || Boolean(pendingMemberAction)} onClick={() => void memberAction("restore-content")}>{pendingMemberAction === "restore-content" ? "처리 중..." : "콘텐츠 복구"}</button></div></section><section className="notification-list" aria-busy={loading}>{(items ?? []).map((item) => <article className="surface-card notification-item" key={item.id}><span className="publication-chip">{item.status}</span><h2>{item.subject}</h2><p>{item.detail ?? "상세 설명 없음"}</p><small>{item.targetType} · {item.targetId ?? "대상 없음"} · {new Date(item.createdAt).toLocaleString("ko-KR")}</small>{item.status === "OPEN" ? <div className="profile-actions"><button className="button button-primary" type="button" disabled={pendingCaseId === item.id || Boolean(pendingCaseId)} onClick={() => void review(item.id, "REVIEWED")}>{pendingCaseId === item.id ? "처리 중..." : "검토 완료"}</button><button className="button button-soft" type="button" disabled={pendingCaseId === item.id || Boolean(pendingCaseId)} onClick={() => void review(item.id, "DISMISSED")}>기각</button></div> : null}</article>)}{!items?.length && !error && !loading ? <p>현재 대기 중인 case가 없습니다.</p> : null}</section><Link className="publication-text-link" to="/admin">운영 콘솔로</Link></main>;
}
