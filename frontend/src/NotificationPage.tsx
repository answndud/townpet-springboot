import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ApiError, notificationApi, type Notification } from "./api/client";
import { useAbortableRequest } from "./hooks/useAbortableRequest";
import { formatDateTime } from "./utils/date";

export default function NotificationPage() {
  const navigate = useNavigate();
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [readingId, setReadingId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [readUpdates, setReadUpdates] = useState<Record<string, Notification>>({});
  const { data: loaded, error: requestError, loading } = useAbortableRequest<{ items: Notification[]; unreadCount: number }>(
    (signal) => Promise.all([notificationApi.list(unreadOnly, signal), notificationApi.unreadCount(signal)]).then(([items, count]) => ({ items, unreadCount: count.count })),
    [unreadOnly],
  );
  const items = (loaded?.items ?? []).map((item) => readUpdates[item.id] ?? item);
  const unreadCount = Math.max(0, (loaded?.unreadCount ?? 0) - Object.keys(readUpdates).filter((id) => loaded?.items.some((item) => item.id === id && !item.readAt)).length);
  const error = requestError instanceof ApiError && requestError.status === 401
    ? "로그인이 필요합니다."
    : requestError
      ? "알림을 불러오지 못했습니다."
      : actionError;

  useEffect(() => {
    if (requestError instanceof ApiError && requestError.status === 401) navigate("/login?next=/notifications", { replace: true });
  }, [navigate, requestError]);

  async function read(item: Notification) {
    if (item.readAt || readingId) return;
    setReadingId(item.id);
    setActionError(null);
    try {
      const updated = await notificationApi.markRead(item.id);
      setReadUpdates((current) => ({ ...current, [item.id]: updated }));
      setActionError(null);
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) navigate("/login?next=/notifications", { replace: true });
      else setActionError("알림을 읽음 처리하지 못했습니다.");
    } finally {
      setReadingId(null);
    }
  }

  return <main className="page notification-page"><section className="localcare-hero"><p className="eyebrow">NOTIFICATIONS</p><h1>알림 {unreadCount ? <span className="publication-chip publication-chip-primary">읽지 않음 {unreadCount}</span> : null}</h1><p>내 활동과 TownPet 운영 소식을 확인하세요.</p></section>{error ? <p role="alert">{error}</p> : null}<button className="button button-soft" type="button" onClick={() => setUnreadOnly((current) => !current)}>{unreadOnly ? "전체 알림 보기" : "읽지 않은 알림만"}</button><section className="notification-list" aria-busy={loading}>{items.map((item) => <button className={item.readAt ? "surface-card notification-item read" : "surface-card notification-item"} key={item.id} type="button" disabled={readingId === item.id} onClick={() => void read(item)}><span className="publication-chip">{item.type}</span><h2>{item.title}</h2><p>{item.body}</p><small>{formatDateTime(item.createdAt)}</small></button>)}</section>{!items.length && !error && !loading ? <p className="surface-card">새 알림이 없습니다.</p> : null}</main>;
}
