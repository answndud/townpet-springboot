import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, apiFetch } from "./api/client";

type Report = { id: string; targetType: string; targetId: string; reason: string; detail: string | null; status: string; createdAt: string };
export default function AdminReportsPage() {
  const navigate = useNavigate(); const [items, setItems] = useState<Report[]>([]); const [error, setError] = useState<string | null>(null);
  useEffect(() => { apiFetch<Report[]>("/api/admin/reports").then(setItems).catch((e: unknown) => { if (e instanceof ApiError && [401, 403].includes(e.status)) navigate("/", { replace: true }); else setError("신고 큐를 불러오지 못했습니다."); }); }, [navigate]);
  return <main className="page notification-page"><section className="localcare-hero"><p className="eyebrow">MODERATION</p><h1>신고 큐</h1><p>운영자가 접수된 신고를 검토합니다.</p></section>{error ? <p role="alert">{error}</p> : null}<section className="notification-list">{items.map((item) => <article className="surface-card notification-item" key={item.id}><span className="publication-chip">{item.reason}</span><h2>{item.targetType} · {item.targetId}</h2><p>{item.detail ?? "상세 설명 없음"}</p><small>{new Date(item.createdAt).toLocaleString("ko-KR")} · {item.status}</small></article>)}</section><Link className="publication-text-link" to="/">홈으로</Link></main>;
}
