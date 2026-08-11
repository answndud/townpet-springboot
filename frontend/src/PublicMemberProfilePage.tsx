import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  ApiError,
  memberApi,
  publicationApi,
  type Member,
  type PublicMember,
  type PublicMemberComment,
  type PublicMemberPublication,
  type PublicMemberReaction,
  type Relationship,
} from "./api/client";

type ActivityTab = "posts" | "comments" | "reactions";

export default function PublicMemberProfilePage() {
  const { memberId = "" } = useParams();
  const navigate = useNavigate();
  const [profile, setProfile] = useState<PublicMember | null>(null);
  const [viewer, setViewer] = useState<Member | null>(null);
  const [relationship, setRelationship] = useState<Relationship | null>(null);
  const [activity, setActivity] = useState<PublicMemberPublication[] | PublicMemberComment[] | PublicMemberReaction[]>([]);
  const [tab, setTab] = useState<ActivityTab>("posts");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    setProfile(null);
    setError(null);
    memberApi.profile(memberId, controller.signal)
      .then(setProfile)
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        if (requestError instanceof ApiError && requestError.status === 401) {
          navigate(`/login?next=/users/${encodeURIComponent(memberId)}`, { replace: true });
          return;
        }
        setError(requestError instanceof ApiError && requestError.status === 404 ? "존재하지 않는 회원입니다." : "프로필을 불러오지 못했습니다.");
      });
    memberApi.current(controller.signal)
      .then((current) => {
        setViewer(current);
        if (current.id !== memberId) return publicationApi.relationship(memberId, controller.signal).then(setRelationship);
        return undefined;
      })
      .catch(() => setViewer(null));
    return () => controller.abort();
  }, [memberId, navigate]);

  useEffect(() => {
    if (!profile) return;
    const controller = new AbortController();
    const request = tab === "posts"
      ? memberApi.publicPublications(memberId, controller.signal)
      : tab === "comments"
        ? memberApi.publicComments(memberId, controller.signal)
        : memberApi.publicReactions(memberId, controller.signal);
    request.then(setActivity).catch(() => setActivity([]));
    return () => controller.abort();
  }, [memberId, profile, tab]);

  async function toggle(kind: "following" | "blocking") {
    if (!relationship || saving) return;
    const next = {
      following: kind === "following" ? !relationship.following : relationship.following,
      blocking: kind === "blocking" ? !relationship.blocking : relationship.blocking,
    };
    setSaving(true);
    try {
      setRelationship(await publicationApi.setRelationship(memberId, next.following, next.blocking));
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) navigate(`/login?next=/users/${memberId}`, { replace: true });
      else setError("관계 변경을 처리하지 못했습니다.");
    } finally {
      setSaving(false);
    }
  }

  if (error && !profile) return <main className="page placeholder-page"><section className="surface-card"><p role="alert">{error}</p><Link className="button button-soft" to="/feed/guest">게시판으로</Link></section></main>;
  if (!profile) return <main className="page placeholder-page"><section className="surface-card" role="status">프로필을 불러오는 중...</section></main>;

  const ownProfile = viewer?.id === profile.id;
  const tabVisible = tab === "posts" ? profile.showPublicPosts : tab === "comments" ? profile.showPublicComments : true;
  return (
    <main className="page placeholder-page">
      <section className="surface-card profile-card">
        <span className="eyebrow">TOWNPET MEMBER</span>
        <h1>{profile.nickname}님의 프로필</h1>
        <p>{profile.bio ?? "아직 소개가 없어요."}</p>
        <h2>반려동물</h2>
        {profile.showPublicPets ? (profile.pets.length ? <ul>{profile.pets.map((pet) => <li key={pet.id}>{pet.name} · {pet.species}</li>)}</ul> : <p>등록된 반려동물이 없어요.</p>) : <p>반려동물 정보는 비공개입니다.</p>}
        {relationship && !ownProfile ? <div className="profile-actions"><button className="button button-primary" type="button" disabled={saving} onClick={() => toggle("following")}>{relationship.following ? "팔로잉" : "팔로우"}</button><button className="button button-soft" type="button" disabled={saving} onClick={() => toggle("blocking")}>{relationship.blocking ? "차단 해제" : "차단"}</button></div> : null}
        {error ? <p className="form-error" role="alert">{error}</p> : null}
        <section className="profile-activity" aria-label="회원 활동">
          <div className="profile-actions">
            <button className="button button-soft" type="button" onClick={() => setTab("posts")} aria-pressed={tab === "posts"}>작성글</button>
            <button className="button button-soft" type="button" onClick={() => setTab("comments")} aria-pressed={tab === "comments"}>댓글</button>
            <button className="button button-soft" type="button" onClick={() => setTab("reactions")} aria-pressed={tab === "reactions"}>반응</button>
          </div>
          {!tabVisible ? <p>이 활동은 비공개입니다.</p> : !activity.length ? <p>공개된 활동이 없습니다.</p> : null}
          {tab === "posts" && tabVisible ? <ul>{(activity as PublicMemberPublication[]).map((item) => <li key={item.id}><Link to={`/posts/${item.id}`}>{item.title}</Link><p>{item.body}</p></li>)}</ul> : null}
          {tab === "comments" && tabVisible ? <ul>{(activity as PublicMemberComment[]).map((item) => <li key={item.id}><Link to={`/posts/${item.publicationId}`}>{item.body}</Link></li>)}</ul> : null}
          {tab === "reactions" ? <ul>{(activity as PublicMemberReaction[]).map((item) => <li key={`${item.publicationId}-${item.createdAt}`}><Link to={`/posts/${item.publicationId}`}>좋아요한 게시글 보기</Link></li>)}</ul> : null}
        </section>
      </section>
    </main>
  );
}
