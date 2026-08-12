import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  ApiError,
  catalogApi,
  publicationApi,
  type Member,
  type Neighborhood,
  type Publication,
  type PublicationScope,
} from "../../api/client";
import { ANIMAL_INTEREST_GROUPS } from "../member/AnimalInterestMenu";
import { useAuth } from "../../auth/AuthContext";

const TITLE_MAX_LENGTH = 120;
const BODY_MAX_LENGTH = 20_000;

export default function PublicationEditPage() {
  const { publicationId = "" } = useParams();
  const navigate = useNavigate();
  const { member: authMember } = useAuth();
  const [member, setMember] = useState<Member | null>(null);
  const [publication, setPublication] = useState<Publication | null>(null);
  const [neighborhoods, setNeighborhoods] = useState<Neighborhood[]>([]);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [scope, setScope] = useState<PublicationScope>("GLOBAL");
  const [animalInterestCode, setAnimalInterestCode] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    if (!authMember) return () => controller.abort();

    Promise.all([
      publicationApi.detail(publicationId, controller.signal),
      catalogApi.neighborhoods(controller.signal),
    ])
      .then(([currentPublication, options]) => {
        if (!active) return;
        if (authMember.id !== currentPublication.authorId) {
          setError("작성자만 이 게시글을 수정할 수 있습니다.");
          return;
        }
        setMember(authMember);
        setPublication(currentPublication);
        setNeighborhoods(options);
        setTitle(currentPublication.title);
        setBody(currentPublication.body);
        setScope(currentPublication.scope);
        setAnimalInterestCode(currentPublication.animalInterestCode ?? "");
      })
      .catch((requestError: unknown) => {
        if (!active || (requestError instanceof DOMException && requestError.name === "AbortError")) {
          return;
        }
        if (requestError instanceof ApiError && requestError.status === 401) {
          navigate(`/login?next=/posts/${publicationId}/edit`, { replace: true });
        } else if (requestError instanceof ApiError && requestError.status === 404) {
          setError("존재하지 않거나 삭제된 게시글입니다.");
        } else {
          setError("게시글 수정 정보를 불러오지 못했습니다.");
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [authMember, navigate, publicationId]);

  const neighborhood = neighborhoods.find((item) => item.id === member?.neighborhoodId) ?? null;
  const canSubmit =
    !submitting &&
    Boolean(publication) &&
    Boolean(title.trim()) &&
    Boolean(body.trim()) &&
    (scope === "GLOBAL" || Boolean(neighborhood));

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canSubmit || !publication) return;
    setSubmitting(true);
    setError(null);
    try {
      const edited = await publicationApi.edit(publication.id, {
        title: title.trim(),
        body: body.trim(),
        scope,
        version: publication.version,
        ...(scope === "LOCAL" && neighborhood ? { neighborhoodId: neighborhood.id } : {}),
        animalInterestCode: animalInterestCode || null,
      });
      navigate(`/posts/${edited.id}`, { replace: true });
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 409) {
        setError("다른 곳에서 게시글이 변경되었습니다. 최신 내용을 다시 불러와 수정해 주세요.");
      } else if (requestError instanceof ApiError && requestError.status === 403) {
        setError("작성자만 이 게시글을 수정할 수 있습니다.");
      } else if (requestError instanceof ApiError && requestError.status === 401) {
        navigate(`/login?next=/posts/${publication.id}/edit`, { replace: true });
      } else if (requestError instanceof ApiError && requestError.status === 400) {
        setError("제목, 본문과 공개 범위를 확인해 주세요.");
      } else {
        setError("게시글을 수정하지 못했습니다.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return (
      <main className="page publication-page">
        <section className="surface-card" role="status">게시글 수정 정보를 불러오는 중...</section>
      </main>
    );
  }

  if (!publication || !member) {
    return (
      <main className="page publication-page publication-state-page">
        <section className="surface-card">
          <p className="eyebrow">글 수정</p>
          <h1>게시글을 수정할 수 없습니다</h1>
          <p className="form-error" role="alert">{error}</p>
          <Link className="button button-soft" to={`/posts/${publicationId}`}>게시글로 돌아가기</Link>
        </section>
      </main>
    );
  }

  return (
    <main className="page publication-page">
      <section className="publication-hero">
        <div>
          <p className="eyebrow">글 수정</p>
          <h1>게시글 수정</h1>
          <p>현재 공개 범위와 내용을 확인한 뒤 변경 사항을 저장해 주세요.</p>
        </div>
        <div className="publication-hero-actions">
          <span className="publication-chip">작성자 수정</span>
          <Link className="publication-text-link" to={`/posts/${publication.id}`}>게시글로 돌아가기</Link>
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
            <label>
              관심 동물 분류 (선택)
              <select value={animalInterestCode} onChange={(event) => setAnimalInterestCode(event.target.value)}>
                <option value="">일반 글</option>
                {ANIMAL_INTEREST_GROUPS.flatMap((group) => group.options).map((option) => (
                  <option key={option.code} value={option.code}>{option.label}</option>
                ))}
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
              />
              <span className="field-help">{body.length.toLocaleString()}/{BODY_MAX_LENGTH.toLocaleString()}</span>
            </label>
          </section>

          <aside className="surface-card publication-policy">
            <p className="eyebrow">수정 안내</p>
            <p>저장 시 읽어 온 버전을 검사해 다른 곳의 변경을 조용히 덮어쓰지 않습니다.</p>
            <div>
              <strong>저장 전 확인</strong>
              <ul>
                <li>공개 범위를 바꾸면 피드 노출 대상도 달라집니다.</li>
                <li>개인정보와 민감한 연락처는 공개하지 마세요.</li>
              </ul>
            </div>
          </aside>
        </div>

        {error ? <p className="form-error publication-error" role="alert">{error}</p> : null}
        <footer className="publication-submit-row">
          <Link className="publication-text-link" to={`/posts/${publication.id}`}>취소</Link>
          <button className="button button-primary" type="submit" disabled={!canSubmit}>
            {submitting ? "저장 중..." : "변경 사항 저장"}
          </button>
        </footer>
      </form>
    </main>
  );
}
