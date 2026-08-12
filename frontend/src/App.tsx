import { Link, NavLink, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { lazy, KeyboardEvent, ReactNode, Suspense, useEffect, useRef, useState } from "react";
import LoginPage from "./LoginPage";
import PasswordResetPage from "./PasswordResetPage";
import VerifyEmailPage from "./VerifyEmailPage";
import LegalPage from "./LegalPage";
import { AuthProvider, useAuth } from "./auth/AuthContext";
import { installPerformanceObservers, recordRouteTiming } from "./utils/performance";

const OnboardingPage = lazy(() => import("./OnboardingPage"));
const ProfilePage = lazy(() => import("./ProfilePage"));
const NotificationPage = lazy(() => import("./NotificationPage"));
const AdminPoliciesPage = lazy(() => import("./AdminPoliciesPage"));
const AdminReportsPage = lazy(() => import("./AdminReportsPage"));
const AdminHomePage = lazy(() => import("./AdminHomePage"));
const AdminAuthAuditsPage = lazy(() => import("./AdminAuthAuditsPage"));
const AdminCorrectionPage = lazy(() => import("./AdminCorrectionPage"));
const AdminModerationLogsPage = lazy(() => import("./AdminModerationLogsPage"));
const AdminBreedsPage = lazy(() => import("./AdminBreedsPage"));
const AdminPersonalizationPage = lazy(() => import("./AdminPersonalizationPage"));
const AdminModeratorCasePage = lazy(() => import("./AdminModeratorCasePage"));
const PublicMemberProfilePage = lazy(() => import("./PublicMemberProfilePage"));
const PublicationCreatePage = lazy(() => import("./features/publication/PublicationCreatePage"));
const PublicationDetailPage = lazy(() => import("./features/publication/PublicationDetailPage"));
const PublicationEditPage = lazy(() => import("./features/publication/PublicationEditPage"));
const PublicationFeedPage = lazy(() => import("./features/publication/PublicationFeedPage"));
const GuestPublicationCreatePage = lazy(() => import("./features/publication/GuestPublicationCreatePage"));
const MarketplaceDetailPage = lazy(() => import("./features/marketplace/MarketplacePages").then(({ MarketplaceDetailPage }) => ({ default: MarketplaceDetailPage })));
const MarketplaceFormPage = lazy(() => import("./features/marketplace/MarketplacePages").then(({ MarketplaceFormPage }) => ({ default: MarketplaceFormPage })));
const MarketplaceListPage = lazy(() => import("./features/marketplace/MarketplacePages").then(({ MarketplaceListPage }) => ({ default: MarketplaceListPage })));
const LostFoundAlertFormPage = lazy(() => import("./features/lostfound/LostFoundPages").then(({ LostFoundAlertFormPage }) => ({ default: LostFoundAlertFormPage })));
const LostFoundDetailPage = lazy(() => import("./features/lostfound/LostFoundPages").then(({ LostFoundDetailPage }) => ({ default: LostFoundDetailPage })));
const LostFoundExactLocationPage = lazy(() => import("./features/lostfound/LostFoundPages").then(({ LostFoundExactLocationPage }) => ({ default: LostFoundExactLocationPage })));
const LostFoundListPage = lazy(() => import("./features/lostfound/LostFoundPages").then(({ LostFoundListPage }) => ({ default: LostFoundListPage })));
const LostFoundSightingFormPage = lazy(() => import("./features/lostfound/LostFoundPages").then(({ LostFoundSightingFormPage }) => ({ default: LostFoundSightingFormPage })));
const LocalCareDetailPage = lazy(() => import("./features/localcare/LocalCarePages").then(({ LocalCareDetailPage }) => ({ default: LocalCareDetailPage })));
const LocalCareListPage = lazy(() => import("./features/localcare/LocalCarePages").then(({ LocalCareListPage }) => ({ default: LocalCareListPage })));
const GatheringCreatePage = lazy(() => import("./features/gathering/GatheringPages").then(({ GatheringCreatePage }) => ({ default: GatheringCreatePage })));
const GatheringDetailPage = lazy(() => import("./features/gathering/GatheringPages").then(({ GatheringDetailPage }) => ({ default: GatheringDetailPage })));
const GatheringListPage = lazy(() => import("./features/gathering/GatheringPages").then(({ GatheringListPage }) => ({ default: GatheringListPage })));
const PersonalPostsPage = lazy(() => import("./features/publication/PersonalPostsPage"));
const SearchPage = lazy(() => import("./features/publication/SearchPage"));
const CorrectionCreatePage = lazy(() => import("./CorrectionCreatePage"));
const AdoptionPage = lazy(() => import("./AdoptionPage"));
const AdoptionDetailPage = lazy(() => import("./AdoptionDetailPage"));
const TownLandingPage = lazy(() => import("./TownLandingPage"));
const BreedLoungePage = lazy(() => import("./BreedLoungePage"));
const NeighborhoodMapPage = lazy(() => import("./NeighborhoodMapPage"));
const TownSectionPage = lazy(() => import("./TownSectionPage"));
const PostSightingsPage = lazy(() => import("./PostSightingsPage"));
const BestPage = lazy(() => import("./BestPage"));
const CareCreatePage = lazy(() => import("./features/care/CarePages").then(({ CareCreatePage }) => ({ default: CareCreatePage })));
const CareDetailPage = lazy(() => import("./features/care/CarePages").then(({ CareDetailPage }) => ({ default: CareDetailPage })));
const CareListPage = lazy(() => import("./features/care/CarePages").then(({ CareListPage }) => ({ default: CareListPage })));
const VolunteerPage = lazy(() => import("./VolunteerPage"));
const HospitalReviewPage = lazy(() => import("./HospitalReviewPage"));

const ROUTE_PRELOADERS = new Map<string, () => Promise<unknown>>([
  ["/feed", () => import("./features/publication/PublicationFeedPage")],
  ["/feed/guest", () => import("./features/publication/PublicationFeedPage")],
  ["/best", () => import("./BestPage")],
  ["/boards/adoption", () => import("./AdoptionPage")],
  ["/marketplace", () => import("./features/marketplace/MarketplacePages")],
  ["/lost-found", () => import("./features/lostfound/LostFoundPages")],
  ["/care", () => import("./features/care/CarePages")],
  ["/gatherings", () => import("./features/gathering/GatheringPages")],
  ["/admin", () => import("./AdminHomePage")],
]);

function preloadRoute(href: string) {
  const path = href.split("?", 1)[0];
  void ROUTE_PRELOADERS.get(path)?.();
}

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

const MEMBER_ACCOUNT_LINKS = [
  ["내 프로필", "/profile"],
  ["알림", "/notifications"],
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
  className,
}: {
  label: string;
  links: ReadonlyArray<readonly [string, string]>;
  className?: string;
}) {
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const menuItemsRef = useRef<HTMLAnchorElement[]>([]);

  useEffect(() => {
    setOpen(false);
  }, [location.pathname, location.search]);

  useEffect(() => {
    if (!open) return;
    const closeOnOutsideClick = (event: MouseEvent) => {
      if (event.target instanceof Node && !menuRef.current?.contains(event.target)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", closeOnOutsideClick);
    return () => document.removeEventListener("mousedown", closeOnOutsideClick);
  }, [open]);

  function focusMenuItem(index: number) {
    const items = menuItemsRef.current;
    if (!items.length) return;
    items[(index + items.length) % items.length]?.focus();
  }

  function handleKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (event.key === "Escape") {
      setOpen(false);
      event.currentTarget.focus();
    } else if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      setOpen(true);
      window.setTimeout(() => focusMenuItem(event.key === "ArrowDown" ? 0 : -1), 0);
    }
  }

  function handleMenuKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    const currentIndex = menuItemsRef.current.indexOf(document.activeElement as HTMLAnchorElement);
    if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      focusMenuItem(currentIndex + (event.key === "ArrowDown" ? 1 : -1));
    } else if (event.key === "Home" || event.key === "End") {
      event.preventDefault();
      focusMenuItem(event.key === "Home" ? 0 : -1);
    } else if (event.key === "Escape") {
      event.preventDefault();
      setOpen(false);
      menuRef.current?.querySelector<HTMLButtonElement>(".header-menu-trigger")?.focus();
    }
  }

  return (
    <div ref={menuRef} className={`header-menu${className ? ` ${className}` : ""}${open ? " open" : ""}`}>
      <button
        className="header-menu-trigger"
        type="button"
        aria-haspopup="true"
        aria-expanded={open}
        aria-controls={`${label}-menu`}
        onClick={() => setOpen((current) => !current)}
        onKeyDown={handleKeyDown}
      >
        {label}<span aria-hidden="true">⌄</span>
      </button>
      <div id={`${label}-menu`} className="header-menu-panel" role="menu" aria-label={`${label} 바로가기`} onKeyDown={handleMenuKeyDown}>
        {links.map(([linkLabel, href]) => (
          <NavLink key={href} ref={(element) => { if (element) menuItemsRef.current[links.findIndex(([, linkHref]) => linkHref === href)] = element; }} role="menuitem" to={href} onMouseEnter={() => preloadRoute(href)} onFocus={() => preloadRoute(href)} onClick={() => setOpen(false)}>
            {linkLabel}
          </NavLink>
        ))}
      </div>
    </div>
  );
}

