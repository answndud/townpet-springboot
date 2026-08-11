import { type FormEvent, useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import {
  ApiError,
  guestApi,
  memberApi,
  publicationApi,
  trustApi,
  type Comment,
  type Bookmark,
  type Relationship,
  type Publication,
  type Reaction,
} from "../../api/client";

function formatDate(value: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export default function PublicationDetailPage() {
  const { publicationId = "" } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const guestView = location.pathname.endsWith("/guest");
  const [publication, setPublication] = useState<Publication | null>(null);
  const [viewerId, setViewerId] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [mutationError, setMutationError] = useState<string | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [commentsLoading, setCommentsLoading] = useState(true);
  const [commentBody, setCommentBody] = useState("");
  const [guestPassword, setGuestPassword] = useState("");
  const [guestManagePassword, setGuestManagePassword] = useState("");
  const [guestEditing, setGuestEditing] = useState(false);
  const [guestTitle, setGuestTitle] = useState("");
  const [guestBody, setGuestBody] = useState("");
  const [replyingTo, setReplyingTo] = useState<Comment | null>(null);
  const [commentSubmitting, setCommentSubmitting] = useState(false);
  const [commentError, setCommentError] = useState<string | null>(null);
  const [reaction, setReaction] = useState<Reaction>({ active: false, count: 0 });
  const [reactionLoading, setReactionLoading] = useState(true);
  const [reactionSubmitting, setReactionSubmitting] = useState(false);
  const [bookmark, setBookmark] = useState<Bookmark>({ active: false });
  const [bookmarkLoading, setBookmarkLoading] = useState(true);
  const [bookmarkSubmitting, setBookmarkSubmitting] = useState(false);
  const [relationship, setRelationship] = useState<Relationship>({ following: false, blocking: false });
  const [relationshipSubmitting, setRelationshipSubmitting] = useState(false);
  const [shareSubmitting, setShareSubmitting] = useState(false);
  const [viewCount, setViewCount] = useState<number | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    setPublication(null);
    setError(null);
    setComments([]);
    setCommentsLoading(true);
    setCommentError(null);
    setReaction({ active: false, count: 0 });
    setReactionLoading(true);
    setBookmark({ active: false });
    setBookmarkLoading(true);
    setRelationship({ following: false, blocking: false });
    publicationApi
      .detail(publicationId, controller.signal)
      .then((nextPublication) => {
        setPublication(nextPublication);
        setGuestTitle(nextPublication.title);
        setGuestBody(nextPublication.body);
        void publicationApi.view(publicationId).then((result) => setViewCount(result.viewCount)).catch(() => undefined);
      })
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        setError(
          requestError instanceof ApiError && requestError.status === 404
            ? "존재하지 않거나 삭제된 게시글입니다."
            : "게시글을 불러오지 못했습니다.",
        );
      });
    publicationApi
      .comments(publicationId, controller.signal)
      .then((result) => setComments(result.items))
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        if (!(requestError instanceof ApiError && requestError.status === 404)) {
          setCommentError("댓글을 불러오지 못했습니다.");
        }
      })
      .finally(() => setCommentsLoading(false));
    publicationApi
      .reaction(publicationId, controller.signal)
      .then(setReaction)
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
      })
      .finally(() => setReactionLoading(false));
    publicationApi
      .bookmark(publicationId, controller.signal)
      .then(setBookmark)
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
      })
      .finally(() => setBookmarkLoading(false));
    memberApi
      .current(controller.signal)
      .then((member) => setViewerId(member.id))
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        if (!(requestError instanceof ApiError && requestError.status === 401)) setViewerId(null);
      });
    return () => controller.abort();
  }, [publicationId]);

  useEffect(() => {
    if (!publication || !viewerId || viewerId === publication.authorId) return;
    const controller = new AbortController();
    publicationApi
      .relationship(publication.authorId, controller.signal)
      .then(setRelationship)
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
      });
    return () => controller.abort();
  }, [publication, viewerId]);

  async function setPublicationReaction() {
    if (!publication || reactionSubmitting) return;
    setReactionSubmitting(true);
    try {
      const next = await publicationApi.setReaction(publication.id, !reaction.active);
      setReaction(next);
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        navigate(`/login?next=/posts/${publication.id}`, { replace: true });
      }
    } finally {
      setReactionSubmitting(false);
    }
  }

  async function setPublicationBookmark() {
    if (!publication || bookmarkSubmitting) return;
    setBookmarkSubmitting(true);
    try {
      const next = await publicationApi.setBookmark(publication.id, !bookmark.active);
      setBookmark(next);
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        navigate(`/login?next=/posts/${publication.id}`, { replace: true });
      }
    } finally {
      setBookmarkSubmitting(false);
    }
  }

  async function setPublicationRelationship(next: "following" | "blocking") {
    if (!publication || relationshipSubmitting || viewerId === publication.authorId) return;
    setRelationshipSubmitting(true);
    const following = next === "following" ? !relationship.following : relationship.following;
    const blocking = next === "blocking" ? !relationship.blocking : relationship.blocking;
    try {
      setRelationship(await publicationApi.setRelationship(publication.authorId, following, blocking));
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        navigate(`/login?next=/posts/${publication.id}`, { replace: true });
      }
    } finally {
      setRelationshipSubmitting(false);
    }
  }

  async function createComment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!publication || !commentBody.trim() || commentSubmitting) return;
    setCommentSubmitting(true);
    setCommentError(null);
    try {
      const created = guestView
        ? await guestApi.createComment(publication.id, { password: guestPassword, body: commentBody.trim(), ...(replyingTo ? { parentCommentId: replyingTo.id } : {}) }) as Comment
        : await publicationApi.createComment(publication.id, { body: commentBody.trim(), ...(replyingTo ? { parentCommentId: replyingTo.id } : {}) });
      setComments((current) => [...current, created]);
      setCommentBody("");
      setReplyingTo(null);
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        navigate(`/login?next=/posts/${publication.id}#comments`, { replace: true });
      } else if (requestError instanceof ApiError && requestError.status === 404) {
        setCommentError("삭제되었거나 댓글을 작성할 수 없는 게시글입니다.");
      } else {
        setCommentError("댓글을 등록하지 못했습니다.");
      }
    } finally {
      setCommentSubmitting(false);
    }
  }

  async function deleteComment(comment: Comment) {
    if (!publication || !window.confirm("이 댓글을 삭제할까요?")) return;
    setCommentError(null);
    try {
      await publicationApi.deleteComment(publication.id, comment.id, comment.version);
      setComments((current) => current.filter((item) => item.id !== comment.id));
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 409) {
        setCommentError("다른 곳에서 댓글이 변경되었습니다. 새로고침 후 다시 시도해 주세요.");
      } else {
        setCommentError("댓글을 삭제하지 못했습니다.");
      }
    }
  }

  async function deletePublication() {
    if (!publication || !window.confirm("이 게시글을 삭제할까요? 삭제 후에는 공개되지 않습니다.")) {
      return;
    }
    setDeleting(true);
    setMutationError(null);
    try {
      await publicationApi.delete(publication.id, publication.version);
      navigate("/feed", { replace: true });
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 409) {
        setMutationError("다른 곳에서 게시글이 변경되었습니다. 새로고침 후 다시 시도해 주세요.");
      } else if (requestError instanceof ApiError && requestError.status === 401) {
        navigate(`/login?next=/posts/${publication.id}`, { replace: true });
      } else {
        setMutationError("게시글을 삭제하지 못했습니다.");
      }
    } finally {
      setDeleting(false);
    }
  }

  async function updateGuestPublication(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!publication || guestManagePassword.length < 8 || !guestTitle.trim() || !guestBody.trim()) return;
    try {
      const updated = await guestApi.updatePublication(publication.id, {
        password: guestManagePassword, title: guestTitle.trim(), body: guestBody.trim(), version: publication.version,
      });
      setPublication((current) => current ? { ...current, title: updated.title, body: updated.body, version: updated.version } : current);
      setGuestEditing(false);
      setMutationError("비회원 게시글을 수정했습니다.");
    } catch (requestError) {
      setMutationError(requestError instanceof ApiError && requestError.status === 409 ? "다른 곳에서 게시글이 변경되었습니다." : "비회원 게시글을 수정하지 못했습니다.");
    }
  }

  async function deleteGuestPublication() {
    if (!publication || guestManagePassword.length < 8 || !window.confirm("비회원 게시글을 삭제할까요?")) return;
    try {
      await guestApi.deletePublication(publication.id, { password: guestManagePassword, version: publication.version });
      navigate("/feed/guest", { replace: true });
    } catch (requestError) {
      setMutationError(requestError instanceof ApiError && requestError.status === 409 ? "다른 곳에서 게시글이 변경되었습니다." : "비회원 게시글을 삭제하지 못했습니다.");
    }
  }

  async function reportPublication() {
    if (!publication || !window.confirm("이 게시글을 신고할까요?")) return;
    try {
      await trustApi.report({ targetType: "PUBLICATION", targetId: publication.id, reason: "OTHER" });
      setMutationError("신고가 접수되었습니다. 운영팀이 확인합니다.");
    } catch (requestError) {
      setMutationError(requestError instanceof ApiError && requestError.status === 409 ? "이미 신고한 게시글입니다." : "신고를 접수하지 못했습니다.");
    }
  }

  async function sharePublication() {
    if (!publication || shareSubmitting) return;
    setShareSubmitting(true);
    try {
      const result = await publicationApi.share(publication.id);
      const shareUrl = `${window.location.origin}${result.path}`;
      if (navigator.share) {
        await navigator.share({ title: publication.title, url: shareUrl });
      } else if (navigator.clipboard) {
        await navigator.clipboard.writeText(shareUrl);
        setMutationError("게시글 링크를 클립보드에 복사했습니다.");
      }
    } catch (requestError) {
      if (!(requestError instanceof DOMException && requestError.name === "AbortError")) {
        setMutationError("게시글 링크를 공유하지 못했습니다.");
      }
    } finally {
      setShareSubmitting(false);
    }
  }

  if (error) {
    return (
      <main className="page publication-page publication-state-page">
        <section className="surface-card">
          <p className="eyebrow">게시글 상세</p>
          <h1>글을 열 수 없습니다</h1>
          <p role="alert">{error}</p>
          <Link className="button button-soft" to="/feed/guest">게시판으로</Link>
        </section>
      </main>
    );
  }

  if (!publication) {
    return (
      <main className="page publication-page">
        <section className="surface-card" role="status">게시글을 불러오는 중...</section>
      </main>
    );
  }

  return (
    <main className="page publication-page publication-detail-page">
      <div className="publication-detail-nav">
        <Link className="publication-text-link" to="/feed/guest">목록으로</Link>
        <div className="publication-detail-actions">
          {viewerId === publication.authorId ? (
            <>
              <Link className="button button-soft" to={`/posts/${publication.id}/edit`}>수정</Link>
              <button
                className="button button-danger"
                type="button"
                disabled={deleting}
                onClick={deletePublication}
              >
                {deleting ? "삭제 중..." : "삭제"}
              </button>
            </>
          ) : null}
          {guestView ? <>
            <button className="button button-soft" type="button" onClick={() => setGuestEditing((current) => !current)}>{guestEditing ? "수정 취소" : "비회원 수정"}</button>
            <button className="button button-danger" type="button" onClick={() => void deleteGuestPublication()}>비회원 삭제</button>
          </> : null}
          <Link className="button button-soft" to="/posts/new">새 글 작성</Link>
          <button className="button button-soft" type="button" disabled={shareSubmitting} onClick={sharePublication}>
            {shareSubmitting ? "공유 중..." : "공유"}
          </button>
          {viewerId && viewerId !== publication.authorId ? <button className="button button-soft" type="button" onClick={reportPublication}>신고</button> : null}
        </div>
      </div>
      {mutationError ? (
        <p className="form-error publication-error" role="alert">{mutationError}</p>
      ) : null}
      <article className="surface-card publication-detail-card">
        <div className="publication-detail-chips">
          <span className="publication-chip publication-chip-primary">자유게시판</span>
          <span className="publication-chip">
            {publication.scope === "LOCAL" ? "내 동네" : "전체 공개"}
          </span>
        </div>
        <header className="publication-detail-heading">
          <h1>{publication.title}</h1>
          <div className="publication-author-row">
            <span className="publication-avatar" aria-hidden="true">T</span>
            <div>
              <Link to={`/members/${publication.authorId}`}><strong>TownPet 회원</strong></Link>
              <p>{formatDate(publication.createdAt)}</p>
            </div>
          </div>
          {viewCount !== null ? <span className="publication-view-count">조회 {viewCount.toLocaleString("ko-KR")}</span> : null}
        </header>
        {guestEditing ? (
          <form className="publication-guest-edit" onSubmit={updateGuestPublication}>
            <label>관리 비밀번호<input type="password" minLength={8} value={guestManagePassword} onChange={(event) => setGuestManagePassword(event.target.value)} /></label>
            <label>제목<input value={guestTitle} onChange={(event) => setGuestTitle(event.target.value)} /></label>
            <label>본문<textarea value={guestBody} onChange={(event) => setGuestBody(event.target.value)} /></label>
            <button className="button button-primary" type="submit" disabled={guestManagePassword.length < 8}>수정 저장</button>
          </form>
        ) : <div className="publication-body">{publication.body}</div>}
        <div className="publication-reaction-row">
          {viewerId ? (
            <button
              className={reaction.active ? "reaction-button active" : "reaction-button"}
              type="button"
              aria-label={reaction.active ? "좋아요 취소" : "좋아요"}
              aria-pressed={reaction.active}
              disabled={reactionLoading || reactionSubmitting}
              onClick={setPublicationReaction}
            >
              <span aria-hidden="true">♥</span> 좋아요 {reaction.count}
            </button>
          ) : (
            <Link className="reaction-button" to={`/login?next=/posts/${publication.id}`}>
              <span aria-hidden="true">♥</span> 좋아요 {reaction.count}
            </Link>
          )}
          <span className="publication-reaction-help">회원당 한 번만 표시됩니다.</span>
          {viewerId ? (
            <button
              className={bookmark.active ? "reaction-button bookmark-button active" : "reaction-button bookmark-button"}
              type="button"
              aria-label={bookmark.active ? "저장 취소" : "저장"}
              aria-pressed={bookmark.active}
              disabled={bookmarkLoading || bookmarkSubmitting}
              onClick={setPublicationBookmark}
            >
              <span aria-hidden="true">🔖</span> 저장
            </button>
          ) : (
            <Link className="reaction-button bookmark-button" to={`/login?next=/posts/${publication.id}`}>
              <span aria-hidden="true">🔖</span> 저장
            </Link>
          )}
          <span className="publication-reaction-help">나만 볼 수 있게 저장합니다.</span>
          {viewerId && viewerId !== publication.authorId ? (
            <>
              <button
                className="reaction-button relationship-button"
                type="button"
                aria-label={relationship.following ? "팔로우 취소" : "팔로우"}
                aria-pressed={relationship.following}
                disabled={relationshipSubmitting}
                onClick={() => setPublicationRelationship("following")}
              >
                {relationship.following ? "팔로잉" : "팔로우"}
              </button>
              <button
                className={relationship.blocking ? "reaction-button relationship-button active" : "reaction-button relationship-button"}
                type="button"
                aria-label={relationship.blocking ? "차단 해제" : "차단"}
                aria-pressed={relationship.blocking}
                disabled={relationshipSubmitting}
                onClick={() => setPublicationRelationship("blocking")}
              >
                {relationship.blocking ? "차단 해제" : "차단"}
              </button>
            </>
          ) : null}
        </div>
      </article>
      <section className="surface-card publication-comments" id="comments" aria-labelledby="comments-heading">
        <div className="publication-comments-heading">
          <div>
            <p className="eyebrow">COMMUNITY</p>
            <h2 id="comments-heading">댓글 {comments.length}</h2>
          </div>
          <span className="publication-chip">작성자만 삭제</span>
        </div>
        {commentError ? <p className="form-error publication-error" role="alert">{commentError}</p> : null}
        {commentsLoading ? (
          <p className="publication-comments-state" role="status">댓글을 불러오는 중...</p>
        ) : comments.length === 0 ? (
          <p className="publication-comments-state">첫 번째 댓글을 남겨 보세요.</p>
        ) : (
          <div className="publication-comment-list">
            {comments.map((comment) => (
              <article className="publication-comment" key={comment.id}>
                <div className="publication-comment-meta">
                  <strong>TownPet 회원</strong>
                  <time dateTime={comment.createdAt}>{formatDate(comment.createdAt)}</time>
                  {viewerId === comment.authorId ? (
                    <button className="text-button" type="button" onClick={() => deleteComment(comment)}>
                      삭제
                    </button>
                  ) : null}
                  {viewerId ? (
                    <button className="text-button" type="button" onClick={() => setReplyingTo(comment)}>
                      답글
                    </button>
                  ) : null}
                </div>
                {comment.parentCommentId ? <span className="publication-comment-reply-label">답글</span> : null}
                <p>{comment.body}</p>
              </article>
            ))}
          </div>
        )}
        {viewerId || guestView ? (
          <form className="publication-comment-form" onSubmit={createComment} noValidate>
            {guestView && !viewerId ? <label>관리 비밀번호<input type="password" minLength={8} value={guestPassword} onChange={(event) => setGuestPassword(event.target.value)} /></label> : null}
            {replyingTo ? (
              <div className="publication-replying">
                <span>{replyingTo.body.slice(0, 60)}에 답글 작성 중</span>
                <button className="text-button" type="button" onClick={() => setReplyingTo(null)}>취소</button>
              </div>
            ) : null}
            <label>
              댓글
              <textarea
                aria-label="댓글"
                maxLength={5000}
                value={commentBody}
                onChange={(event) => setCommentBody(event.target.value)}
                placeholder="반려생활에 도움이 되는 이야기를 남겨 주세요."
              />
            </label>
            <div className="publication-comment-submit">
              <span className="field-help">{commentBody.length.toLocaleString()}/5,000</span>
              <button className="button button-primary" type="submit" disabled={commentSubmitting || !commentBody.trim() || (guestView && !viewerId && guestPassword.length < 8)}>
                {commentSubmitting ? "등록 중..." : "댓글 등록"}
              </button>
            </div>
          </form>
        ) : (
          <p className="publication-login-prompt">
            <Link to={`/login?next=/posts/${publication.id}#comments`}>로그인</Link>하면 댓글을 남길 수 있어요.
          </p>
        )}
      </section>
    </main>
  );
}
