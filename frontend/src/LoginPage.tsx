import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { apiFetch, getCsrfToken } from "./api/client";

export default function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await getCsrfToken();
      await apiFetch("/api/v1/auth/sessions", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ email, password }),
      });
      navigate("/profile");
    } catch {
      setError("이메일 또는 비밀번호를 확인해 주세요.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="page placeholder-page">
      <section className="surface-card login-card">
        <span className="eyebrow">TOWNPET ACCOUNT</span>
        <h1>TownPet 로그인</h1>
        <p>내 동네 반려생활 정보를 이어서 확인하세요.</p>
        <form onSubmit={submit} className="login-form">
          <label>
            이메일
            <input required type="email" value={email} onChange={(event) => setEmail(event.target.value)} />
          </label>
          <label>
            비밀번호
            <input required minLength={8} type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
          </label>
          {error ? <p role="alert" className="form-error">{error}</p> : null}
          <button className="button button-primary" type="submit" disabled={submitting}>
            {submitting ? "확인 중..." : "로그인"}
          </button>
        </form>
        <Link to="/" className="back-link">홈으로 돌아가기</Link>
      </section>
    </main>
  );
}
