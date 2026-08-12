import { FormEvent, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  ApiError,
  lostFoundApi,
  type LostFoundAlert,
  type LostFoundAlertKind,
  type LostFoundSighting,
} from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import { useAbortableRequest } from "../../hooks/useAbortableRequest";

const kindLabel: Record<LostFoundAlertKind, string> = { LOST: "분실", FOUND: "발견" };
const statusLabel = { ACTIVE: "진행 중", RESOLVED: "해결", CLOSED: "종료" } as const;
const dateFormat = new Intl.DateTimeFormat("ko-KR", { month: "long", day: "numeric", hour: "2-digit", minute: "2-digit" });
const dateText = (value: string) => dateFormat.format(new Date(value));

function LostFoundHero({ canWrite }: { canWrite: boolean }) {
  return <header className="lostfound-hero"><div><p className="eyebrow">LOST & FOUND</p><h1>분실·목격 제보</h1><p>근처의 분실·발견 소식을 확인하고, 목격 정보를 안전하게 공유하세요.</p></div>{canWrite ? <Link className="button button-primary" to="/lost-found/new">제보 등록</Link> : null}</header>;
}

export function LostFoundListPage() {
  const [kind, setKind] = useState<LostFoundAlertKind | "">("");
  const [latitude, setLatitude] = useState("");
  const [longitude, setLongitude] = useState("");
  const [radius, setRadius] = useState("5000");
  const [submitted, setSubmitted] = useState({ latitude: "", longitude: "", radius: "5000" });
  const { member } = useAuth();
  const invalidLocation = Boolean(submitted.latitude.trim() || submitted.longitude.trim()) && (!submitted.latitude.trim() || !submitted.longitude.trim());
  const { data: items, error: requestError, loading } = useAbortableRequest<LostFoundAlert[]>((signal) => invalidLocation
    ? Promise.resolve([])
    : lostFoundApi.list({ kind: kind || undefined, latitude: submitted.latitude ? Number(submitted.latitude) : undefined, longitude: submitted.longitude ? Number(submitted.longitude) : undefined, radiusMeters: submitted.latitude ? Number(submitted.radius) : undefined, limit: 30, signal }), [kind, invalidLocation, submitted.latitude, submitted.longitude, submitted.radius]);
  const alerts = items ?? [];
  const viewerRole = member?.role ?? null;
  const error = invalidLocation ? "반경 검색에는 위도와 경도를 모두 입력해 주세요." : requestError ? "분실·목격 목록을 불러오지 못했습니다." : null;

  return (
    <main className="page lostfound-page">
      <LostFoundHero canWrite={viewerRole !== "MODERATOR"} />
      <div className="lostfound-toolbar">
        <div className="lostfound-filters">
          <button className={!kind ? "market-filter active" : "market-filter"} aria-pressed={!kind} onClick={() => setKind("")} type="button">전체</button>
          <button className={kind === "LOST" ? "market-filter active" : "market-filter"} aria-pressed={kind === "LOST"} onClick={() => setKind("LOST")} type="button">분실</button>
          <button className={kind === "FOUND" ? "market-filter active" : "market-filter"} aria-pressed={kind === "FOUND"} onClick={() => setKind("FOUND")} type="button">발견</button>
        </div>
        <span className="marketplace-count">{alerts.length}건</span>
      </div>
      <form className="lostfound-radius" onSubmit={(event) => { event.preventDefault(); setSubmitted({ latitude, longitude, radius }); }}>
        <label>위도<input inputMode="decimal" value={latitude} onChange={(event) => setLatitude(event.target.value)} placeholder="예: 37.55" /></label>
        <label>경도<input inputMode="decimal" value={longitude} onChange={(event) => setLongitude(event.target.value)} placeholder="예: 126.91" /></label>
        <label>반경(m)<input type="number" min="1" max="100000" value={radius} onChange={(event) => setRadius(event.target.value)} /></label>
        <button className="button button-soft" type="submit">근처 검색</button>
      </form>
      {error ? <p className="form-error marketplace-error" role="alert">{error}</p> : null}
      {loading ? <section className="surface-card" role="status">목록을 불러오는 중...</section> : null}
      {!loading && !alerts.length && !error ? <section className="surface-card marketplace-empty"><h2>등록된 제보가 없습니다</h2><p>새로운 분실·발견 소식을 등록해 주세요.</p></section> : null}
      <section className="lostfound-grid" aria-label="분실·목격 목록">
        {alerts.map((item) => <Link className="surface-card lostfound-card" key={item.id} to={`/lost-found/${item.id}`}><div className="marketplace-card-meta"><span className="publication-chip publication-chip-primary">{kindLabel[item.kind]}</span><span className="publication-chip">{statusLabel[item.status]}</span></div><h2>{item.title}</h2><p>{item.description}</p><small>마지막 확인 {dateText(item.lastSeenAt)} · 근사 위치 {item.approximateLocation.latitude.toFixed(3)}, {item.approximateLocation.longitude.toFixed(3)}</small></Link>)}
      </section>
    </main>
  );
}

