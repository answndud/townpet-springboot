import { Link, NavLink, Route, Routes, useLocation } from "react-router-dom";
import LoginPage from "./LoginPage";
import OnboardingPage from "./OnboardingPage";
import PasswordResetPage from "./PasswordResetPage";
import ProfilePage from "./ProfilePage";
import VerifyEmailPage from "./VerifyEmailPage";
import PublicMemberProfilePage from "./PublicMemberProfilePage";
import PublicationCreatePage from "./features/publication/PublicationCreatePage";
import PublicationDetailPage from "./features/publication/PublicationDetailPage";
import PublicationEditPage from "./features/publication/PublicationEditPage";
import PublicationFeedPage from "./features/publication/PublicationFeedPage";
import { MarketplaceDetailPage, MarketplaceFormPage, MarketplaceListPage } from "./features/marketplace/MarketplacePages";
import { LostFoundAlertFormPage, LostFoundDetailPage, LostFoundExactLocationPage, LostFoundListPage, LostFoundSightingFormPage } from "./features/lostfound/LostFoundPages";
import { LocalCareDetailPage, LocalCareListPage } from "./features/localcare/LocalCarePages";
import { GatheringCreatePage, GatheringDetailPage, GatheringListPage } from "./features/gathering/GatheringPages";
import NotificationPage from "./NotificationPage";
import LegalPage from "./LegalPage";
import PersonalPostsPage from "./features/publication/PersonalPostsPage";
import SearchPage from "./features/publication/SearchPage";
import AdminReportsPage from "./AdminReportsPage";

const TOPIC_LINKS = [
  ["지도 만들기", "/campaigns/neighborhood-map"],
  ["분실/목격", "/lost-found"],
  ["동물병원", "/feed/guest?type=HOSPITAL_REVIEW"],
  ["산책코스", "/feed/guest?type=WALK_ROUTE"],
  ["질문/답변", "/feed/guest?type=QA_QUESTION"],
  ["중고거래", "/marketplace"],
] as const;

function Header() {
  const location = useLocation();
  const publicHeader = ["/", "/guides", "/campaigns/neighborhood-map", "/towns", "/marketplace"].some(
    (path) => location.pathname === path || location.pathname.startsWith(`${path}/`),
  );
  const guestFeedHeader = location.pathname === "/feed/guest";

  return (
    <header className="site-header">
      <div className="header-inner">
        <Link className="brand" to="/" aria-label="TownPet 홈으로 이동">
          <img src="/townpet-logo.svg" alt="TownPet" />
        </Link>
        {publicHeader || guestFeedHeader ? (
          <nav aria-label="공개 안내 페이지 주요 이동">
            <NavLink to="/feed/guest">게시판</NavLink>
            <NavLink to="/login" data-testid="header-login-link-home">
              로그인
            </NavLink>
          </nav>
        ) : (
          <nav aria-label="주요 이동" className="desktop-nav">
            <NavLink to="/feed">게시판</NavLink>
            <NavLink to="/profile">내 프로필</NavLink>
            <NavLink to="/notifications">알림</NavLink>
            <NavLink to="/marketplace">거래</NavLink>
            <NavLink to="/login">로그인</NavLink>
          </nav>
        )}
      </div>
    </header>
  );
}

function HomePage() {
  return (
    <main className="page page-home">
      <section className="hero-section">
        <p className="eyebrow">동네 반려생활 정보</p>
        <h1>우리 동네 반려생활 정보</h1>
        <p className="hero-copy">
          동물병원, 산책코스, 분실동물, 입양, 중고거래 정보를 지역별로 찾고 공유하는 동네 반려생활
          정보 커뮤니티입니다.
        </p>
        <div className="hero-actions">
          <Link className="button button-primary" to="/feed/guest">
            전체 피드
          </Link>
          <Link className="button button-soft" to="/onboarding">
            내 동네 설정
          </Link>
        </div>
      </section>
      <section className="topic-section" aria-labelledby="topic-title">
        <div>
          <h2 id="topic-title">관심 주제</h2>
          <p>분실, 병원, 산책처럼 자주 찾는 정보를 바로 확인하세요.</p>
        </div>
        <div className="topic-list">
          {TOPIC_LINKS.map(([label, href]) => (
            <Link className="topic-chip" key={href} to={href}>
              {label}
            </Link>
          ))}
        </div>
      </section>
      <section className="preview-grid" aria-label="TownPet 주요 기능">
        <article className="surface-card">
          <span className="card-label">LOCAL FEED</span>
          <h2>우리 동네 소식을 한눈에</h2>
          <p>지역 기반 게시글과 반려생활 정보를 최신순으로 확인할 수 있어요.</p>
        </article>
        <article className="surface-card">
          <span className="card-label">SAFE COMMUNITY</span>
          <h2>함께 만드는 안전한 커뮤니티</h2>
          <p>필요한 정보만 차분하게 나누고, 서로의 반려생활을 응원합니다.</p>
        </article>
      </section>
    </main>
  );
}

