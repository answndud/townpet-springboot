import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError, memberApi, publicationApi, type Member, type PublicMember, type Relationship } from "./api/client";

export default function PublicMemberProfilePage() {
  const { memberId = "" } = useParams();
  const navigate = useNavigate();
  const [profile, setProfile] = useState<PublicMember | null>(null);
  const [viewer, setViewer] = useState<Member | null>(null);
  const [relationship, setRelationship] = useState<Relationship | null>(null);
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
        setError(requestError instanceof ApiError && requestError.status === 404 ? "존재하지 않는 회원입니다." : "프로필을 불러오지 못했습니다.");
      });
    memberApi.current(controller.signal)
      .then((current) => {
        setViewer(current);
        if (current.id !== memberId) {
          return publicationApi.relationship(memberId, controller.signal).then(setRelationship);
        }
        return undefined;
      })
      .catch(() => setViewer(null));
    return () => controller.abort();
  }, [memberId]);

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
      if (requestError instanceof ApiError && requestError.status === 401) {
        navigate(`/login?next=/members/${memberId}`, { replace: true });
      } else {
        setError("관계 변경을 처리하지 못했습니다.");
      }
    } finally {
      setSaving(false);
    }
  }

  if (error && !profile) {
    return <main className="page placeholder-page"><section className="surface-card"><p role="alert">{error}</p><Link className="button button-soft" to="/feed/guest">게시판으로</Link></section></main>;
  }
  if (!profile) {
    return <main className="page placeholder-page"><section className="surface-card" role="status">프로필을 불러오는 중...</section></main>;
  }
  const ownProfile = viewer?.id === profile.id;
  return (
    <main className="page placeholder-page">
      <section className="surface-card profile-card">
        <span className="eyebrow">TOWNPET MEMBER</span>
        <h1>{profile.nickname}님의 프로필</h1>
        <p>{profile.bio ?? "아직 소개가 없어요."}</p>
        <h2>반려동물</h2>
        {profile.pets.length ? <ul>{profile.pets.map((pet) => <li key={pet.id}>{pet.name} · {pet.species}</li>)}</ul> : <p>등록된 반려동물이 없어요.</p>}
        {relationship && !ownProfile ? (
          <div className="profile-actions">
            <button className="button button-primary" type="button" disabled={saving} onClick={() => toggle("following")}>{relationship.following ? "팔로잉" : "팔로우"}</button>
            <button className="button button-soft" type="button" disabled={saving} onClick={() => toggle("blocking")}>{relationship.blocking ? "차단 해제" : "차단"}</button>
          </div>
        ) : null}
        {error ? <p className="form-error" role="alert">{error}</p> : null}
      </section>
    </main>
  );
}
