import { FormEvent, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import AuthPageLayout from "./AuthPageLayout";
import { ApiError, authApi } from "./api/client";

const DEMO_EMAIL = import.meta.env.VITE_DEMO_EMAIL;
const DEMO_PASSWORD = import.meta.env.VITE_DEMO_PASSWORD;
const DEMO_DATA_ENABLED = import.meta.env.VITE_DEMO_DATA_ENABLED === "true";

function safeNextPath(candidate: string | null, role: "MEMBER" | "MODERATOR") {
  const internalPath = candidate?.startsWith("/") && !candidate.startsWith("//") ? candidate : null;
  if (role === "MEMBER" && internalPath?.startsWith("/admin")) return "/profile";
  return internalPath ?? (role === "MODERATOR" ? "/admin" : "/profile");
}

export default function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [capsLockOn, setCapsLockOn] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const session = await authApi.login(email, password);
      navigate(safeNextPath(searchParams.get("next"), session.role));
    } catch (requestError) {
      setError(
        requestError instanceof ApiError && requestError.status === 401
          ? "이메일 인증 여부와 로그인 정보를 확인해 주세요."
          : "로그인 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.",
      );
    } finally {
      setSubmitting(false);
    }
  }

  function fillDemoCredential() {
    if (!DEMO_EMAIL || !DEMO_PASSWORD) return;
    setEmail(DEMO_EMAIL);
    setPassword(DEMO_PASSWORD);
    setError(null);
  }

  return (
    <AuthPageLayout
      eyebrow="계정 접속"
      title="로그인"
      description="가입한 이메일과 비밀번호로 로그인해 주세요."
    >
      <form onSubmit={submit} className="auth-form" noValidate>
        <label>
          이메일
          <input
            data-testid="login-email"
            required
            type="email"
            inputMode="email"
            autoComplete="email"
            spellCheck={false}
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="you@townpet.dev"
            aria-invalid={Boolean(error)}
          />
        </label>
        <div className="field-heading">
          <span>비밀번호</span>
          <Link to="/password/reset">비밀번호 재설정</Link>
        </div>
        <div className="password-field">
          <input
            data-testid="login-password"
            required
            minLength={8}
            type={showPassword ? "text" : "password"}
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            onKeyUp={(event) => setCapsLockOn(event.getModifierState("CapsLock"))}
            onBlur={() => setCapsLockOn(false)}
            placeholder="비밀번호를 입력해 주세요"
            aria-label="비밀번호"
            aria-invalid={Boolean(error)}
          />
          <button
            type="button"
            className="button button-soft password-toggle"
            onClick={() => setShowPassword((visible) => !visible)}
            aria-pressed={showPassword}
          >
            {showPassword ? "숨기기" : "표시"}
          </button>
        </div>
        {capsLockOn ? (
          <p className="form-notice" role="status" aria-live="polite">
            Caps Lock이 켜져 있습니다.
          </p>
        ) : null}
        {error ? (
          <p role="alert" className="form-error" aria-live="polite">
            {error}
          </p>
        ) : null}
        <button
          data-testid="login-submit"
          className="button button-primary"
          type="submit"
          disabled={submitting || !email.trim() || !password}
        >
          {submitting ? "로그인 중..." : "이메일로 로그인"}
        </button>
      </form>

      {DEMO_DATA_ENABLED && DEMO_EMAIL && DEMO_PASSWORD ? (
        <aside className="demo-credential" aria-label="포트폴리오 데모 계정">
          <div>
            <strong>데모 계정</strong>
            <p>공개 showcase의 합성 계정이며 입력 데이터는 주기적으로 초기화됩니다.</p>
          </div>
          <code>{DEMO_EMAIL}</code>
          <code>{DEMO_PASSWORD}</code>
          <button type="button" className="button button-soft" onClick={fillDemoCredential}>
            데모 계정 입력
          </button>
        </aside>
      ) : null}

      <nav className="auth-links" aria-label="계정 도움말">
        <Link to="/verify-email">이메일 인증</Link>
        <Link to="/password/reset">비밀번호 재설정</Link>
      </nav>
    </AuthPageLayout>
  );
}
