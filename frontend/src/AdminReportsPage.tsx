import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { adminModerationApi, ApiError, apiFetch } from "./api/client";
import { useAbortableRequest } from "./hooks/useAbortableRequest";

type Report = { id: string; targetType: string; targetId: string; reason: string; detail: string | null; status: string; createdAt: string };

export default function AdminReportsPage() {
  const navigate = useNavigate();
  const { reportId } = useParams();
  const { data, error: requestError, loading, retry } = useAbortableRequest<Report | Report[]>((signal) => apiFetch<Report | Report[]>(reportId ? `/api/admin/reports/${encodeURIComponent(reportId)}` : "/api/admin/reports", { signal }), [reportId]);
  const [pendingAction, setPendingAction] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const detail = reportId && data && !Array.isArray(data) ? data : null;
  const content = detail ? [detail] : Array.isArray(data) ? data : [];
  const error = requestError instanceof ApiError && [401, 403].includes(requestError.status)
    ? "운영자 권한이 필요합니다."
    : requestError
      ? "신고 정보를 불러오지 못했습니다."
      : actionError;

  useEffect(() => {
    if (requestError instanceof ApiError && [401, 403].includes(requestError.status)) navigate("/", { replace: true });
  }, [navigate, requestError]);

  async function act(item: Report, status: "REVIEWED" | "REJECTED") {
    if (pendingAction) return;
    setPendingAction(`${item.id}:${status}`); setActionError(null); setNotice(null);
    try { await adminModerationApi.reviewReport(item.id, status); setNotice(status === "REVIEWED" ? "신고를 검토 완료 처리했습니다." : "신고를 기각했습니다."); retry(); }
    catch (requestError) { setActionError(requestError instanceof ApiError ? "신고 상태를 변경하지 못했습니다." : "처리 중 오류가 발생했습니다."); }
    finally { setPendingAction(null); }
  }
  async function hide(item: Report) {
    if (item.targetType !== "PUBLICATION" || pendingAction) return;
    setPendingAction(`${item.id}:hide`); setActionError(null); setNotice(null);
    try { await adminModerationApi.setPublicationVisibility(item.targetId, false, `신고 ${item.id} 검토 결과`); setNotice("게시글을 숨겼습니다."); }
    catch { setActionError("게시글을 숨기지 못했습니다."); }
    finally { setPendingAction(null); }
  }
  return <main className="page notification-page"><section className="localcare-hero"><p className="eyebrow">MODERATION</p><h1>{detail ? "신고 상세" : "신고 큐"}</h1><p>운영자가 접수된 신고를 검토합니다.</p></section>{error ? <p role="alert">{error}</p> : null}{notice ? <p role="status">{notice}</p> : null}<section className="notification-list" aria-busy={loading}>{content.map((item) => <article className="surface-card notification-item" key={item.id}><Link to={`/admin/reports/${item.id}`}><span className="publication-chip">{item.reason}</span><h2>{item.targetType} · {item.targetId}</h2><p>{item.detail ?? "상세 설명 없음"}</p><small>{new Date(item.createdAt).toLocaleString("ko-KR")} · {item.status}</small></Link>{detail && item.status === "OPEN" ? <div className="profile-actions"><button className="button button-primary" disabled={Boolean(pendingAction)} type="button" onClick={() => void act(item, "REVIEWED")}>{pendingAction === `${item.id}:REVIEWED` ? "처리 중..." : "검토 완료"}</button><button className="button button-soft" disabled={Boolean(pendingAction)} type="button" onClick={() => void act(item, "REJECTED")}>{pendingAction === `${item.id}:REJECTED` ? "처리 중..." : "기각"}</button>{item.targetType === "PUBLICATION" ? <button className="button button-danger" disabled={Boolean(pendingAction)} type="button" onClick={() => void hide(item)}>{pendingAction === `${item.id}:hide` ? "처리 중..." : "게시글 숨김"}</button> : null}</div> : null}</article>)}</section>{!content.length && !error && !loading ? <p className="surface-card">처리할 신고가 없습니다.</p> : null}<Link className="publication-text-link" to={detail ? "/admin/reports" : "/"}>{detail ? "신고 큐로" : "홈으로"}</Link></main>;
}
