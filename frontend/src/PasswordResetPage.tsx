import { FormEvent, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import AuthPageLayout from "./AuthPageLayout";
import { ApiError, authApi } from "./api/client";

export default function PasswordResetPage() {
  const [searchParams] = useSearchParams();
  const [email, setEmail] = useState("");
  const [token, setToken] = useState(() => searchParams.get("token") ?? "");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [requesting, setRequesting] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [requestSent, setRequestSent] = useState(false);
  const [resetComplete, setResetComplete] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function requestReset(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setRequesting(true);
    setRequestSent(false);
    setError(null);
    try {
      await authApi.requestPasswordReset(email);
      setRequestSent(true);
    } catch {
      setError("요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setRequesting(false);
    }
  }

  async function confirmReset(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setResetComplete(false);
    if (password !== passwordConfirm) {
      setError("새 비밀번호가 일치하지 않습니다.");
      return;
    }

    setConfirming(true);
    try {
      await authApi.confirmPasswordReset(token, password);
      setResetComplete(true);
      setPassword("");
      setPasswordConfirm("");
    } catch (requestError) {
      setError(
        requestError instanceof ApiError && requestError.status === 400
          ? "토큰이 만료되었거나 비밀번호 정책을 충족하지 않습니다."
          : "비밀번호를 재설정하지 못했습니다.",
      );
    } finally {
      setConfirming(false);
    }
  }

  return (
    <AuthPageLayout
      eyebrow="계정 복구"
      title="비밀번호 재설정"
      description="가입한 이메일로 일회성 토큰을 요청한 뒤 새 비밀번호를 설정합니다."
    >
      <div className="auth-steps">
        <form onSubmit={requestReset} className="auth-form auth-step">
          <div>
            <h2>1. 재설정 메일 받기</h2>
            <p>계정 존재 여부와 관계없이 같은 안내를 제공합니다.</p>
          </div>
          <label>
            이메일
            <input
              required
              type="email"
              inputMode="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="you@townpet.dev"
            />
          </label>
          {requestSent ? (
            <div className="form-success" role="status" aria-live="polite">
              <p>입력한 주소의 계정을 확인한 뒤 필요한 경우 재설정 메일을 보냈습니다.</p>
              <p>메일이 도착하지 않으면 스팸함을 확인하거나 몇 분 뒤 다시 요청해 주세요.</p>
            </div>
          ) : null}
          <button
            type="submit"
            className="button button-soft"
            disabled={requesting || !email.trim()}
          >
            {requesting ? "요청 중..." : "재설정 메일 요청"}
          </button>
        </form>

        <form onSubmit={confirmReset} className="auth-form auth-step">
          <div>
            <h2>2. 새 비밀번호 설정</h2>
            <p>대·소문자, 숫자와 특수문자를 포함해 10자 이상 입력해 주세요.</p>
          </div>
          <label>
            재설정 토큰
            <input
              required
              minLength={32}
              maxLength={128}
              value={token}
              onChange={(event) => setToken(event.target.value)}
              autoComplete="off"
              placeholder="메일로 받은 토큰"
            />
          </label>
          <label>
            새 비밀번호
            <input
              required
              minLength={10}
              maxLength={72}
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </label>
          <label>
            새 비밀번호 확인
            <input
              required
              minLength={10}
              maxLength={72}
              type="password"
              autoComplete="new-password"
              value={passwordConfirm}
              onChange={(event) => setPasswordConfirm(event.target.value)}
            />
          </label>
          {error ? (
            <p role="alert" className="form-error" aria-live="polite">
              {error}
            </p>
          ) : null}
          {resetComplete ? (
            <p className="form-success" role="status" aria-live="polite">
              비밀번호가 재설정되었습니다. <Link to="/login">로그인으로 이동</Link>
            </p>
          ) : null}
          <button
            type="submit"
            className="button button-primary"
            disabled={confirming || !token.trim() || !password || !passwordConfirm}
          >
            {confirming ? "재설정 중..." : "비밀번호 재설정"}
          </button>
        </form>
      </div>
    </AuthPageLayout>
  );
}
