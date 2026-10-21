import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ApiError, authApi } from "./api/client";

function safeNextPath(candidate: string | null) {
  return candidate?.startsWith("/") && !candidate.startsWith("//") ? candidate : "/admin";
}

export default function MfaPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const mode = searchParams.get("mode") === "enroll" ? "enroll" : "verify";
  const nextPath = safeNextPath(searchParams.get("next"));
  const [enrollment, setEnrollment] = useState<{ secret: string; otpauthUri: string; expiresAt: string } | null>(null);
  const [code, setCode] = useState("");
  const [recoveryMode, setRecoveryMode] = useState(false);
  const [recoveryCodes, setRecoveryCodes] = useState<string[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (mode !== "enroll") return;
    let active = true;
    void authApi.startMfaEnrollment()
      .then((response) => { if (active) setEnrollment(response); })
      .catch(() => { if (active) setError("MFA 등록 시작 실패"); });
    return () => { active = false; };
  }, [mode]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      if (mode === "enroll") {
        const response = await authApi.confirmMfaEnrollment(code);
        setRecoveryCodes(response.recoveryCodes);
        setCode("");
      } else if (recoveryMode) {
        await authApi.useMfaRecoveryCode(code);
        navigate(nextPath, { replace: true });
      } else {
        await authApi.verifyMfa(code);
        navigate(nextPath, { replace: true });
      }
    } catch (requestError) {
      setError(
        requestError instanceof ApiError && requestError.status === 429
          ? "인증 시도 초과 · 잠시 후 다시 시도"
          : "인증 코드 오류",
      );
    } finally {
      setSubmitting(false);
    }
  }

  if (recoveryCodes) {
    return (
      <main className="page placeholder-page">
        <section className="surface-card profile-card">

          <h1>복구 코드 보관</h1>
          <p>복구 코드 · 1회 표시</p>
          <pre aria-label="MFA 복구 코드">{recoveryCodes.join("\n")}</pre>
          <button className="button button-primary" type="button" onClick={() => navigate(nextPath, { replace: true })}>운영 콘솔로 이동</button>
        </section>
      </main>
    );
  }

  return (
    <main className="page placeholder-page">
      <section className="surface-card profile-card">

        <h1>{mode === "enroll" ? "운영자 MFA 등록" : "운영자 MFA 확인"}</h1>
        {mode === "enroll" ? (
          <>
            <p>QR 또는 키 등록 · 6자리 코드 입력</p>
            {enrollment ? <><code>{enrollment.secret}</code><p><small>{enrollment.otpauthUri}</small></p></> : <p role="status">등록 정보를 준비하는 중...</p>}
          </>
        ) : <p>인증 앱 코드 · 6자리</p>}
        <form className="auth-form" onSubmit={submit}>
          <label>{recoveryMode ? "복구 코드" : "인증 코드"}<input required inputMode="numeric" autoComplete="one-time-code" value={code} onChange={(event) => setCode(event.target.value)} maxLength={recoveryMode ? 16 : 6} /></label>
          {error ? <p className="form-error" role="alert">{error}</p> : null}
          <button className="button button-primary" type="submit" disabled={submitting || (mode === "enroll" && !enrollment)}>{submitting ? "확인 중..." : "확인"}</button>
          {mode === "verify" ? <button className="button button-soft" type="button" onClick={() => { setRecoveryMode((value) => !value); setCode(""); setError(null); }}>{recoveryMode ? "인증 앱 코드 사용" : "복구 코드 사용"}</button> : null}
        </form>
        <Link to="/" className="button button-soft">홈으로 돌아가기</Link>
      </section>
    </main>
  );
}
