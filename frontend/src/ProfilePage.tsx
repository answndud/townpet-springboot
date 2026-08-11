import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { authApi, memberApi } from "./api/client";
import type { Member } from "./api/client";

export default function ProfilePage() {
  const navigate = useNavigate();
  const [member, setMember] = useState<Member | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loggingOut, setLoggingOut] = useState(false);
  const [savingVisibility, setSavingVisibility] = useState(false);
  const [visibilitySaved, setVisibilitySaved] = useState(false);
  const [showPublicPosts, setShowPublicPosts] = useState(true);
  const [showPublicComments, setShowPublicComments] = useState(true);
  const [showPublicPets, setShowPublicPets] = useState(true);

  useEffect(() => {
    memberApi
      .current()
      .then((current) => {
        setMember(current);
        setShowPublicPosts(current.showPublicPosts);
        setShowPublicComments(current.showPublicComments);
        setShowPublicPets(current.showPublicPets);
      })
      .catch(() => setError("로그인이 만료되었습니다."));
  }, []);

  async function logout() {
    setLoggingOut(true);
    setError(null);
    try {
      await authApi.logout();
      navigate("/login");
    } catch {
      setError("로그아웃 요청을 처리하지 못했습니다.");
      setLoggingOut(false);
    }
  }

  async function saveVisibility(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!member) return;
    setSavingVisibility(true);
    setVisibilitySaved(false);
    setError(null);
    try {
      const updated = await memberApi.updateProfile({
        bio: member.bio ?? "",
        showPublicPosts,
        showPublicComments,
        showPublicPets,
      });
      setMember(updated);
      setVisibilitySaved(true);
    } catch {
      setError("공개 범위 설정을 저장하지 못했습니다.");
    } finally {
      setSavingVisibility(false);
    }
  }

  if (error) {
    return (
      <main className="page placeholder-page">
        <section className="surface-card">
          <p role="alert">{error}</p>
          <button className="button button-primary" onClick={() => navigate("/login")}>로그인으로</button>
        </section>
      </main>
    );
  }

  return (
    <main className="page placeholder-page">
      <section className="surface-card profile-card">
        <span className="eyebrow">MY TOWNPET</span>
        <h1>{member ? `${member.nickname}님의 프로필` : "프로필 불러오는 중..."}</h1>
        {member ? (
          <>
            <p>{member.bio ?? "아직 소개가 없어요."}</p>
            <div className="profile-actions">
              <Link className="button button-soft" to="/my-posts">내 작성글</Link>
              <Link className="button button-soft" to="/bookmarks">북마크</Link>
            </div>
            <h2>반려동물</h2>
            <ul>{member.pets.map((pet) => <li key={pet.id}>{pet.name} · {pet.species}</li>)}</ul>
            <form className="form-section" onSubmit={saveVisibility}>
              <h2>공개 범위</h2>
              <label><input type="checkbox" checked={showPublicPosts} onChange={(event) => setShowPublicPosts(event.target.checked)} /> 게시글 공개</label>
              <label><input type="checkbox" checked={showPublicComments} onChange={(event) => setShowPublicComments(event.target.checked)} /> 댓글 활동 공개</label>
              <label><input type="checkbox" checked={showPublicPets} onChange={(event) => setShowPublicPets(event.target.checked)} /> 반려동물 공개</label>
              {visibilitySaved ? <p className="form-success" role="status">공개 범위가 저장되었습니다.</p> : null}
              <button className="button button-soft" type="submit" disabled={savingVisibility}>{savingVisibility ? "저장 중..." : "공개 범위 저장"}</button>
            </form>
          </>
        ) : null}
        <div className="profile-actions">
          <Link className="button button-soft" to="/onboarding">
            내 동네 설정
          </Link>
          <button className="button button-soft" onClick={logout} disabled={!member || loggingOut}>
            {loggingOut ? "로그아웃 중..." : "로그아웃"}
          </button>
        </div>
      </section>
    </main>
  );
}
