import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, guestApi } from "../../api/client";

export default function GuestPublicationCreatePage() {
  const navigate = useNavigate();
  const [password, setPassword] = useState("");
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (password.length < 8 || !title.trim() || !body.trim() || submitting) return;
    setSubmitting(true);
    setError(null);
    try {
      await guestApi.createAuthor(password);
      const publication = await guestApi.createPublication({ password, title: title.trim(), body: body.trim() });
      navigate(`/posts/${publication.id}/guest`);
    } catch (requestError) {
      setError(requestError instanceof ApiError && requestError.status === 400
        ? "비밀번호는 8자 이상이며 제목과 본문을 입력해 주세요."
        : "비회원 글을 등록하지 못했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="page publication-page">
      <section className="publication-hero">
        <div><p className="eyebrow">GUEST COMMUNITY</p><h1>비회원 글쓰기</h1><p>관리 비밀번호로 나중에 내 글을 수정·삭제할 수 있어요.</p></div>
        <Link className="publication-text-link" to="/?view=all">피드로 돌아가기</Link>
      </section>
      <form className="publication-form" onSubmit={submit} noValidate>
        <section className="surface-card publication-fields">
          <label>관리 비밀번호<input type="password" minLength={8} maxLength={72} value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="new-password" /></label>
          <span className="field-help">8~72자 · 글 관리에 필요합니다.</span>
          <label>제목<input maxLength={120} value={title} onChange={(event) => setTitle(event.target.value)} /></label>
          <label>본문<textarea maxLength={20000} value={body} onChange={(event) => setBody(event.target.value)} /></label>
        </section>
        {error ? <p className="form-error publication-error" role="alert">{error}</p> : null}
        <footer className="publication-submit-row"><Link className="publication-text-link" to="/?view=all">취소</Link><button className="button button-primary" type="submit" disabled={submitting || password.length < 8 || !title.trim() || !body.trim()}>{submitting ? "등록 중..." : "비회원으로 등록"}</button></footer>
      </form>
    </main>
  );
}