export function LostFoundDetailPage() {
  const { alertId = "" } = useParams();
  const navigate = useNavigate();
  const { member } = useAuth();
  const { data, error: requestError, loading, retry } = useAbortableRequest<{ alert: LostFoundAlert; sightings: LostFoundSighting[] }>((signal) => Promise.all([lostFoundApi.detail(alertId, signal), lostFoundApi.sightings(alertId, 50, signal)]).then(([loaded, reports]) => ({ alert: loaded, sightings: reports })), [alertId]);
  const [pendingStatus, setPendingStatus] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const alert = data?.alert ?? null;
  const sightings = data?.sightings ?? [];
  const error = requestError instanceof ApiError && requestError.status === 404 ? "제보를 찾을 수 없습니다." : requestError ? "제보를 불러오지 못했습니다." : actionError;
  async function changeStatus(status: "ACTIVE" | "RESOLVED" | "CLOSED") { if (!alert || pendingStatus) return; setPendingStatus(status); setActionError(null); try { const input = status === "ACTIVE" ? { status, reopenReason: "추가 목격 제보" } : status === "RESOLVED" ? { status, resolutionOutcome: "보호자에게 인계됨" } : { status, closeReason: "제보 종료" }; await lostFoundApi.changeStatus(alert.id, input); retry(); } catch (requestError) { if (requestError instanceof ApiError && requestError.status === 401) { navigate(`/login?next=/lost-found/${alert.id}`); return; } setActionError("상태를 변경하지 못했습니다."); } finally { setPendingStatus(null); } }
  if (loading) return <main className="page lostfound-page"><section className="surface-card" role="status">제보를 불러오는 중...</section></main>;
  if (!alert || error) return <main className="page lostfound-page marketplace-state"><section className="surface-card"><h1>{error ?? "제보를 찾을 수 없습니다"}</h1><Link className="button button-soft" to="/lost-found">목록으로</Link></section></main>;
  const owner = member?.role === "MEMBER" && member.id === alert.reporterMemberId;
  return <main className="page lostfound-page"><div className="marketplace-detail-nav"><Link className="publication-text-link" to="/lost-found">← 제보 목록</Link>{member?.role === "MEMBER" ? <Link className="button button-soft" to="/lost-found/new">새 제보</Link> : null}</div><article className="surface-card lostfound-detail"><div className="marketplace-card-meta"><span className="publication-chip publication-chip-primary">{kindLabel[alert.kind]}</span><span className="publication-chip">{statusLabel[alert.status]}</span></div><h1>{alert.title}</h1><p className="lostfound-description">{alert.description}</p><p className="lostfound-meta">마지막 확인 {dateText(alert.lastSeenAt)} · 공개 위치는 약 250m 근사값입니다.</p><div className="lostfound-sightings"><div className="section-heading-row"><div><p className="eyebrow">SIGHTINGS</p><h2>목격 제보 {sightings.length}건</h2></div>{member?.role === "MEMBER" && alert.status === "ACTIVE" ? <Link className="button button-primary" to={`/lost-found/${alert.id}/sightings/new`}>목격 제보</Link> : null}</div>{!sightings.length ? <p className="publication-comments-state">아직 목격 제보가 없습니다.</p> : <div className="lostfound-sighting-list">{sightings.map((sighting) => <article className="lostfound-sighting" key={sighting.id}><strong>{dateText(sighting.seenAt)}</strong><p>{sighting.description}</p><small>근사 위치 {sighting.approximateLocation.latitude.toFixed(3)}, {sighting.approximateLocation.longitude.toFixed(3)}</small>{owner ? <Link className="publication-text-link" to={`/lost-found/sightings/${sighting.id}/exact`}>정확 위치 보기</Link> : null}</article>)}</div>}</div>{owner && alert.status !== "CLOSED" ? <div className="marketplace-actions">{alert.status === "ACTIVE" ? <button className="button button-soft" type="button" disabled={Boolean(pendingStatus)} onClick={() => void changeStatus("RESOLVED")}>해결 처리</button> : <button className="button button-soft" type="button" disabled={Boolean(pendingStatus)} onClick={() => void changeStatus("ACTIVE")}>재개</button>}<button className="button button-danger" type="button" disabled={Boolean(pendingStatus)} onClick={() => void changeStatus("CLOSED")}>종료</button></div> : null}{error ? <p className="form-error marketplace-error" role="alert">{error}</p> : null}</article></main>;
}

