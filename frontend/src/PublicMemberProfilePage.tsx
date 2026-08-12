import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError, memberApi, publicationApi, type PublicMember, type PublicMemberComment, type PublicMemberPublication, type PublicMemberReaction, type Relationship } from "./api/client";
import { useAuth } from "./auth/AuthContext";
import { useAbortableRequest } from "./hooks/useAbortableRequest";

type ActivityTab = "posts" | "comments" | "reactions";
type Activity = PublicMemberPublication[] | PublicMemberComment[] | PublicMemberReaction[];

export default function PublicMemberProfilePage() {
  const { memberId = "" } = useParams();
  const navigate = useNavigate();
  const { member: viewer } = useAuth();
  const { data: profile, error: profileError, loading: profileLoading } = useAbortableRequest<PublicMember>((signal) => memberApi.profile(memberId, signal), [memberId]);
  const [tab, setTab] = useState<ActivityTab>("posts");
  const { data: activity, loading: activityLoading } = useAbortableRequest<Activity>((signal) => tab === "posts" ? memberApi.publicPublications(memberId, signal) : tab === "comments" ? memberApi.publicComments(memberId, signal) : memberApi.publicReactions(memberId, signal), [memberId, tab]);
  const { data: relationship, retry: reloadRelationship } = useAbortableRequest<Relationship | null>((signal) => viewer && viewer.id !== memberId ? publicationApi.relationship(memberId, signal) : Promise.resolve(null), [viewer?.id, memberId]);
  const [saving, setSaving] = useState(false);
  const error = profileError instanceof ApiError && profileError.status === 401 ? "로그인이 필요합니다." : profileError instanceof ApiError && profileError.status === 404 ? "존재하지 않는 회원입니다." : profileError ? "프로필을 불러오지 못했습니다." : null;
  useEffect(() => {
    if (profileError instanceof ApiError && profileError.status === 401) navigate(`/login?next=/users/${encodeURIComponent(memberId)}`, { replace: true });
  }, [memberId, navigate, profileError]);
  if (error) return <main className="page placeholder-page"><section className="surface-card"><p role="alert">{error}</p><Link className="button button-soft" to="/feed/guest">게시판으로</Link></section></main>;
  if (!profile || profileLoading) return <main className="page placeholder-page"><section className="surface-card" role="status">프로필을 불러오는 중...</section></main>;
  const ownProfile = viewer?.id === profile.id;
  const tabVisible = tab === "posts" ? profile.showPublicPosts : tab === "comments" ? profile.showPublicComments : profile.showPublicReactions;
  async function toggle(kind: "following" | "blocking") {
    if (!relationship || saving) return;
    const next = { following: kind === "following" ? !relationship.following : relationship.following, blocking: kind === "blocking" ? !relationship.blocking : relationship.blocking };
    setSaving(true);
    try { await publicationApi.setRelationship(memberId, next.following, next.blocking); reloadRelationship(); }
    catch (requestError) { if (requestError instanceof ApiError && requestError.status === 401) navigate(`/login?next=/users/${memberId}`, { replace: true }); }
    finally { setSaving(false); }
  }
  return <main className="page placeholder-page"><section className="surface-card profile-card"><span className="eyebrow">TOWNPET MEMBER</span><h1>{profile.nickname}님의 프로필</h1><p>{profile.bio ?? "아직 소개가 없어요."}</p><h2>반려동물</h2>{profile.showPublicPets ? (profile.pets.length ? <ul>{profile.pets.map((pet) => <li key={pet.id}>{pet.name} · {pet.species}</li>)}</ul> : <p>등록된 반려동물이 없어요.</p>) : <p>반려동물 정보는 비공개입니다.</p>}{relationship && !ownProfile ? <div className="profile-actions"><button className="button button-primary" type="button" disabled={saving} onClick={() => void toggle("following")}>{relationship.following ? "팔로잉" : "팔로우"}</button><button className="button button-soft" type="button" disabled={saving} onClick={() => void toggle("blocking")}>{relationship.blocking ? "차단 해제" : "차단"}</button></div> : null}<section className="profile-activity" aria-label="회원 활동"><div className="profile-actions"><button className="button button-soft" type="button" onClick={() => setTab("posts")} aria-pressed={tab === "posts"}>작성글</button><button className="button button-soft" type="button" onClick={() => setTab("comments")} aria-pressed={tab === "comments"}>댓글</button><button className="button button-soft" type="button" onClick={() => setTab("reactions")} aria-pressed={tab === "reactions"}>반응</button></div>{!tabVisible ? <p>이 활동은 비공개입니다.</p> : activityLoading ? <p role="status">활동을 불러오는 중...</p> : !activity?.length ? <p>공개된 활동이 없습니다.</p> : null}{tab === "posts" && tabVisible ? <ul>{(activity as PublicMemberPublication[] | undefined)?.map((item) => <li key={item.id}><Link to={`/posts/${item.id}`}>{item.title}</Link><p>{item.body}</p></li>)}</ul> : null}{tab === "comments" && tabVisible ? <ul>{(activity as PublicMemberComment[] | undefined)?.map((item) => <li key={item.id}><Link to={`/posts/${item.publicationId}`}>{item.body}</Link></li>)}</ul> : null}{tab === "reactions" && tabVisible ? <ul>{(activity as PublicMemberReaction[] | undefined)?.map((item) => <li key={`${item.publicationId}-${item.createdAt}`}><Link to={`/posts/${item.publicationId}`}>좋아요한 게시글 보기</Link></li>)}</ul> : null}</section></section></main>;
}
