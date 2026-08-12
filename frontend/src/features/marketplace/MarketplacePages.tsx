import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  ApiError,
  marketplaceApi,
  type MarketplaceListing,
  type MarketplaceListingKind,
  type MarketplaceListingStatus,
} from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import { useAbortableRequest } from "../../hooks/useAbortableRequest";
import { formatDateTime } from "../../utils/date";

const KIND_LABELS: Record<MarketplaceListingKind, string> = {
  SELL: "판매",
  RENT: "대여",
  SHARE: "나눔",
  GROUP_BUY: "공동구매",
};
const STATUS_LABELS: Record<MarketplaceListingStatus, string> = {
  AVAILABLE: "판매 중",
  RESERVED: "예약됨",
  COMPLETED: "거래 완료",
  CANCELLED: "취소됨",
};
function formatPrice(price: number | null) {
  return price === null ? "무료 나눔" : `${price.toLocaleString("ko-KR")}원`;
}

function MarketplaceHeader({ action = true }: { action?: boolean }) {
  return (
    <header className="marketplace-hero">
      <div>
        <p className="eyebrow">TOWNPET MARKETPLACE</p>
        <h1>반려생활 물품을 나누고 거래해요</h1>
        <p>결제 없이 판매·대여·나눔 조건을 정리하고 안전하게 상태를 공유합니다.</p>
      </div>
      {action ? <Link className="button button-primary" to="/marketplace/new">글 올리기</Link> : null}
    </header>
  );
}

export function MarketplaceListPage() {
  const [kind, setKind] = useState<MarketplaceListingKind | "">("");
  const { member } = useAuth();
  const { data: items, error: requestError, loading } = useAbortableRequest<MarketplaceListing[]>((signal) => marketplaceApi.list(kind || undefined, 30, signal), [kind]);
  const listings = items ?? [];
  const viewerRole = member?.role ?? null;
  const error = requestError ? "거래 목록을 불러오지 못했습니다." : null;

  return (
    <main className="page marketplace-page">
      <MarketplaceHeader action={viewerRole !== "MODERATOR"} />
      <div className="marketplace-toolbar">
        <div className="marketplace-filters" aria-label="거래 유형 필터">
          <button className={!kind ? "market-filter active" : "market-filter"} aria-pressed={!kind} type="button" onClick={() => setKind("")}>전체</button>
          {(Object.keys(KIND_LABELS) as MarketplaceListingKind[]).map((option) => (
            <button className={kind === option ? "market-filter active" : "market-filter"} aria-pressed={kind === option} key={option} type="button" onClick={() => setKind(option)}>{KIND_LABELS[option]}</button>
          ))}
        </div>
        <span className="marketplace-count">{listings.length}개</span>
      </div>
      {error ? <p className="form-error marketplace-error" role="alert">{error}</p> : null}
      {loading ? <section className="surface-card" role="status">거래 목록을 불러오는 중...</section> : null}
      {!loading && listings.length === 0 ? (
        <section className="surface-card marketplace-empty"><h2>아직 등록된 물품이 없습니다</h2><p>첫 번째 반려생활 물품을 올려 보세요.</p>{viewerRole !== "MODERATOR" ? <Link className="button button-soft" to="/marketplace/new">물품 올리기</Link> : null}</section>
      ) : null}
      {!loading && listings.length > 0 ? (
        <section className="marketplace-grid" aria-label="거래 목록">
          {listings.map((item) => (
            <Link className="surface-card marketplace-card" key={item.id} to={`/marketplace/${item.id}`}>
              <div className="marketplace-card-meta"><span className="publication-chip publication-chip-primary">{KIND_LABELS[item.kind]}</span><span className="publication-chip">{STATUS_LABELS[item.status]}</span></div>
              <h2>{item.title}</h2><p>{item.description}</p>
              <strong>{formatPrice(item.priceKrw)}</strong>
              <small>{formatDateTime(item.createdAt)}</small>
            </Link>
          ))}
        </section>
      ) : null}
    </main>
  );
}

