import { FormEvent, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError, gatheringApi, type Gathering } from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import { useAbortableRequest } from "../../hooks/useAbortableRequest";
import { formatDateMediumTime } from "../../utils/date";

function date(value: string) { return formatDateMediumTime(value); }

export function GatheringListPage() {
  const { member } = useAuth();
  const { data: items, error: requestError, loading } = useAbortableRequest<Gathering[]>((signal) => gatheringApi.list(signal), []);
  const error = requestError ? "모임을 불러오지 못했습니다." : null;
  const gatherings = items ?? [];
  return <main className="page gathering-page"><section className="localcare-hero"><p className="eyebrow">TOWNPET GATHERING</p><h1>함께 걷고 함께 배우는 모임</h1><p>동네 반려생활 모임의 일정과 남은 자리를 확인하세요.</p>{member?.role !== "MODERATOR" ? <Link className="button button-primary" to="/gatherings/new">모임 만들기</Link> : null}</section>{error ? <p role="alert">{error}</p> : null}<section className="gathering-grid" aria-busy={loading}>{gatherings.map((item) => <Link className="surface-card gathering-card" key={item.id} to={`/gatherings/${item.id}`}><span className="publication-chip publication-chip-primary">{item.participantCount}/{item.capacity}명</span><h2>{item.title}</h2><p>{item.description}</p><small>{date(item.startsAt)} · {item.location}</small></Link>)}</section>{!gatherings.length && !error && !loading ? <p className="surface-card">예정된 모임이 없습니다.</p> : null}</main>;
}

export function GatheringDetailPage() {
  const { gatheringId = "" } = useParams();
  const navigate = useNavigate();
  const { member } = useAuth();
  const { data: item, error: requestError, loading, retry } = useAbortableRequest<Gathering>((signal) => gatheringApi.detail(gatheringId, signal), [gatheringId]);
  const [actionError, setActionError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const error = requestError instanceof ApiError && requestError.status === 404 ? "모임을 찾을 수 없습니다." : requestError ? "모임을 불러오지 못했습니다." : actionError;
  async function change(action: "join" | "leave" | "cancel") { if (saving) return; setSaving(true); setActionError(null); try { await gatheringApi[action](gatheringId); retry(); } catch (requestError) { if (requestError instanceof ApiError && requestError.status === 401) navigate(`/login?next=/gatherings/${gatheringId}`); else setActionError(requestError instanceof ApiError ? requestError.message : "요청을 처리하지 못했습니다."); } finally { setSaving(false); } }
  if (error && !item) return <main className="page placeholder-page"><section className="surface-card"><p role="alert">{error}</p><Link className="button button-soft" to="/gatherings">목록으로</Link></section></main>;
  if (!item || loading) return <main className="page placeholder-page"><section className="surface-card" role="status">모임을 불러오는 중...</section></main>;
  return <main className="page gathering-page"><Link className="publication-text-link" to="/gatherings">목록으로</Link><article className="surface-card gathering-detail"><span className="publication-chip publication-chip-primary">{item.participantCount}/{item.capacity}명 참여</span><h1>{item.title}</h1><p>{item.description}</p><dl><dt>일시</dt><dd>{date(item.startsAt)}</dd><dt>장소</dt><dd>{item.location}</dd></dl>{error ? <p className="form-error" role="alert">{error}</p> : null}<div className="profile-actions">{member?.role === "MEMBER" && item.status === "ACTIVE" && !item.joined && member.id !== item.hostMemberId ? <button className="button button-primary" type="button" disabled={saving || item.participantCount >= item.capacity} onClick={() => void change("join")}>참여하기</button> : null}{member?.role === "MEMBER" && item.joined ? <button className="button button-soft" type="button" disabled={saving} onClick={() => void change("leave")}>참여 취소</button> : null}{member?.role === "MEMBER" && item.status === "ACTIVE" && member.id === item.hostMemberId ? <button className="button button-danger" type="button" disabled={saving} onClick={() => void change("cancel")}>모임 취소</button> : null}</div></article></main>;
}

export function GatheringCreatePage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ title: "", description: "", location: "", startsAt: "", capacity: "8" });
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  async function submit(event: FormEvent) { event.preventDefault(); if (saving) return; setSaving(true); setError(null); try { const created = await gatheringApi.create({ ...form, startsAt: new Date(form.startsAt).toISOString(), capacity: Number(form.capacity) }); navigate(`/gatherings/${created.id}`); } catch (requestError) { if (requestError instanceof ApiError && requestError.status === 401) navigate("/login?next=/gatherings/new"); else setError("모임을 만들지 못했습니다."); } finally { setSaving(false); } }
  return <main className="page gathering-page"><section className="surface-card publication-form"><p className="eyebrow">NEW GATHERING</p><h1>모임 만들기</h1><form onSubmit={submit}><label>제목<input required maxLength={160} value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></label><label>설명<textarea required maxLength={5000} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label><label>장소<input required maxLength={200} value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} /></label><label>일시<input required type="datetime-local" value={form.startsAt} onChange={(e) => setForm({ ...form, startsAt: e.target.value })} /></label><label>정원<input required min="2" max="100" type="number" value={form.capacity} onChange={(e) => setForm({ ...form, capacity: e.target.value })} /></label>{error ? <p role="alert">{error}</p> : null}<button className="button button-primary" disabled={saving} type="submit">{saving ? "생성 중..." : "모임 만들기"}</button></form></section></main>;
}
