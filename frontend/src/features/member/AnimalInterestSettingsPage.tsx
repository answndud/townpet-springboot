import { Link } from "react-router-dom";
import { AnimalInterestSettings } from "./AnimalInterestMenu";

export default function AnimalInterestSettingsPage() {
  return (
    <main className="page interest-settings-page">
      <header className="community-hero">
        <div>
          <p className="eyebrow">PERSONALIZATION</p>
          <h1>관심 동물 관리</h1>
          <p>관심 동물은 게시글을 숨기는 필터가 아니라, 자주 방문할 커뮤니티를 정하는 설정입니다.</p>
        </div>
        <Link className="button button-soft" to="/animals/all">커뮤니티로 돌아가기</Link>
      </header>
      <section className="surface-card interest-settings-card">
        <AnimalInterestSettings embedded />
      </section>
    </main>
  );
}
