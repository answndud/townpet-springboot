import { type FormEvent, type ReactNode, useEffect, useMemo, useRef } from "react";
import { type Comment } from "../../../api/client";
import { formatDateTimeLong } from "../../../utils/date";

type PublicationCommentThreadProps = {
  comments: Comment[];
  replyingTo: Comment | null;
  memberViewer: boolean;
  guestViewer: boolean;
  viewerId: string | null;
  guestPassword: string;
  commentBody: string;
  commentSubmitting: boolean;
  onDelete: (comment: Comment) => void;
  onEdit: (comment: Comment) => void;
  guestManageComment: Comment | null;
  guestManageMode: "edit" | "delete" | null;
  guestEditBody: string;
  guestManagePassword: string;
  onChangeGuestEditBody: (value: string) => void;
  onChangeGuestManagePassword: (value: string) => void;
  onCancelGuestManage: () => void;
  onSubmitGuestManage: (event: FormEvent<HTMLFormElement>, comment: Comment) => void;
  onReport: (comment: Comment) => void;
  onChangeGuestPassword: (value: string) => void;
  onReply: (comment: Comment) => void;
  onCancelReply: () => void;
  onChangeBody: (value: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

type CommentItemProps = Omit<PublicationCommentThreadProps, "comments" | "replyingTo"> & {
  comment: Comment;
  children: ReactNode;
  isReplying: boolean;
};

function CommentItem({
  comment,
  children,
  isReplying,
  memberViewer,
  guestViewer,
  viewerId,
  guestPassword,
  commentBody,
  commentSubmitting,
  onDelete,
  onEdit,
  guestManageComment,
  guestManageMode,
  guestEditBody,
  guestManagePassword,
  onChangeGuestEditBody,
  onChangeGuestManagePassword,
  onCancelGuestManage,
  onSubmitGuestManage,
  onReport,
  onChangeGuestPassword,
  onReply,
  onCancelReply,
  onChangeBody,
  onSubmit,
}: CommentItemProps) {
  const replyButtonRef = useRef<HTMLButtonElement>(null);
  const wasReplying = useRef(false);

  useEffect(() => {
    if (wasReplying.current && !isReplying) replyButtonRef.current?.focus();
    wasReplying.current = isReplying;
  }, [isReplying]);

  return (
    <article className={comment.parentCommentId ? "publication-comment publication-comment-reply" : "publication-comment"} data-comment-id={comment.id}>
      <div className="publication-comment-meta">
        <strong>{comment.authorType === "GUEST" ? "비회원" : "TownPet 회원"}</strong>
        <time dateTime={comment.createdAt}>{formatDateTimeLong(comment.createdAt)}</time>
        {memberViewer && viewerId === comment.authorId ? <button className="text-button comment-action comment-action-danger" type="button" onClick={() => onDelete(comment)}>삭제</button> : null}
        {guestViewer && comment.authorType === "GUEST" ? <>
          <button className="text-button comment-action" type="button" onClick={() => onEdit(comment)}>수정</button>
          <button className="text-button comment-action comment-action-danger" type="button" onClick={() => onDelete(comment)}>삭제</button>
        </> : null}
        {memberViewer && viewerId !== comment.authorId ? <button className="text-button comment-action" type="button" onClick={() => onReport(comment)}>신고</button> : null}
        {memberViewer || guestViewer ? <button ref={replyButtonRef} className="text-button comment-action" type="button" onClick={() => onReply(comment)} aria-expanded={isReplying} aria-controls={`reply-form-${comment.id}`}>답글</button> : null}
      </div>
      {comment.parentCommentId ? <span className="publication-comment-reply-label">답글</span> : null}
      <p>{comment.body}</p>
      {guestViewer && guestManageComment?.id === comment.id ? (
        <form className="publication-comment-form publication-comment-form-inline" aria-label={guestManageMode === "edit" ? "비회원 댓글 수정" : "비회원 댓글 삭제 확인"} onSubmit={(event) => onSubmitGuestManage(event, comment)}>
          <label>댓글 비밀번호<input name="password" type="password" minLength={8} maxLength={72} autoComplete="current-password" value={guestManagePassword} onChange={(event) => onChangeGuestManagePassword(event.target.value)} required /></label>
          {guestManageMode === "edit" ? <label>댓글 내용<textarea maxLength={5000} value={guestEditBody} onChange={(event) => onChangeGuestEditBody(event.target.value)} required /></label> : <p className="field-help">작성할 때 설정한 비밀번호가 일치하면 댓글을 삭제합니다.</p>}
          <div className="publication-comment-submit">
            <button className="button button-primary" type="submit">{guestManageMode === "edit" ? "수정 저장" : "댓글 삭제"}</button>
            <button className="button button-soft" type="button" onClick={onCancelGuestManage}>취소</button>
          </div>
        </form>
      ) : null}
      {isReplying ? (
        <form id={`reply-form-${comment.id}`} className="publication-comment-form publication-comment-form-inline" aria-label="답글 작성" onSubmit={onSubmit} noValidate>
          <div className="publication-replying">
            <span>{comment.body.slice(0, 60)}에 답글 작성 중</span>
            <button className="text-button comment-action" type="button" onClick={onCancelReply}>취소</button>
          </div>
          <label>
            답글
            <textarea aria-label="답글" maxLength={5000} value={commentBody} onChange={(event) => onChangeBody(event.target.value)} placeholder="반려생활에 도움이 되는 이야기를 남겨 주세요." autoFocus />
          </label>
          {guestViewer ? <>
            <p className="field-help">댓글마다 비밀번호가 적용되며, 같은 비밀번호로 해당 댓글을 수정·삭제합니다.</p>
            <label>댓글 비밀번호<input type="password" minLength={8} value={guestPassword} onChange={(event) => onChangeGuestPassword(event.target.value)} autoComplete="current-password" /></label>
          </> : null}
          <div className="publication-comment-submit">
            <span className="field-help">{commentBody.length.toLocaleString()}/5,000</span>
            <button className="button button-primary" type="submit" disabled={commentSubmitting || !commentBody.trim() || (guestViewer && guestPassword.length < 8)}>{commentSubmitting ? "등록 중..." : "답글 등록"}</button>
          </div>
        </form>
      ) : null}
      {children}
    </article>
  );
}

export function PublicationCommentThread({ comments, replyingTo, ...props }: PublicationCommentThreadProps) {
  const commentsByParent = useMemo(() => {
    const grouped = new Map<string | null, Comment[]>();
    for (const comment of comments) {
      const siblings = grouped.get(comment.parentCommentId) ?? [];
      siblings.push(comment);
      grouped.set(comment.parentCommentId, siblings);
    }
    return grouped;
  }, [comments]);

  function renderComments(parentCommentId: string | null): ReactNode {
    return (commentsByParent.get(parentCommentId) ?? []).map((comment) => (
      <CommentItem {...props} comment={comment} key={comment.id} isReplying={replyingTo?.id === comment.id}>
        {(commentsByParent.get(comment.id) ?? []).length ? <div className="publication-comment-children">{renderComments(comment.id)}</div> : null}
      </CommentItem>
    ));
  }

  return <>{renderComments(null)}</>;
}
