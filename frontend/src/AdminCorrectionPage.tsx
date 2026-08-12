import { useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, apiFetch } from "./api/client";
import { useAbortableRequest } from "./hooks/useAbortableRequest";
import { formatDateTime } from "./utils/date";
type Correction = { id: string; memberId: string; title: string; body: string; status: string; createdAt: string };
export default function AdminCorrectionPage() {
  const navigate = useNavigate();
  const { data: items, error: requestError, loading } = useAbortableRequest<Correction[]>((signal) => apiFetch<Correction[]>("/api/admin/corrections", { signal }), []);
  useEffect(() => { if (requestError instanceof ApiError && [401, 403].includes(requestError.status)) navigate("/", { replace: true }); }, [navigate, requestError]);
  const error = requestError ? "정정 요청을 불러오지 못했습니다." : null;
  return <main className="page notification-page"><section className="localcare-hero"><p className="eyebrow">CORRECTIONS</p><h1>정정 요청 큐</h1><p>회원이 보낸 정보 정정 요청을 검토합니다.</p></section>{error ? <p role="alert">{error}</p> : null}<section className="notification-list" aria-busy={loading}>{(items ?? []).map((item) => <article className="surface-card notification-item" key={item.id}><span className="publication-chip">{item.status}</span><h2>{item.title}</h2><p>{item.body}</p><small>{item.memberId} · {formatDateTime(item.createdAt)}</small></article>)}{!items?.length && !error && !loading ? <p>정정 요청이 없습니다.</p> : null}</section><Link className="publication-text-link" to="/admin">운영 콘솔로</Link></main>;
}