export function LostFoundAlertFormPage() {
  const navigate = useNavigate();
  const [kind, setKind] = useState<LostFoundAlertKind>("LOST"); const [title, setTitle] = useState(""); const [description, setDescription] = useState(""); const [latitude, setLatitude] = useState(""); const [longitude, setLongitude] = useState(""); const [lastSeenAt, setLastSeenAt] = useState(new Date().toISOString().slice(0, 16)); const [error, setError] = useState<string | null>(null); const [saving, setSaving] = useState(false);
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); setSaving(true); setError(null); try { const item = await lostFoundApi.create({ kind, title: title.trim(), description: description.trim(), latitude: Number(latitude), longitude: Number(longitude), lastSeenAt: new Date(lastSeenAt).toISOString() }); navigate(`/lost-found/${item.id}`); } catch (requestError) { if (requestError instanceof ApiError && requestError.status === 401) { navigate("/login?next=/lost-found/new"); return; } setError("제목, 설명과 위치를 확인해 주세요."); } finally { setSaving(false); } }
  return <main className="page lostfound-page"><section className="lostfound-hero"><div><p className="eyebrow">LOST & FOUND</p><h1>새 분실·발견 제보</h1><p>공개 위치는 근사값으로 저장됩니다. 정확한 위치는 alert 작성자만 확인할 수 있습니다.</p></div></section><form className="surface-card lostfound-form" onSubmit={submit}><label>유형<select value={kind} onChange={(event) => setKind(event.target.value as LostFoundAlertKind)}><option value="LOST">분실</option><option value="FOUND">발견</option></select></label><label>제목<input required maxLength={120} value={title} onChange={(event) => setTitle(event.target.value)} /></label><label>설명<textarea required maxLength={5000} value={description} onChange={(event) => setDescription(event.target.value)} /></label><label>마지막 확인 시각<input required type="datetime-local" value={lastSeenAt} onChange={(event) => setLastSeenAt(event.target.value)} /></label><div className="lostfound-coordinate-grid"><label>위도<input required inputMode="decimal" value={latitude} onChange={(event) => setLatitude(event.target.value)} placeholder="37.55" /></label><label>경도<input required inputMode="decimal" value={longitude} onChange={(event) => setLongitude(event.target.value)} placeholder="126.91" /></label></div>{error ? <p className="form-error marketplace-error" role="alert">{error}</p> : null}<div className="publication-submit-row"><Link className="publication-text-link" to="/lost-found">취소</Link><button className="button button-primary" disabled={saving} type="submit">{saving ? "등록 중..." : "제보 등록"}</button></div></form></main>;
}

export function LostFoundSightingFormPage() {
  const { alertId = "" } = useParams(); const navigate = useNavigate(); const [description, setDescription] = useState(""); const [latitude, setLatitude] = useState(""); const [longitude, setLongitude] = useState(""); const [seenAt, setSeenAt] = useState(new Date().toISOString().slice(0, 16)); const [error, setError] = useState<string | null>(null); const [saving, setSaving] = useState(false);
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); setSaving(true); try { await lostFoundApi.createSighting(alertId, { description: description.trim(), latitude: Number(latitude), longitude: Number(longitude), seenAt: new Date(seenAt).toISOString() }); navigate(`/lost-found/${alertId}`); } catch (requestError) { if (requestError instanceof ApiError && requestError.status === 401) { navigate(`/login?next=/lost-found/${alertId}/sightings/new`); return; } setError(requestError instanceof ApiError && requestError.status === 409 ? "종료된 alert에는 목격 제보를 추가할 수 없습니다." : "목격 제보를 저장하지 못했습니다."); } finally { setSaving(false); } }
  return <main className="page lostfound-page"><section className="lostfound-hero"><div><p className="eyebrow">SIGHTING REPORT</p><h1>목격 제보 남기기</h1><p>공개 목록에는 근사 위치만 표시됩니다.</p></div></section><form className="surface-card lostfound-form" onSubmit={submit}><label>목격 시각<input required type="datetime-local" value={seenAt} onChange={(event) => setSeenAt(event.target.value)} /></label><label>설명<textarea required maxLength={2000} value={description} onChange={(event) => setDescription(event.target.value)} placeholder="어디에서 무엇을 보았는지 적어 주세요." /></label><div className="lostfound-coordinate-grid"><label>위도<input required inputMode="decimal" value={latitude} onChange={(event) => setLatitude(event.target.value)} /></label><label>경도<input required inputMode="decimal" value={longitude} onChange={(event) => setLongitude(event.target.value)} /></label></div>{error ? <p className="form-error marketplace-error" role="alert">{error}</p> : null}<div className="publication-submit-row"><Link className="publication-text-link" to={`/lost-found/${alertId}`}>취소</Link><button className="button button-primary" disabled={saving} type="submit">{saving ? "저장 중..." : "제보 등록"}</button></div></form></main>;
}

export function LostFoundExactLocationPage() {
  const { sightingId = "" } = useParams();
  const { data: location, error: requestError } = useAbortableRequest<{ sightingId: string; latitude: number; longitude: number }>((signal) => lostFoundApi.exactLocation(sightingId, signal), [sightingId]);
  const error = requestError ? "정확 위치는 alert 작성자만 확인할 수 있습니다." : null;
  return <main className="page lostfound-page marketplace-state"><section className="surface-card"><p className="eyebrow">OWNER ONLY</p><h1>정확 위치 evidence</h1>{location ? <p className="lostfound-exact">{location.latitude}, {location.longitude}<br /><small>이 조회는 접근 audit에 기록되었습니다.</small></p> : <p className="form-error" role="alert">{error ?? "정확 위치를 확인하는 중..."}</p>}<Link className="button button-soft" to="/lost-found">목록으로</Link></section></main>;
}
