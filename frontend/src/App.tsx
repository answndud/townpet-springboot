import { Link, Navigate, NavLink, Route, Routes, useLocation, useNavigate, useParams } from "react-router-dom";
import { lazy, KeyboardEvent, ReactNode, Suspense, useEffect, useRef, useState } from "react";
import LoginPage from "./LoginPage";
import PasswordResetPage from "./PasswordResetPage";
import VerifyEmailPage from "./VerifyEmailPage";
import LegalPage from "./LegalPage";
import HomeFeedPage from "./HomeFeedPage";
import { AuthProvider, useAuth } from "./auth/AuthContext";
import AnimalBoardMenu from "./features/member/AnimalBoardMenu";
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
const AnimalCommunityPage = lazy(() => import("./features/community/AnimalCommunityPage"));
const CommonBoardPage = lazy(() => import("./features/community/AnimalCommunityPage").then(({ CommonBoardPage }) => ({ default: CommonBoardPage })));
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
const AdoptionCreatePage = lazy(() => import("./AdoptionCreatePage"));
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
  ["/boards", () => import("./features/community/AnimalCommunityPage")],
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

const COMMON_BOARD_LINKS = [
  ["전체 공통게시판", "/boards/all"],
  ["입양", "/boards/adoption"],
  ["분실·목격", "/boards/lost-found"],
  ["동물병원 후기", "/boards/hospital-reviews"],
  ["동네 모임", "/boards/gatherings"],
  ["동네 거래", "/boards/marketplace"],
  ["이웃 돌봄", "/boards/care"],
  ["봉사 기회", "/boards/volunteer"],
] as const;

const COMMON_BOARD_CODES = new Set(COMMON_BOARD_LINKS.map(([, href]) => href.split("/").pop()));

const MEMBER_ACCOUNT_LINKS = [
  ["내 프로필", "/profile"],
  ["알림", "/notifications"],
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
              <AnimalBoardMenu />
              <HeaderMenu label="공통게시판" links={COMMON_BOARD_LINKS} />
              <NavLink className="desktop-nav-secondary" to="/profile">내 프로필</NavLink>
            </nav>
          ) : (
            <nav aria-label="주요 이동" className="desktop-nav">
              <AnimalBoardMenu />
              <HeaderMenu label="공통게시판" links={COMMON_BOARD_LINKS} />
              <NavLink className="desktop-nav-secondary" to="/profile">내 프로필</NavLink>
              <NavLink className="desktop-nav-secondary" to="/notifications">알림</NavLink>
              <HeaderMenu className="mobile-only-menu" label="더보기" links={MEMBER_ACCOUNT_LINKS} />
            </nav>
          )
        ) : (
          <nav aria-label="공개 안내 페이지 주요 이동" className="desktop-nav">
            <AnimalBoardMenu />
            <HeaderMenu label="공통게시판" links={COMMON_BOARD_LINKS} />
            <NavLink to="/profile">내 프로필</NavLink>
            <NavLink to="/login" data-testid="header-login-link-home">
              로그인
            </NavLink>
          </nav>
        )}
      </div>
    </header>
  );
}

function AnimalBoardRouteAlias() {
  const { boardCode = "" } = useParams();
  return COMMON_BOARD_CODES.has(boardCode) ? (
    <Navigate to={`/boards/${boardCode}`} replace />
  ) : (
    <AnimalCommunityPage />
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

function RootRoute() {
  const { status } = useAuth();

  if (status === "loading") {
    return <main className="page placeholder-page"><section className="surface-card" role="status">피드를 준비하는 중...</section></main>;
  }

  return <HomeFeedPage />;
}

function NotFoundPage() {
  return (
    <main className="page placeholder-page">
      <section className="surface-card">
        <span className="eyebrow">TOWNPET</span>
        <h1>페이지를 찾을 수 없습니다</h1>
        <p>요청한 주소가 없거나 더 이상 공개되지 않습니다.</p>
        <Link className="button button-primary" to="/">
          홈으로 돌아가기
        </Link>
      </section>
    </main>
  );
}

function RoutePerformanceProbe({ path, startedAt }: { path: string; startedAt: number }) {
  useEffect(() => {
    const frame = window.requestAnimationFrame(() => {
      recordRouteTiming(path, performance.now() - startedAt);
    });
    return () => window.cancelAnimationFrame(frame);
  }, [path, startedAt]);
  return null;
}

function AppShell() {
  const location = useLocation();
  const routeKey = `${location.pathname}${location.search}`;
  const routeStartRef = useRef<{ key: string; startedAt: number }>({ key: routeKey, startedAt: typeof performance === "undefined" ? 0 : performance.now() });
  if (routeStartRef.current.key !== routeKey) {
    routeStartRef.current = { key: routeKey, startedAt: typeof performance === "undefined" ? 0 : performance.now() };
  }

  useEffect(() => {
    installPerformanceObservers();
  }, []);

  useEffect(() => {
    const titleByPath: Array<[string, string]> = [
      ["/login", "로그인"],
      ["/password/reset", "비밀번호 재설정"],
      ["/verify-email", "이메일 인증"],
      ["/onboarding", "내 동네 설정"],
      ["/boards", "공통게시판"],
      ["/animals", "동물 게시판"],
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
          <RoutePerformanceProbe path={routeKey} startedAt={routeStartRef.current.startedAt} />
          <Routes>
        <Route path="/" element={<RootRoute />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/password/reset" element={<PasswordResetPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/onboarding" element={<MemberRoute><OnboardingPage /></MemberRoute>} />
        <Route path="/boards" element={<Navigate to="/boards/all" replace />} />
        <Route path="/boards/:boardCode" element={<CommonBoardPage />} />
        <Route path="/animals/all/:boardCode" element={<AnimalBoardRouteAlias />} />
        <Route path="/animals/:animalCode" element={<AnimalCommunityPage />} />
        <Route path="/animals/:animalCode/:boardCode" element={<AnimalBoardRouteAlias />} />
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
        <Route path="/adoptions/new" element={<MemberRoute><AdoptionCreatePage /></MemberRoute>} />
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
        <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </Suspense>
      </div>
    </div>
  );
}

export default function App() {
  return <AuthProvider><AppShell /></AuthProvider>;
}
