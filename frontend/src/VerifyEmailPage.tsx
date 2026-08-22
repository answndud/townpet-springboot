import { FormEvent, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import AuthPageLayout from "./AuthPageLayout";
import { ApiError, authApi } from "./api/client";

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const [email, setEmail] = useState(() => searchParams.get("email") ?? "");
  const [token, setToken] = useState(() => searchParams.get("token") ?? "");
  const [requesting, setRequesting] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [requestSent, setRequestSent] = useState(false);
  const [verified, setVerified] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function requestVerification(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setRequesting(true);
    setRequestSent(false);
    setError(null);
    try {
      await authApi.requestEmailVerification(email);
      setRequestSent(true);
    } catch {
      setError("인증 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setRequesting(false);
    }
  }

  async function confirmVerification(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setConfirming(true);
    setVerified(false);
    setError(null);
    try {
      await authApi.confirmEmailVerification(token);
      setVerified(true);
    } catch (requestError) {
      setError(
        requestError instanceof ApiError && requestError.status === 400
          ? "인증 토큰이 만료되었거나 이미 사용되었습니다."
          : "이메일 인증을 완료하지 못했습니다.",
      );
    } finally {
      setConfirming(false);
    }
  }

  return (
    <AuthPageLayout
      eyebrow="계정 확인"
      title="이메일 인증"
      description="이메일 소유를 확인해야 Credentials 로그인을 사용할 수 있습니다."
    >
      <div className="auth-steps">
        <form onSubmit={requestVerification} className="auth-form auth-step">
          <div>
            <h2>1. 인증 메일 받기</h2>
            <p>계정이나 인증 상태를 노출하지 않고 같은 결과를 안내합니다.</p>
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
              <p>입력한 주소의 계정을 확인한 뒤 필요한 경우 인증 메일을 보냈습니다.</p>
              <p>메일이 도착하지 않으면 스팸함을 확인하거나 몇 분 뒤 다시 요청해 주세요.</p>
            </div>
          ) : null}
          <button
            type="submit"
            className="button button-soft"
            disabled={requesting || !email.trim()}
          >
            {requesting ? "요청 중..." : "인증 메일 요청"}
          </button>
        </form>

        <form onSubmit={confirmVerification} className="auth-form auth-step">
          <div>
            <h2>2. 인증 토큰 확인</h2>
            <p>메일로 받은 일회성 토큰을 입력해 주세요.</p>
          </div>
          <label>
            인증 토큰
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
          {error ? (
            <p role="alert" className="form-error" aria-live="polite">
              {error}
            </p>
          ) : null}
          {verified ? (
            <p className="form-success" role="status" aria-live="polite">
              이메일 인증이 완료되었습니다. <Link to="/login">로그인으로 이동</Link>
            </p>
          ) : null}
          <button
            type="submit"
            className="button button-primary"
            disabled={confirming || !token.trim()}
          >
            {confirming ? "확인 중..." : "이메일 인증 완료"}
          </button>
        </form>
      </div>
    </AuthPageLayout>
  );
}
