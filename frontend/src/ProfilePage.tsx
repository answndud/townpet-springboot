import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiFetch, getCsrfToken } from "./api/client";

type Member = {
  nickname: string;
  bio: string | null;
  pets: Array<{ id: string; name: string; species: string }>;
};

export default function ProfilePage() {
  const navigate = useNavigate();
  const [member, setMember] = useState<Member | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<Member>("/api/v1/members/me")
      .then(setMember)
      .catch(() => setError("로그인이 만료되었습니다."));
  }, []);

  async function logout() {
    await getCsrfToken();
    await apiFetch<void>("/api/v1/auth/sessions/current", { method: "DELETE" });
    navigate("/login");
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
        <button className="button button-soft" onClick={logout} disabled={!member}>로그아웃</button>
      </section>
    </main>
  );
}
