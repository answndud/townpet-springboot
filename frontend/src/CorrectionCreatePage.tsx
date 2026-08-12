import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, apiMutate } from "./api/client";

export default function CorrectionCreatePage() {
  const navigate = useNavigate();
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!title.trim() || !body.trim()) {
      setError("제목과 정정 내용을 입력해 주세요.");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await apiMutate("/api/corrections", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ title: title.trim(), body: body.trim() }),
      });
      navigate("/", { replace: true });
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        navigate("/login?next=/corrections/new");
      } else {
        setError("정정 요청을 등록하지 못했습니다.");
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="page marketplace-page">
      <section className="marketplace-hero">
        <div>
          <p className="eyebrow">CORRECTION REQUEST</p>
          <h1>정보 정정 요청</h1>
          <p>잘못된 정보나 보완이 필요한 내용을 운영팀에 알려 주세요.</p>
        </div>
      </section>
      <form className="surface-card marketplace-form" onSubmit={submit} noValidate>
        <label>제목<input maxLength={120} value={title} onChange={(event) => setTitle(event.target.value)} placeholder="예: 병원 운영시간이 변경됐어요" /></label>
        <label>정정 내용<textarea maxLength={2000} value={body} onChange={(event) => setBody(event.target.value)} placeholder="현재 정보와 올바른 정보를 함께 적어 주세요." /></label>
        {error ? <p className="form-error" role="alert">{error}</p> : null}
        <div className="publication-submit-row"><Link className="publication-text-link" to="/">취소</Link><button className="button button-primary" type="submit" disabled={saving}>{saving ? "등록 중..." : "정정 요청 보내기"}</button></div>
      </form>
    </main>
  );
}