export function MarketplaceDetailPage() {
  const { listingId = "" } = useParams();
  const navigate = useNavigate();
  const { member } = useAuth();
  const { data: listing, error: requestError, loading, retry } = useAbortableRequest<MarketplaceListing>((signal) => marketplaceApi.detail(listingId, signal), [listingId]);
  const [changing, setChanging] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const error = requestError instanceof ApiError && requestError.status === 404 ? "거래 정보를 찾을 수 없습니다." : requestError ? "거래 정보를 불러오지 못했습니다." : actionError;

  async function changeStatus(status: MarketplaceListingStatus) {
    if (!listing || changing) return;
    setChanging(true); setActionError(null);
    try { await marketplaceApi.changeStatus(listing.id, status, listing.version); retry(); }
    catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) { navigate(`/login?next=/marketplace/${listing.id}`); return; }
      setActionError(requestError instanceof ApiError && requestError.status === 409 ? "현재 상태에서는 변경할 수 없습니다." : "상태를 변경하지 못했습니다.");
    } finally { setChanging(false); }
  }

  if (loading) return <main className="page marketplace-page"><section className="surface-card" role="status">거래 정보를 불러오는 중...</section></main>;
  if (!listing || error) return <main className="page marketplace-page marketplace-state"><section className="surface-card"><p className="eyebrow">MARKETPLACE</p><h1>{error ?? "거래 정보를 찾을 수 없습니다"}</h1><Link className="button button-soft" to="/marketplace">목록으로</Link></section></main>;
  const owner = member?.role === "MEMBER" && member.id === listing.ownerMemberId;
  return (
    <main className="page marketplace-page">
      <div className="marketplace-detail-nav"><Link className="publication-text-link" to="/marketplace">← 거래 목록</Link>{owner && listing.status === "AVAILABLE" ? <Link className="button button-soft" to={`/marketplace/${listing.id}/edit`}>수정</Link> : null}</div>
      <article className="surface-card marketplace-detail-card">
        <div className="marketplace-card-meta"><span className="publication-chip publication-chip-primary">{KIND_LABELS[listing.kind]}</span><span className="publication-chip">{STATUS_LABELS[listing.status]}</span></div>
        <h1>{listing.title}</h1><p className="marketplace-price">{formatPrice(listing.priceKrw)}</p><p className="marketplace-description">{listing.description}</p>
        <div className="marketplace-detail-meta"><span>TownPet 회원</span><span>·</span><time dateTime={listing.createdAt}>{formatDateTime(listing.createdAt)}</time></div>
        {error ? <p className="form-error marketplace-error" role="alert">{error}</p> : null}
        {owner && (listing.status === "AVAILABLE" || listing.status === "RESERVED") ? <div className="marketplace-actions">
          {listing.status === "AVAILABLE" ? <button className="button button-primary" disabled={changing} onClick={() => changeStatus("RESERVED")}>예약 처리</button> : <button className="button button-soft" disabled={changing} onClick={() => changeStatus("AVAILABLE")}>예약 취소</button>}
          <button className="button button-soft" disabled={changing} onClick={() => changeStatus("COMPLETED")}>거래 완료</button>
          <button className="button button-danger" disabled={changing} onClick={() => changeStatus("CANCELLED")}>게시글 취소</button>
        </div> : null}
      </article>
    </main>
  );
}

export function MarketplaceFormPage({ edit = false, initialKind = "SELL" }: { edit?: boolean; initialKind?: MarketplaceListingKind }) {
  const { listingId = "" } = useParams();
  const navigate = useNavigate();
  const [kind, setKind] = useState<MarketplaceListingKind>(initialKind);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");
  const [version, setVersion] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { data: loadedItem, error: loadError, loading } = useAbortableRequest<MarketplaceListing | null>((signal) => edit ? marketplaceApi.detail(listingId, signal) : Promise.resolve(null), [edit, listingId]);
  useEffect(() => {
    if (!loadedItem) return;
    setKind(loadedItem.kind); setTitle(loadedItem.title); setDescription(loadedItem.description); setPrice(loadedItem.priceKrw === null ? "" : String(loadedItem.priceKrw)); setVersion(loadedItem.version);
  }, [loadedItem]);
  useEffect(() => { if (loadError) setError("수정할 listing을 불러오지 못했습니다."); }, [loadError]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const priceKrw = kind === "SHARE" ? null : Number(price);
    const invalidPrice = kind !== "SHARE" && (priceKrw === null || !Number.isInteger(priceKrw) || priceKrw < 0);
    if (!title.trim() || !description.trim() || invalidPrice) { setError("제목, 설명과 가격을 확인해 주세요."); return; }
    setSubmitting(true); setError(null);
    try {
      const input = { kind, title: title.trim(), description: description.trim(), priceKrw };
      const item = edit ? await marketplaceApi.update(listingId, { ...input, version }) : await marketplaceApi.create(input);
      navigate(`/marketplace/${item.id}`);
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) { navigate(`/login?next=${edit ? `/marketplace/${listingId}/edit` : "/marketplace/new"}`); return; }
      setError(requestError instanceof ApiError && requestError.status === 409 ? "예약 이후에는 거래 조건을 수정할 수 없습니다." : "등록 정보를 저장하지 못했습니다.");
    } finally { setSubmitting(false); }
  }

  if (edit && loading) return <main className="page marketplace-page"><section className="surface-card" role="status">수정 정보를 불러오는 중...</section></main>;
  return (
    <main className="page marketplace-page"><section className="marketplace-hero"><div><p className="eyebrow">MARKETPLACE</p><h1>{edit ? "거래 글 수정" : "새 거래 글"}</h1><p>가격과 상태를 분명하게 적어 이웃이 쉽게 판단할 수 있게 해 주세요.</p></div></section>
      <form className="surface-card marketplace-form" onSubmit={submit} noValidate>
        <label>거래 유형<select value={kind} disabled={edit} onChange={(event) => setKind(event.target.value as MarketplaceListingKind)}>{(Object.keys(KIND_LABELS) as MarketplaceListingKind[]).map((option) => <option key={option} value={option}>{KIND_LABELS[option]}</option>)}</select></label>
        <label>제목<input maxLength={120} value={title} onChange={(event) => setTitle(event.target.value)} placeholder="예: 강아지 이동장 판매해요" /></label>
        <label>설명<textarea maxLength={5000} value={description} onChange={(event) => setDescription(event.target.value)} placeholder="상태, 거래 방법과 주의사항을 적어 주세요." /></label>
        {kind === "SHARE" ? <p className="field-help marketplace-share-help">나눔은 가격 없이 등록됩니다.</p> : <label>가격(원)<input type="number" min="0" step="1" value={price} onChange={(event) => setPrice(event.target.value)} placeholder="0" /></label>}
        {error ? <p className="form-error marketplace-error" role="alert">{error}</p> : null}
        <div className="publication-submit-row"><Link className="publication-text-link" to={edit ? `/marketplace/${listingId}` : "/marketplace"}>취소</Link><button className="button button-primary" type="submit" disabled={submitting}>{submitting ? "저장 중..." : edit ? "수정 저장" : "등록"}</button></div>
      </form>
    </main>
  );
}
