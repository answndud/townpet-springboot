import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { authApi, memberApi } from "./api/client";
import type { Member } from "./api/client";

export default function ProfilePage() {
  const navigate = useNavigate();
  const [member, setMember] = useState<Member | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loggingOut, setLoggingOut] = useState(false);

  useEffect(() => {
    memberApi
      .current()
      .then(setMember)
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
            <h2>반려동물</h2>
            <ul>{member.pets.map((pet) => <li key={pet.id}>{pet.name} · {pet.species}</li>)}</ul>
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
