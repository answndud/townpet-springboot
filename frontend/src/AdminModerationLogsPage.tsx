import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, apiFetch } from "./api/client";

type Log = { id: string; actorMemberId: string; targetMemberId: string | null; targetType: string; targetId: string | null; action: string; reason: string | null; createdAt: string };

export default function AdminModerationLogsPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<Log[]>([]);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    apiFetch<Log[]>("/api/admin/moderation-logs")
      .then(setItems)
      .catch((requestError: unknown) => {
        if (requestError instanceof ApiError && [401, 403].includes(requestError.status)) navigate("/", { replace: true });
        else setError("moderation log를 불러오지 못했습니다.");
      });
  }, [navigate]);
  return <main className="page notification-page"><section className="localcare-hero"><p className="eyebrow">MODERATION LOG</p><h1>moderation log</h1><p>운영자 조치의 대상과 사유를 확인합니다.</p></section>{error ? <p role="alert">{error}</p> : null}<section className="notification-list">{items.map((item) => <article className="surface-card notification-item" key={item.id}><span className="publication-chip">{item.action}</span><h2>{item.targetType} · {item.targetId ?? item.targetMemberId ?? "-"}</h2><p>{item.reason ?? "사유 없음"}</p><small>{new Date(item.createdAt).toLocaleString("ko-KR")}</small></article>)}{!items.length && !error ? <p>moderation log가 없습니다.</p> : null}</section><Link className="publication-text-link" to="/admin">운영 콘솔로</Link></main>;
}
