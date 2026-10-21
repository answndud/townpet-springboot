import { type FormEvent, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AdminComment, adminModerationApi, ApiError, apiFetch } from "./api/client";
import { useAbortableRequest } from "./hooks/useAbortableRequest";
import { formatDateTime } from "./utils/date";

type Filters = { publicationId: string; authorType: string; lifecycle: string; from: string; until: string };

const emptyFilters: Filters = { publicationId: "", authorType: "", lifecycle: "", from: "", until: "" };

function listUrl(filters: Filters) {
  const search = new URLSearchParams({ limit: "50" });
  if (filters.publicationId.trim()) search.set("publicationId", filters.publicationId.trim());
  if (filters.authorType) search.set("authorType", filters.authorType);
  if (filters.lifecycle) search.set("lifecycle", filters.lifecycle);
  if (filters.from) search.set("from", new Date(filters.from).toISOString());
  if (filters.until) search.set("until", new Date(filters.until).toISOString());
  return `/api/admin/comments?${search}`;
}

export default function AdminCommentModerationPage() {
  const navigate = useNavigate();
  const [draft, setDraft] = useState<Filters>(emptyFilters);
  const [query, setQuery] = useState<Filters>(emptyFilters);
  const [reason, setReason] = useState("");
  const [pendingId, setPendingId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const requestUrl = listUrl(query);
  const { data: comments, error: requestError, loading, retry } = useAbortableRequest<AdminComment[]>(
    (signal) => apiFetch<AdminComment[]>(requestUrl, { signal }),
    [requestUrl],
  );

  useEffect(() => {
    if (requestError instanceof ApiError && [401, 403].includes(requestError.status)) navigate("/", { replace: true });
  }, [navigate, requestError]);

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setQuery(draft);
  }

  async function moderate(comment: AdminComment, action: "DELETE" | "RESTORE") {
    if (!reason.trim() || pendingId) return;
    setPendingId(comment.id);
    setActionError(null);
    setNotice(null);
    try {
      await adminModerationApi.moderateComment(comment.id, action, reason.trim());
      setNotice(action === "DELETE" ? "댓글을 운영자 조치로 삭제했습니다." : "댓글을 복구했습니다.");
      retry();
    } catch (error) {
      if (error instanceof ApiError && [401, 403].includes(error.status)) navigate("/", { replace: true });
      else setActionError("댓글 상태 변경 실패");
    } finally {
      setPendingId(null);
    }
  }

  const pageError = requestError ? "댓글을 불러오지 못했습니다." : actionError;
  return (
    <main className="page notification-page">
      <section className="localcare-hero">
        <h1>댓글 운영 관리</h1>
      </section>
      {pageError ? <p role="alert">{pageError}</p> : null}
      {notice ? <p role="status">{notice}</p> : null}
      <form className="surface-card publication-fields" onSubmit={applyFilters}>
        <h2>댓글 찾기</h2>
        <label>게시글 ID<input value={draft.publicationId} onChange={(event) => setDraft({ ...draft, publicationId: event.target.value })} inputMode="text" /></label>
        <label>작성자 유형<select value={draft.authorType} onChange={(event) => setDraft({ ...draft, authorType: event.target.value })}><option value="">전체</option><option value="MEMBER">회원</option><option value="GUEST">비회원</option></select></label>
        <label>상태<select value={draft.lifecycle} onChange={(event) => setDraft({ ...draft, lifecycle: event.target.value })}><option value="">전체</option><option value="ACTIVE">공개</option><option value="DELETED">작성자 삭제</option><option value="MODERATOR_DELETED">운영자 삭제</option></select></label>
        <label>작성 시작<input type="datetime-local" value={draft.from} onChange={(event) => setDraft({ ...draft, from: event.target.value })} /></label>
        <label>작성 종료<input type="datetime-local" value={draft.until} onChange={(event) => setDraft({ ...draft, until: event.target.value })} /></label>
        <div className="profile-actions"><button className="button button-primary" type="submit">검색</button><button className="button button-soft" type="button" onClick={() => { setDraft(emptyFilters); setQuery(emptyFilters); }}>초기화</button></div>
      </form>
      <label className="surface-card">운영 조치 사유<input value={reason} onChange={(event) => setReason(event.target.value)} maxLength={500} required /></label>
      <section className="notification-list" aria-label="댓글 목록" aria-busy={loading}>
        {(comments ?? []).map((comment) => (
          <article className="surface-card notification-item" key={comment.id}>
            <div className="publication-comment-meta"><span className="publication-chip">{comment.authorType === "GUEST" ? "비회원" : "회원"}</span><span>{comment.lifecycle === "ACTIVE" ? "공개" : comment.lifecycle === "DELETED" ? "작성자 삭제" : "운영자 삭제"}</span><time dateTime={comment.createdAt}>{formatDateTime(comment.createdAt)}</time></div>
            <p>{comment.body}</p>
            <small>{comment.parentCommentId ? "답글" : "댓글"} · {comment.id}</small>
            <div className="profile-actions"><Link to={`/posts/${comment.publicationId}`}>게시글 맥락 보기</Link>{comment.lifecycle === "ACTIVE" ? <button className="button button-danger" type="button" disabled={!reason.trim() || Boolean(pendingId)} onClick={() => void moderate(comment, "DELETE")}>{pendingId === comment.id ? "처리 중..." : "운영자 삭제"}</button> : comment.lifecycle === "MODERATOR_DELETED" ? <button className="button button-soft" type="button" disabled={!reason.trim() || Boolean(pendingId)} onClick={() => void moderate(comment, "RESTORE")}>{pendingId === comment.id ? "처리 중..." : "복구"}</button> : null}</div>
          </article>
        ))}
        {!comments?.length && !pageError && !loading ? <p className="surface-card">댓글 없음</p> : null}
      </section>
      <Link className="publication-text-link" to="/admin">운영 콘솔로</Link>
    </main>
  );
}
