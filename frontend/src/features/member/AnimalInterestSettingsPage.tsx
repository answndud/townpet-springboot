import { Link } from "react-router-dom";
import { AnimalInterestSettings } from "./AnimalInterestMenu";

export default function AnimalInterestSettingsPage() {
  return (
    <main className="page interest-settings-page">
      <header className="community-hero">
        <div>
          <p className="eyebrow">ANIMAL BOARDS</p>
          <h1>동물 게시판 관리</h1>
          <p>자주 방문할 동물 게시판을 정하는 설정입니다. 공통게시판의 글은 이 설정과 관계없이 모두에게 열려 있습니다.</p>
        </div>
        <Link className="button button-soft" to="/animals/all">동물 게시판으로 돌아가기</Link>
      </header>
      <section className="surface-card interest-settings-card">
        <AnimalInterestSettings embedded />
      </section>
    </main>
  );
}
