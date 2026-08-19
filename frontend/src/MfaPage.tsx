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
      .catch(() => { if (active) setError("MFA 등록을 시작하지 못했습니다. 잠시 후 다시 시도해 주세요."); });
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
          ? "시도 횟수가 너무 많습니다. 잠시 후 다시 시도해 주세요."
          : "인증 코드를 확인하지 못했습니다. 다시 입력해 주세요.",
      );
    } finally {
      setSubmitting(false);
    }
  }

  if (recoveryCodes) {
    return (
      <main className="page placeholder-page">
        <section className="surface-card profile-card">
          <p className="eyebrow">MODERATOR SECURITY</p>
          <h1>복구 코드 보관</h1>
          <p>복구 코드는 지금 한 번만 표시됩니다. 안전한 비밀번호 관리자에 저장하세요.</p>
          <pre aria-label="MFA 복구 코드">{recoveryCodes.join("\n")}</pre>
          <button className="button button-primary" type="button" onClick={() => navigate(nextPath, { replace: true })}>운영 콘솔로 이동</button>
        </section>
      </main>
    );
  }

  return (
    <main className="page placeholder-page">
      <section className="surface-card profile-card">
        <p className="eyebrow">MODERATOR SECURITY</p>
        <h1>{mode === "enroll" ? "운영자 MFA 등록" : "운영자 MFA 확인"}</h1>
        {mode === "enroll" ? (
          <>
            <p>인증 앱에서 아래 QR URI 또는 secret을 등록한 뒤 6자리 코드를 입력하세요.</p>
            {enrollment ? <><code>{enrollment.secret}</code><p><small>{enrollment.otpauthUri}</small></p></> : <p role="status">등록 정보를 준비하는 중...</p>}
          </>
        ) : <p>인증 앱의 6자리 코드를 입력하세요.</p>}
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
