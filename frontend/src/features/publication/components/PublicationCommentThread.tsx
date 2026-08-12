import { type FormEvent, type ReactNode, useEffect, useMemo, useRef } from "react";
import { type Comment } from "../../../api/client";
import { formatDateTimeLong } from "../../../utils/date";

type PublicationCommentThreadProps = {
  comments: Comment[];
  replyingTo: Comment | null;
  memberViewer: boolean;
  viewerId: string | null;
  commentBody: string;
  commentSubmitting: boolean;
  onDelete: (comment: Comment) => void;
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
  viewerId,
  commentBody,
  commentSubmitting,
  onDelete,
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
        <strong>TownPet 회원</strong>
        <time dateTime={comment.createdAt}>{formatDateTimeLong(comment.createdAt)}</time>
        {memberViewer && viewerId === comment.authorId ? <button className="text-button" type="button" onClick={() => onDelete(comment)}>삭제</button> : null}
        {memberViewer ? <button ref={replyButtonRef} className="text-button" type="button" onClick={() => onReply(comment)} aria-expanded={isReplying} aria-controls={`reply-form-${comment.id}`}>답글</button> : null}
      </div>
      {comment.parentCommentId ? <span className="publication-comment-reply-label">답글</span> : null}
      <p>{comment.body}</p>
      {isReplying ? (
        <form id={`reply-form-${comment.id}`} className="publication-comment-form publication-comment-form-inline" aria-label="답글 작성" onSubmit={onSubmit} noValidate>
          <div className="publication-replying">
            <span>{comment.body.slice(0, 60)}에 답글 작성 중</span>
            <button className="text-button" type="button" onClick={onCancelReply}>취소</button>
          </div>
          <label>
            답글
            <textarea aria-label="답글" maxLength={5000} value={commentBody} onChange={(event) => onChangeBody(event.target.value)} placeholder="반려생활에 도움이 되는 이야기를 남겨 주세요." autoFocus />
          </label>
          <div className="publication-comment-submit">
            <span className="field-help">{commentBody.length.toLocaleString()}/5,000</span>
            <button className="button button-primary" type="submit" disabled={commentSubmitting || !commentBody.trim()}>{commentSubmitting ? "등록 중..." : "답글 등록"}</button>
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
