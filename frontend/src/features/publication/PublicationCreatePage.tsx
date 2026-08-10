import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ApiError,
  catalogApi,
  memberApi,
  publicationApi,
  type Member,
  type Neighborhood,
  type PublicationScope,
} from "../../api/client";

const TITLE_MAX_LENGTH = 120;
const BODY_MAX_LENGTH = 20_000;

export default function PublicationCreatePage() {
  const navigate = useNavigate();
  const [member, setMember] = useState<Member | null>(null);
  const [neighborhoods, setNeighborhoods] = useState<Neighborhood[]>([]);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [scope, setScope] = useState<PublicationScope>("GLOBAL");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    Promise.all([
      memberApi.current(controller.signal),
      catalogApi.neighborhoods(controller.signal),
    ])
      .then(([currentMember, options]) => {
        if (!active) return;
        setMember(currentMember);
        setNeighborhoods(options);
      })
      .catch((requestError: unknown) => {
        if (!active || (requestError instanceof DOMException && requestError.name === "AbortError")) {
          return;
        }
        if (requestError instanceof ApiError && requestError.status === 401) {
          navigate("/login?next=/posts/new", { replace: true });
          return;
        }
        setError("글 작성 정보를 불러오지 못했습니다.");
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [navigate]);

  const neighborhood = neighborhoods.find((item) => item.id === member?.neighborhoodId) ?? null;
  const canSubmit =
    !submitting &&
    Boolean(title.trim()) &&
    Boolean(body.trim()) &&
    (scope === "GLOBAL" || Boolean(neighborhood));

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canSubmit) return;
    setSubmitting(true);
    setError(null);
    try {
      const publication = await publicationApi.create({
        title: title.trim(),
        body: body.trim(),
        scope,
        ...(scope === "LOCAL" && neighborhood ? { neighborhoodId: neighborhood.id } : {}),
      });
      navigate(`/posts/${publication.id}`);
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        navigate("/login?next=/posts/new", { replace: true });
        return;
      }
      setError(
        requestError instanceof ApiError && requestError.status === 400
          ? "제목, 본문과 공개 범위를 확인해 주세요."
          : "글을 등록하지 못했습니다. 잠시 후 다시 시도해 주세요.",
      );
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return (
      <main className="page publication-page">
        <section className="surface-card" role="status">글 작성 정보를 불러오는 중...</section>
      </main>
    );
  }

  if (error && !member) {
    return (
      <main className="page publication-page publication-state-page">
        <section className="surface-card">
          <p className="eyebrow">글 작성</p>
          <h1>글쓰기 준비가 지연됐습니다</h1>
          <p className="form-error" role="alert">{error}</p>
          <button className="button button-soft" type="button" onClick={() => window.location.reload()}>
            다시 시도
          </button>
        </section>
      </main>
    );
  }

  return (
    <main className="page publication-page">
      <section className="publication-hero">
        <div>
          <p className="eyebrow">글 작성</p>
          <h1>새 글 작성</h1>
          <p>게시 범위를 정한 뒤 이웃에게 도움이 되는 이야기를 나눠 주세요.</p>
        </div>
        <div className="publication-hero-actions">
          <span className="publication-chip">회원 작성</span>
          <Link className="publication-text-link" to="/feed">피드로 돌아가기</Link>
        </div>
      </section>

      <form className="publication-form" onSubmit={submit} noValidate>
        <div className="publication-form-grid">
          <section className="surface-card publication-fields">
            <div className="publication-section-title">글 정보</div>
            <label>
              분류
              <select value="FREE_BOARD" disabled>
                <option value="FREE_BOARD">자유게시판</option>
              </select>
            </label>
            <fieldset className="publication-scope-field">
              <legend>공개 범위</legend>
              <label className="publication-radio">
                <input
                  type="radio"
                  name="scope"
                  value="GLOBAL"
                  checked={scope === "GLOBAL"}
                  onChange={() => setScope("GLOBAL")}
                />
                <span><strong>전체</strong><small>모든 방문자가 볼 수 있어요.</small></span>
              </label>
              <label className="publication-radio">
                <input
                  type="radio"
                  name="scope"
                  value="LOCAL"
                  checked={scope === "LOCAL"}
                  disabled={!neighborhood}
                  onChange={() => setScope("LOCAL")}
                />
                <span>
                  <strong>내 동네</strong>
                  <small>{neighborhood ? neighborhood.name : "대표 동네를 먼저 설정해 주세요."}</small>
                </span>
              </label>
              {!neighborhood ? (
                <Link className="publication-text-link" to="/onboarding">프로필에서 동네 설정</Link>
              ) : null}
            </fieldset>
            <label>
              제목
              <input
                required
                autoFocus
                aria-label="제목"
                maxLength={TITLE_MAX_LENGTH}
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                placeholder="제목을 입력해 주세요"
              />
              <span className="field-help">{title.length}/{TITLE_MAX_LENGTH}</span>
            </label>
            <label>
              본문
              <textarea
                required
                aria-label="본문"
                maxLength={BODY_MAX_LENGTH}
                value={body}
                onChange={(event) => setBody(event.target.value)}
                placeholder="반려생활 이야기를 구체적으로 적어 주세요."
              />
              <span className="field-help">{body.length.toLocaleString()}/{BODY_MAX_LENGTH.toLocaleString()}</span>
            </label>
          </section>

          <aside className="surface-card publication-policy">
            <p className="eyebrow">작성 기준</p>
            <p>자유게시판 글은 선택한 공개 범위에 맞춰 등록됩니다.</p>
            <div>
              <strong>등록 전 확인</strong>
              <ul>
                <li>동물, 지역, 상황을 구체적으로 적어 주세요.</li>
                <li>개인정보와 민감한 연락처는 공개하지 마세요.</li>
                <li>게시판 성격에 맞는 내용인지 한 번 더 확인해 주세요.</li>
              </ul>
            </div>
          </aside>
        </div>

        {error ? <p className="form-error publication-error" role="alert">{error}</p> : null}
        <footer className="publication-submit-row">
          <Link className="publication-text-link" to="/feed">취소</Link>
          <button className="button button-primary" type="submit" disabled={!canSubmit}>
            {submitting ? "등록 중..." : "등록"}
          </button>
        </footer>
      </form>
    </main>
  );
}
