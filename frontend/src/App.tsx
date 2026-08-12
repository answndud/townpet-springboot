import { Link, NavLink, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { ReactNode, useEffect, useState } from "react";
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
import GuestPublicationCreatePage from "./features/publication/GuestPublicationCreatePage";
import { MarketplaceDetailPage, MarketplaceFormPage, MarketplaceListPage } from "./features/marketplace/MarketplacePages";
import { LostFoundAlertFormPage, LostFoundDetailPage, LostFoundExactLocationPage, LostFoundListPage, LostFoundSightingFormPage } from "./features/lostfound/LostFoundPages";
import { LocalCareDetailPage, LocalCareListPage } from "./features/localcare/LocalCarePages";
import { GatheringCreatePage, GatheringDetailPage, GatheringListPage } from "./features/gathering/GatheringPages";
import NotificationPage from "./NotificationPage";
import AdminPoliciesPage from "./AdminPoliciesPage";
import LegalPage from "./LegalPage";
import PersonalPostsPage from "./features/publication/PersonalPostsPage";
import SearchPage from "./features/publication/SearchPage";
import AdminReportsPage from "./AdminReportsPage";
import AdminHomePage from "./AdminHomePage";
import AdminAuthAuditsPage from "./AdminAuthAuditsPage";
import AdminCorrectionPage from "./AdminCorrectionPage";
import AdminModerationLogsPage from "./AdminModerationLogsPage";
import CorrectionCreatePage from "./CorrectionCreatePage";
import AdoptionPage from "./AdoptionPage";
import AdoptionDetailPage from "./AdoptionDetailPage";
import TownLandingPage from "./TownLandingPage";
import BreedLoungePage from "./BreedLoungePage";
import NeighborhoodMapPage from "./NeighborhoodMapPage";
import TownSectionPage from "./TownSectionPage";
import PostSightingsPage from "./PostSightingsPage";
import AdminBreedsPage from "./AdminBreedsPage";
import BestPage from "./BestPage";
import AdminPersonalizationPage from "./AdminPersonalizationPage";
import AdminModeratorCasePage from "./AdminModeratorCasePage";
import { CareCreatePage, CareDetailPage, CareListPage } from "./features/care/CarePages";
import VolunteerPage from "./VolunteerPage";
import HospitalReviewPage from "./HospitalReviewPage";
import { ApiError, memberApi } from "./api/client";
import type { Member } from "./api/client";

const TOPIC_LINKS = [
  ["지도 만들기", "/campaigns/neighborhood-map"],
  ["분실/목격", "/lost-found"],
  ["동물병원", "/feed/guest?type=HOSPITAL_REVIEW"],
  ["산책코스", "/feed/guest?type=WALK_ROUTE"],
  ["질문/답변", "/feed/guest?type=QA_QUESTION"],
  ["중고거래", "/marketplace"],
] as const;

const PUBLIC_BOARD_LINKS = [
  ["전체 공개 피드", "/feed/guest"],
  ["인기 게시글", "/best"],
  ["입양", "/boards/adoption"],
  ["분실·목격", "/lost-found"],
] as const;

const MEMBER_BOARD_LINKS = [
  ["내 피드", "/feed"],
  ["전체 공개 피드", "/feed/guest"],
  ["인기 게시글", "/best"],
  ["입양", "/boards/adoption"],
  ["분실·목격", "/lost-found"],
] as const;

const MARKETPLACE_LINKS = [
  ["동네 거래", "/marketplace"],
  ["동물병원 후기", "/hospital-reviews"],
  ["봉사 기회", "/volunteer"],
  ["산책·질문 모임", "/gatherings"],
  ["이웃 돌봄", "/care"],
] as const;

function HeaderMenu({
  label,
  links,
}: {
  label: string;
  links: ReadonlyArray<readonly [string, string]>;
}) {
  const location = useLocation();
  const [open, setOpen] = useState(false);

  useEffect(() => {
    setOpen(false);
  }, [location.pathname, location.search]);

  return (
    <div className={`header-menu${open ? " open" : ""}`}>
      <button
        className="header-menu-trigger"
        type="button"
        aria-haspopup="true"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
      >
        {label}<span aria-hidden="true">⌄</span>
      </button>
      <div className="header-menu-panel" role="menu" aria-label={`${label} 바로가기`}>
        {links.map(([linkLabel, href]) => (
          <NavLink key={href} role="menuitem" to={href} onClick={() => setOpen(false)}>
            {linkLabel}
          </NavLink>
        ))}
      </div>
    </div>
  );
}

function Header() {
  const location = useLocation();
  const [member, setMember] = useState<Member | null>(null);

  useEffect(() => {
    if (["/login", "/password", "/verify-email", "/onboarding"].some((path) => location.pathname === path || location.pathname.startsWith(`${path}/`))) return;
    let active = true;
    let controller: AbortController | null = null;
    const loadMember = () => {
      controller?.abort();
      controller = new AbortController();
      memberApi.current().then((nextMember) => {
        if (active) setMember(nextMember);
      }).catch((requestError: unknown) => {
        if (active && !(requestError instanceof DOMException && requestError.name === "AbortError")) {
          setMember(null);
        }
      });
    };
    const handleAuthChange = () => {
      setMember(null);
      loadMember();
    };
    loadMember();
    window.addEventListener("townpet:auth-change", handleAuthChange);
    return () => {
      active = false;
      controller?.abort();
      window.removeEventListener("townpet:auth-change", handleAuthChange);
    };
  }, [location.pathname]);

  const boardLinks = member ? MEMBER_BOARD_LINKS : PUBLIC_BOARD_LINKS;

  return (
    <header className="site-header">
      <div className="header-inner">
        <Link className="brand" to="/" aria-label="TownPet 홈으로 이동">
          <img src="/townpet-logo.svg" alt="TownPet" />
        </Link>
        {member ? (
          member.role === "MODERATOR" ? (
            <nav aria-label="운영자 주요 이동" className="desktop-nav">
              <NavLink to="/admin">운영 콘솔</NavLink>
              <NavLink to="/feed/guest">공개 피드</NavLink>
            </nav>
          ) : (
            <nav aria-label="주요 이동" className="desktop-nav">
            <HeaderMenu label="게시판" links={boardLinks} />
              <NavLink className="desktop-nav-secondary" to="/profile">내 프로필</NavLink>
              <NavLink className="desktop-nav-secondary" to="/notifications">알림</NavLink>
            <HeaderMenu label="거래" links={MARKETPLACE_LINKS} />
            </nav>
          )
        ) : (
          <nav aria-label="공개 안내 페이지 주요 이동" className="desktop-nav">
            <HeaderMenu label="게시판" links={PUBLIC_BOARD_LINKS} />
            <NavLink to="/login" data-testid="header-login-link-home">
              로그인
            </NavLink>
          </nav>
        )}
      </div>
    </header>
  );
}

function ModeratorRoute({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const [allowed, setAllowed] = useState<boolean | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    memberApi.current(controller.signal)
      .then((member) => {
        if (member.role === "MODERATOR") setAllowed(true);
        else navigate("/", { replace: true });
      })
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        if (requestError instanceof ApiError && requestError.status === 401) {
          navigate("/login?next=/admin", { replace: true });
        } else {
          navigate("/", { replace: true });
        }
      });
    return () => controller.abort();
  }, [navigate]);

  if (allowed !== true) {
    return <main className="page placeholder-page"><section className="surface-card" role="status">운영자 권한을 확인하는 중...</section></main>;
  }
  return <>{children}</>;
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
  const location = useLocation();

  useEffect(() => {
    const titleByPath: Array<[string, string]> = [
      ["/login", "로그인"],
      ["/password/reset", "비밀번호 재설정"],
      ["/verify-email", "이메일 인증"],
      ["/onboarding", "내 동네 설정"],
      ["/notifications", "알림"],
      ["/marketplace", "동네 거래"],
      ["/lost-found", "분실·목격"],
      ["/search", "반려생활 정보 검색"],
      ["/best", "인기 게시글"],
      ["/admin", "운영 콘솔"],
    ];
    const match = titleByPath.find(([path]) => location.pathname === path || location.pathname.startsWith(`${path}/`));
    document.title = match ? `TownPet | ${match[1]}` : location.pathname === "/" ? "TownPet | 우리 동네 반려생활 정보" : "TownPet | 반려생활 커뮤니티";
  }, [location.pathname]);

  return (
    <div className="app-shell-bg">
      <a className="skip-link" href="#main-content">본문으로 바로가기</a>
      <Header />
      <div id="main-content" tabIndex={-1}>
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
        <Route path="/corrections/new" element={<CorrectionCreatePage />} />
        <Route path="/best" element={<BestPage />} />
        <Route path="/boards/adoption" element={<AdoptionPage />} />
        <Route path="/adoptions/:adoptionId" element={<AdoptionDetailPage />} />
        <Route path="/volunteer" element={<VolunteerPage />} />
        <Route path="/hospital-reviews" element={<HospitalReviewPage />} />
        <Route path="/towns/:townSlug" element={<TownLandingPage />} />
        <Route path="/towns/:townSlug/:sectionSlug" element={<TownSectionPage />} />
        <Route path="/lounges/breeds/:breedCode" element={<BreedLoungePage />} />
        <Route path="/admin/reports" element={<ModeratorRoute><AdminReportsPage /></ModeratorRoute>} />
        <Route path="/admin/reports/:reportId" element={<ModeratorRoute><AdminReportsPage /></ModeratorRoute>} />
        <Route path="/admin" element={<ModeratorRoute><AdminHomePage /></ModeratorRoute>} />
        <Route path="/admin/ops" element={<ModeratorRoute><AdminHomePage /></ModeratorRoute>} />
        <Route path="/admin/auth-audits" element={<ModeratorRoute><AdminAuthAuditsPage /></ModeratorRoute>} />
        <Route path="/admin/breeds" element={<ModeratorRoute><AdminBreedsPage /></ModeratorRoute>} />
        <Route path="/admin/care-feedbacks" element={<ModeratorRoute><AdminModeratorCasePage /></ModeratorRoute>} />
        <Route path="/admin/corrections" element={<ModeratorRoute><AdminCorrectionPage /></ModeratorRoute>} />
        <Route path="/admin/hospital-review-flags" element={<ModeratorRoute><AdminModeratorCasePage /></ModeratorRoute>} />
        <Route path="/admin/moderation-logs" element={<ModeratorRoute><AdminModerationLogsPage /></ModeratorRoute>} />
        <Route path="/admin/moderation/direct" element={<ModeratorRoute><AdminModeratorCasePage /></ModeratorRoute>} />
        <Route path="/admin/personalization" element={<ModeratorRoute><AdminPersonalizationPage /></ModeratorRoute>} />
        <Route path="/admin/policies" element={<ModeratorRoute><AdminPoliciesPage /></ModeratorRoute>} />
        <Route path="/guides" element={<LocalCareListPage />} />
        <Route path="/guides/:resourceId" element={<LocalCareDetailPage />} />
        <Route path="/campaigns/neighborhood-map" element={<NeighborhoodMapPage />} />
        <Route path="/commercial" element={<MarketplaceListPage />} />
        <Route path="/lounges/breeds/:breedCode/groupbuys/new" element={<MarketplaceFormPage initialKind="GROUP_BUY" />} />
        <Route path="/gatherings" element={<GatheringListPage />} />
        <Route path="/care" element={<CareListPage />} />
        <Route path="/care/new" element={<CareCreatePage />} />
        <Route path="/care/:requestId" element={<CareDetailPage />} />
        <Route path="/gatherings/new" element={<GatheringCreatePage />} />
        <Route path="/gatherings/:gatheringId" element={<GatheringDetailPage />} />
        <Route path="/members/:memberId" element={<PublicMemberProfilePage />} />
        <Route path="/users/:memberId" element={<PublicMemberProfilePage />} />
        <Route path="/posts/new" element={<PublicationCreatePage />} />
        <Route path="/guest/posts/new" element={<GuestPublicationCreatePage />} />
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
        <Route path="/posts/:publicationId/sightings" element={<PostSightingsPage />} />
        <Route path="*" element={<PlaceholderPage />} />
        </Routes>
      </div>
    </div>
  );
}
