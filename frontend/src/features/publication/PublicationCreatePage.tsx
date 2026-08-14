import { FormEvent, useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import {
  ApiError,
  publicationApi,
  mediaApi,
  type Publication,
} from "../../api/client";
import AnimalCommunitySelector, { initialAnimalCommunityCodes } from "../member/AnimalCommunitySelector";
import { useAuth } from "../../auth/AuthContext";

const TITLE_MAX_LENGTH = 120;
const BODY_MAX_LENGTH = 20_000;

export default function PublicationCreatePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const { member, status: authStatus } = useAuth();
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const initialBoard = searchParams.get("board") ?? "";
  const boardType: Publication["type"] = initialBoard === "questions" ? "QA_QUESTION" : initialBoard === "showcase" ? "PET_SHOWCASE" : initialBoard === "product-reviews" ? "PRODUCT_REVIEW" : "FREE_BOARD";
  const [publicationType] = useState<Publication["type"]>(boardType);
  const [animalCommunityCodes, setAnimalCommunityCodes] = useState<string[]>(() => initialAnimalCommunityCodes(searchParams));
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [partialPublicationId, setPartialPublicationId] = useState<string | null>(null);

  const loading = authStatus === "loading";
  const loginPath = `/login?next=${encodeURIComponent(`${location.pathname}${location.search}`)}`;
  useEffect(() => {
    if (authStatus === "anonymous") navigate(loginPath, { replace: true });
  }, [authStatus, loginPath, navigate]);
  useEffect(() => {
    if (authStatus === "error") setError("로그인 상태를 확인하지 못했습니다.");
  }, [authStatus]);

  const canSubmit =
    !submitting &&
    Boolean(title.trim()) &&
    Boolean(body.trim());

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canSubmit) return;
    setSubmitting(true);
    setError(null);
    setPartialPublicationId(null);
    let createdPublicationId: string | null = null;
    try {
      const publication = await publicationApi.create({
        title: title.trim(),
        body: body.trim(),
        ...(publicationType !== "FREE_BOARD" ? { type: publicationType } : {}),
        ...(animalCommunityCodes.length
          ? { animalInterestCode: animalCommunityCodes[0], animalCommunityCodes }
          : {}),
      });
      createdPublicationId = publication.id;
      if (file) {
        const bytes = await file.arrayBuffer();
        const digest = await crypto.subtle.digest("SHA-256", bytes);
        const checksum = Array.from(new Uint8Array(digest), (item) => item.toString(16).padStart(2, "0")).join("");
        const asset = await mediaApi.create({ checksumSha256: checksum, contentType: file.type, byteSize: file.size });
        await mediaApi.uploadPresigned(asset, file);
        await mediaApi.finalize(asset.id, checksum);
        await mediaApi.attach(asset.id, publication.id);
      }
      navigate(`/posts/${publication.id}`);
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        navigate(loginPath, { replace: true });
        return;
      }
      setError(
        createdPublicationId
          ? "글은 등록됐지만 첨부 파일을 연결하지 못했습니다. 아래 글에서 내용을 확인한 뒤 필요하면 다시 첨부해 주세요."
          : requestError instanceof ApiError && requestError.status === 400
          ? "제목과 본문을 확인해 주세요."
          : "글을 등록하지 못했습니다. 잠시 후 다시 시도해 주세요.",
      );
      if (createdPublicationId) setPartialPublicationId(createdPublicationId);
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
          <Link className="button button-soft" to="/my-posts">작성글로 돌아가기</Link>
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
          <p>모든 방문자가 볼 수 있는 게시글을 작성해 주세요.</p>
        </div>
        <div className="publication-hero-actions">
          <span className="publication-chip">회원 작성</span>
          <Link className="publication-text-link" to="/my-posts">작성글로 돌아가기</Link>
        </div>
      </section>

      <form className="publication-form" onSubmit={submit} noValidate>
        <div className="publication-form-grid">
          <section className="surface-card publication-fields">
            <div className="publication-section-title">글 정보</div>
            <label>
              분류
              <select value={publicationType} disabled>
                <option value="FREE_BOARD">자유게시판</option>
                <option value="QA_QUESTION">질문·답변</option>
                <option value="PET_SHOWCASE">반려동물 자랑</option>
                <option value="PRODUCT_REVIEW">용품 후기</option>
              </select>
            </label>
            <AnimalCommunitySelector value={animalCommunityCodes} onChange={setAnimalCommunityCodes} />
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
            <label>
              첨부 파일 (선택)
              <input type="file" accept="image/jpeg,image/png,image/gif,application/pdf" onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
              <span className="field-help">이미지·PDF, 최대 10MB</span>
            </label>
          </section>

          <aside className="surface-card publication-policy">
            <p className="eyebrow">작성 기준</p>
            <p>자유게시판 글은 전체 커뮤니티에 공개됩니다.</p>
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

        {error ? <p className="form-error publication-error" role="alert">{error} {partialPublicationId ? <Link className="publication-text-link" to={`/posts/${partialPublicationId}`}>등록된 글 열기</Link> : null}</p> : null}
        <footer className="publication-submit-row">
          <Link className="publication-text-link" to="/my-posts">취소</Link>
          <button className="button button-primary" type="submit" disabled={!canSubmit}>
            {submitting ? "등록 중..." : "등록"}
          </button>
        </footer>
      </form>
    </main>
  );
}