function PlaceholderPage() {
  const location = useLocation();
  return (
    <main className="page placeholder-page">
      <section className="surface-card">
        <span className="eyebrow">SPRING BOOT MIGRATION</span>
        <h1>{location.pathname === "/login" ? "TownPet 로그인" : "TownPet 게시판"}</h1>
        <p>이 화면은 React·Vite shell에서 다음 vertical slice로 연결될 예정입니다.</p>
        <Link className="button button-primary" to="/">
          홈으로 돌아가기
        </Link>
      </section>
    </main>
  );
}

export default function App() {
  return (
    <div className="app-shell-bg">
      <Header />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/password/reset" element={<PasswordResetPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/onboarding" element={<OnboardingPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/notifications" element={<NotificationPage />} />
        <Route path="/privacy" element={<LegalPage />} />
        <Route path="/terms" element={<LegalPage />} />
        <Route path="/my-posts" element={<PersonalPostsPage />} />
        <Route path="/saved" element={<PersonalPostsPage saved />} />
        <Route path="/bookmarks" element={<PersonalPostsPage saved />} />
        <Route path="/search" element={<SearchPage />} />
        <Route path="/search/guest" element={<SearchPage guest />} />
        <Route path="/admin/reports" element={<AdminReportsPage />} />
        <Route path="/admin/reports/:reportId" element={<AdminReportsPage />} />
        <Route path="/guides" element={<LocalCareListPage />} />
        <Route path="/guides/:resourceId" element={<LocalCareDetailPage />} />
        <Route path="/gatherings" element={<GatheringListPage />} />
        <Route path="/gatherings/new" element={<GatheringCreatePage />} />
        <Route path="/gatherings/:gatheringId" element={<GatheringDetailPage />} />
        <Route path="/members/:memberId" element={<PublicMemberProfilePage />} />
        <Route path="/users/:memberId" element={<PublicMemberProfilePage />} />
        <Route path="/posts/new" element={<PublicationCreatePage />} />
        <Route path="/posts/:publicationId/edit" element={<PublicationEditPage />} />
        <Route path="/posts/:publicationId" element={<PublicationDetailPage />} />
        <Route path="/posts/:publicationId/guest" element={<PublicationDetailPage />} />
        <Route path="/feed" element={<PublicationFeedPage memberView />} />
        <Route path="/feed/guest" element={<PublicationFeedPage memberView={false} />} />
        <Route path="/marketplace" element={<MarketplaceListPage />} />
        <Route path="/marketplace/new" element={<MarketplaceFormPage />} />
        <Route path="/marketplace/:listingId/edit" element={<MarketplaceFormPage edit />} />
        <Route path="/marketplace/:listingId" element={<MarketplaceDetailPage />} />
        <Route path="/lost-found" element={<LostFoundListPage />} />
        <Route path="/lost-found/new" element={<LostFoundAlertFormPage />} />
        <Route path="/lost/new" element={<LostFoundAlertFormPage />} />
        <Route path="/lost-found/:alertId/sightings/new" element={<LostFoundSightingFormPage />} />
        <Route path="/lost-found/sightings/:sightingId/exact" element={<LostFoundExactLocationPage />} />
        <Route path="/lost-found/:alertId" element={<LostFoundDetailPage />} />
        <Route path="*" element={<PlaceholderPage />} />
      </Routes>
    </div>
  );
}