function Header() {
  const { status, member } = useAuth();

  const boardLinks = member ? MEMBER_BOARD_LINKS : PUBLIC_BOARD_LINKS;

  return (
    <header className="site-header">
      <div className="header-inner">
        <Link className="brand" to="/" aria-label="TownPet 홈으로 이동">
          <img src="/townpet-logo.svg" alt="TownPet" />
        </Link>
        {status === "authenticated" && member ? (
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
            <HeaderMenu className="mobile-only-menu" label="더보기" links={MEMBER_ACCOUNT_LINKS} />
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
  const location = useLocation();
  const { status, member } = useAuth();

  useEffect(() => {
    if (status === "anonymous") navigate(`/login?next=${encodeURIComponent(location.pathname + location.search)}`, { replace: true });
    else if (status === "authenticated" && member?.role !== "MODERATOR") navigate("/", { replace: true });
  }, [location.pathname, location.search, member?.role, navigate, status]);

  if (status === "error") return <AuthError />;
  if (status !== "authenticated" || member?.role !== "MODERATOR") {
    return <main className="page placeholder-page"><section className="surface-card" role="status">운영자 권한을 확인하는 중...</section></main>;
  }
  return <>{children}</>;
}

function MemberRoute({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { status, member } = useAuth();

  useEffect(() => {
    if (status === "anonymous") navigate(`/login?next=${encodeURIComponent(location.pathname + location.search)}`, { replace: true });
    else if (status === "authenticated" && member?.role !== "MEMBER") navigate("/feed/guest", { replace: true });
  }, [location.pathname, location.search, member?.role, navigate, status]);

  if (status === "error") return <AuthError />;
  if (status !== "authenticated" || member?.role !== "MEMBER") {
    return <main className="page placeholder-page"><section className="surface-card" role="status">회원 권한을 확인하는 중...</section></main>;
  }
  return <>{children}</>;
}

function NonModeratorRoute({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const { status, member } = useAuth();

  useEffect(() => {
    if (status === "authenticated" && member?.role === "MODERATOR") navigate("/feed/guest", { replace: true });
  }, [member?.role, navigate, status]);

  if (status === "error") return <AuthError />;
  if (status === "loading" || (status === "authenticated" && member?.role === "MODERATOR")) {
    return <main className="page placeholder-page"><section className="surface-card" role="status">접근 권한을 확인하는 중...</section></main>;
  }
  return <>{children}</>;
}

function AuthError() {
  const { refresh } = useAuth();
  return <main className="page placeholder-page"><section className="surface-card"><p className="form-error" role="alert">로그인 상태를 확인하지 못했습니다. 네트워크를 확인한 뒤 다시 시도해 주세요.</p><button className="button button-soft" type="button" onClick={refresh}>다시 시도</button></section></main>;
}

function HomePage() {
  const { member } = useAuth();

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
          {member?.role !== "MODERATOR" ? <Link className="button button-soft" to="/onboarding">내 동네 설정</Link> : null}
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

function AppShell() {
  const location = useLocation();

  useEffect(() => {
    installPerformanceObservers();
  }, []);

  useEffect(() => {
    const startedAt = typeof performance === "undefined" ? 0 : performance.now();
    const frame = window.requestAnimationFrame(() => {
      if (startedAt) recordRouteTiming(`${location.pathname}${location.search}`, performance.now() - startedAt);
    });
    return () => window.cancelAnimationFrame(frame);
  }, [location.pathname, location.search]);

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
        <Suspense fallback={<main className="page placeholder-page"><section className="surface-card" role="status">화면을 준비하는 중...</section></main>}>
          <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/password/reset" element={<PasswordResetPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/onboarding" element={<MemberRoute><OnboardingPage /></MemberRoute>} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/notifications" element={<MemberRoute><NotificationPage /></MemberRoute>} />
        <Route path="/privacy" element={<LegalPage />} />
        <Route path="/terms" element={<LegalPage />} />
        <Route path="/my-posts" element={<MemberRoute><PersonalPostsPage /></MemberRoute>} />
        <Route path="/saved" element={<MemberRoute><PersonalPostsPage saved /></MemberRoute>} />
        <Route path="/bookmarks" element={<MemberRoute><PersonalPostsPage saved /></MemberRoute>} />
        <Route path="/search" element={<SearchPage />} />
        <Route path="/search/guest" element={<SearchPage guest />} />
        <Route path="/corrections/new" element={<MemberRoute><CorrectionCreatePage /></MemberRoute>} />
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
        <Route path="/admin/care-feedbacks" element={<ModeratorRoute><AdminModeratorCasePage initialQueue="care-feedbacks" /></ModeratorRoute>} />
        <Route path="/admin/corrections" element={<ModeratorRoute><AdminCorrectionPage /></ModeratorRoute>} />
        <Route path="/admin/hospital-review-flags" element={<ModeratorRoute><AdminModeratorCasePage initialQueue="hospital-review-flags" /></ModeratorRoute>} />
        <Route path="/admin/moderation-logs" element={<ModeratorRoute><AdminModerationLogsPage /></ModeratorRoute>} />
        <Route path="/admin/moderation/direct" element={<ModeratorRoute><AdminModeratorCasePage initialQueue="moderation/direct" /></ModeratorRoute>} />
        <Route path="/admin/personalization" element={<ModeratorRoute><AdminPersonalizationPage /></ModeratorRoute>} />
        <Route path="/admin/policies" element={<ModeratorRoute><AdminPoliciesPage /></ModeratorRoute>} />
        <Route path="/guides" element={<LocalCareListPage />} />
        <Route path="/guides/:resourceId" element={<LocalCareDetailPage />} />
        <Route path="/campaigns/neighborhood-map" element={<NeighborhoodMapPage />} />
        <Route path="/commercial" element={<MarketplaceListPage />} />
        <Route path="/lounges/breeds/:breedCode/groupbuys/new" element={<MemberRoute><MarketplaceFormPage initialKind="GROUP_BUY" /></MemberRoute>} />
        <Route path="/gatherings" element={<GatheringListPage />} />
        <Route path="/care" element={<CareListPage />} />
        <Route path="/care/new" element={<MemberRoute><CareCreatePage /></MemberRoute>} />
        <Route path="/care/:requestId" element={<CareDetailPage />} />
        <Route path="/gatherings/new" element={<MemberRoute><GatheringCreatePage /></MemberRoute>} />
        <Route path="/gatherings/:gatheringId" element={<GatheringDetailPage />} />
        <Route path="/members/:memberId" element={<PublicMemberProfilePage />} />
        <Route path="/users/:memberId" element={<PublicMemberProfilePage />} />
        <Route path="/posts/new" element={<MemberRoute><PublicationCreatePage /></MemberRoute>} />
        <Route path="/guest/posts/new" element={<NonModeratorRoute><GuestPublicationCreatePage /></NonModeratorRoute>} />
        <Route path="/posts/:publicationId/edit" element={<MemberRoute><PublicationEditPage /></MemberRoute>} />
        <Route path="/posts/:publicationId" element={<PublicationDetailPage />} />
        <Route path="/posts/:publicationId/guest" element={<PublicationDetailPage />} />
        <Route path="/feed" element={<MemberRoute><PublicationFeedPage memberView /></MemberRoute>} />
        <Route path="/feed/guest" element={<PublicationFeedPage memberView={false} />} />
        <Route path="/marketplace" element={<MarketplaceListPage />} />
        <Route path="/marketplace/new" element={<MemberRoute><MarketplaceFormPage /></MemberRoute>} />
        <Route path="/marketplace/:listingId/edit" element={<MemberRoute><MarketplaceFormPage edit /></MemberRoute>} />
        <Route path="/marketplace/:listingId" element={<MarketplaceDetailPage />} />
        <Route path="/lost-found" element={<LostFoundListPage />} />
        <Route path="/lost-found/new" element={<MemberRoute><LostFoundAlertFormPage /></MemberRoute>} />
        <Route path="/lost/new" element={<MemberRoute><LostFoundAlertFormPage /></MemberRoute>} />
        <Route path="/lost-found/:alertId/sightings/new" element={<MemberRoute><LostFoundSightingFormPage /></MemberRoute>} />
        <Route path="/lost-found/sightings/:sightingId/exact" element={<MemberRoute><LostFoundExactLocationPage /></MemberRoute>} />
        <Route path="/lost-found/:alertId" element={<LostFoundDetailPage />} />
        <Route path="/posts/:publicationId/sightings" element={<MemberRoute><PostSightingsPage /></MemberRoute>} />
        <Route path="*" element={<PlaceholderPage />} />
          </Routes>
        </Suspense>
      </div>
    </div>
  );
}

export default function App() {
  return <AuthProvider><AppShell /></AuthProvider>;
}
