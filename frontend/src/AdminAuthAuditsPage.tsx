import { useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, apiFetch } from "./api/client";
import { useAbortableRequest } from "./hooks/useAbortableRequest";
import { formatDateTime } from "./utils/date";

type AuthAudit = { memberId: string; action: string; createdAt: string };
export default function AdminAuthAuditsPage() {
  const navigate = useNavigate();
  const { data: items, error: requestError, loading } = useAbortableRequest<AuthAudit[]>((signal) => apiFetch<AuthAudit[]>("/api/admin/auth-audits", { signal }), []);
  useEffect(() => { if (requestError instanceof ApiError && [401, 403].includes(requestError.status)) navigate("/", { replace: true }); }, [navigate, requestError]);
  const error = requestError ? "인증 감사 기록을 불러오지 못했습니다." : null;
  return <main className="page notification-page"><section className="localcare-hero"><h1>인증 감사 기록</h1><a className="button button-soft" href="/api/admin/auth-audits/export">CSV 내보내기</a></section>{error ? <p role="alert">{error}</p> : null}<section className="notification-list" aria-label="인증 감사 목록" aria-busy={loading}>{(items ?? []).map((item) => <article className="surface-card notification-item" key={`${item.memberId}-${item.createdAt}-${item.action}`}><span className="publication-chip">{item.action}</span><h2>{item.memberId}</h2><small>{formatDateTime(item.createdAt)}</small></article>)}{!items?.length && !error && !loading ? <p>기록 없음</p> : null}</section><Link className="publication-text-link" to="/admin">운영 콘솔</Link></main>;
}
