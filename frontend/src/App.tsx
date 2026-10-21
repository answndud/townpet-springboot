import { Link, Navigate, NavLink, Route, Routes, useLocation, useNavigate, useParams } from "react-router-dom";
import { lazy, ReactNode, Suspense, useEffect, useRef, useState } from "react";
import { authApi } from "./api/client";
import LoginPage from "./LoginPage";
import PasswordResetPage from "./PasswordResetPage";
import VerifyEmailPage from "./VerifyEmailPage";
import LegalPage from "./LegalPage";
import HomeFeedPage from "./HomeFeedPage";
import { AuthProvider, useAuth } from "./auth/AuthContext";
import NavigationMenu from "./components/NavigationMenu";
import AnimalBoardMenu from "./features/member/AnimalBoardMenu";
import ErrorBoundary from "./components/ErrorBoundary";
import { installPerformanceObservers, recordRouteTiming } from "./utils/performance";

const OnboardingPage = lazy(() => import("./OnboardingPage"));
const ProfilePage = lazy(() => import("./ProfilePage"));
const NotificationPage = lazy(() => import("./NotificationPage"));
const AdminPoliciesPage = lazy(() => import("./AdminPoliciesPage"));
const AdminReportsPage = lazy(() => import("./AdminReportsPage"));
const AdminCommentModerationPage = lazy(() => import("./AdminCommentModerationPage"));
const AdminHomePage = lazy(() => import("./AdminHomePage"));
const MfaPage = lazy(() => import("./MfaPage"));
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

function Header() {
  const { status, member } = useAuth();
  const navigate = useNavigate();
  const [logoutError, setLogoutError] = useState<string | null>(null);
  const [loggingOut, setLoggingOut] = useState(false);
  const location = useLocation();
  const [panel, setPanel] = useState<"browse" | "account" | null>(null);
  const header = useRef<HTMLElement>(null);
  const browseButton = useRef<HTMLButtonElement>(null);
  const accountButton = useRef<HTMLButtonElement>(null);
  const openedBy = useRef<HTMLButtonElement | null>(null);
  const authenticated = status === "authenticated" && member;
  useEffect(() => {
    let mobile = window.innerWidth <= 760;
    const close = () => {
      const next = window.innerWidth <= 760;
      if (next !== mobile) { setPanel(null); mobile = next; }
    };
    window.addEventListener("resize", close);
    return () => window.removeEventListener("resize", close);
  }, []);
  useEffect(() => { setPanel(null); }, [location.pathname, location.search]);
  useEffect(() => {
    if (!panel) return;
    const container = header.current?.querySelector<HTMLElement>(panel === "browse" ? "#mobile-navigation" : "#account-navigation");
    container?.querySelector<HTMLElement>("a,button")?.focus();
    function keydown(event: globalThis.KeyboardEvent) {
      if (event.key === "Escape") { setPanel(null); openedBy.current?.focus(); }
      if (event.key === "Tab" && container) {
        const items = Array.from(container.querySelectorAll<HTMLElement>("a,button"));
        const first = items[0], last = items.at(-1);
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
      }
    }
    function outside(event: PointerEvent) { if (!header.current?.contains(event.target as Node)) setPanel(null); }
    document.addEventListener("keydown", keydown);
    document.addEventListener("pointerdown", outside);
    return () => { document.removeEventListener("keydown", keydown); document.removeEventListener("pointerdown", outside); };
  }, [panel]);
  function toggle(next: "browse" | "account", button: HTMLButtonElement | null) { openedBy.current = button; setPanel(current => current === next ? null : next); }
  return <header ref={header} className="site-header">
    <div className="header-inner">
      <Link className="brand" to="/" aria-label="TownPet 홈으로 이동">TownPet</Link>
      <nav aria-label="주요 이동" className="desktop-nav">
        <NavLink to="/">홈</NavLink><AnimalBoardMenu /><NavigationMenu label="공통게시판" links={COMMON_BOARD_LINKS} onIntent={preloadRoute} /><NavLink to="/guides">지역 가이드</NavLink>
      </nav>
      <div className="header-account">
        {authenticated ? <button ref={accountButton} className="account-toggle" type="button" aria-expanded={panel === "account"} aria-controls="account-navigation" onClick={() => toggle("account",accountButton.current)}>내 계정</button> : <Link className="account-login" to="/login" data-testid="header-login-link-home">로그인</Link>}
        <button ref={browseButton} className="mobile-menu-toggle" type="button" aria-expanded={panel === "browse"} aria-controls="mobile-navigation" onClick={() => toggle("browse",browseButton.current)}>{panel === "browse" ? "닫기" : "메뉴"}<span aria-hidden="true">{panel === "browse" ? "×" : "☰"}</span></button>
      </div>
    </div>
    {panel === "account" && authenticated ? <nav id="account-navigation" className="account-navigation" aria-label="내 계정">
      {MEMBER_ACCOUNT_LINKS.filter(([,href]) => member.role === "MEMBER" || href === "/profile").map(([label,href]) => <Link key={href} to={href}>{label}</Link>)}
      {member.role === "MEMBER" ? <><Link to="/my-posts">내 글</Link><Link to="/saved">저장한 글</Link></> : null}
      {member.role === "MODERATOR" ? <Link to="/admin">운영 콘솔</Link> : null}
      <button className="account-logout" type="button" disabled={loggingOut} onClick={async () => {
        setLoggingOut(true); setLogoutError(null);
        try { await authApi.logout(); setPanel(null); navigate("/login"); }
        catch { setLogoutError("로그아웃 실패"); }
        finally { setLoggingOut(false); }
      }}>{loggingOut ? "로그아웃 중…" : "로그아웃"}</button>
      {logoutError ? <p className="form-error" role="alert">{logoutError}</p> : null}
    </nav> : null}
    {panel === "browse" ? <nav id="mobile-navigation" className="mobile-navigation" aria-label="모바일 주요 이동">
      <div><strong>게시판</strong><Link to="/">홈</Link><Link to="/animals/all">동물 게시판</Link><Link to="/boards/all">전체 공통게시판</Link></div>
      <div><strong>지역 생활</strong><Link to="/marketplace">동네 거래</Link><Link to="/lost-found">분실·목격</Link><Link to="/gatherings">동네 모임</Link><Link to="/care">이웃 돌봄</Link><Link to="/boards/adoption">입양</Link><Link to="/hospital-reviews">병원 후기</Link><Link to="/volunteer">봉사</Link><Link to="/guides">지역 가이드</Link></div>
    </nav> : null}
  </header>;
}

function Footer() {
  return (
    <footer className="site-footer">
      <div className="header-inner">
        <div className="footer-brand"><strong>TownPet</strong><span>반려동물 커뮤니티</span><small>합성 데이터 · 포트폴리오</small></div>
        <nav aria-label="서비스 안내">
          <Link to="/terms">이용약관</Link>
          <Link to="/privacy">개인정보처리방침</Link>
        </nav>
      </div>
    </footer>
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
    else if (status === "authenticated" && member?.role !== "MEMBER") navigate("/", { replace: true });
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
    if (status === "authenticated" && member?.role === "MODERATOR") navigate("/", { replace: true });
  }, [member?.role, navigate, status]);

  if (status === "error") return <AuthError />;
  if (status === "loading" || (status === "authenticated" && member?.role === "MODERATOR")) {
    return <main className="page placeholder-page"><section className="surface-card" role="status">접근 권한을 확인하는 중...</section></main>;
  }
  return <>{children}</>;
}

function AuthError() {
  const { refresh } = useAuth();
  return <main className="page placeholder-page"><section className="surface-card"><p className="form-error" role="alert">로그인 상태 확인 실패</p><button className="button button-soft" type="button" onClick={refresh}>다시 시도</button></section></main>;
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

        <h1>페이지를 찾을 수 없습니다</h1>
        <p>주소 없음 또는 비공개</p>
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

const SEO_BASE_URL = "https://townpet.cloud";
const PRIVATE_ROUTE_PREFIXES = [
  "/admin", "/profile", "/notifications", "/my-posts", "/saved", "/bookmarks",
  "/login", "/password", "/verify-email", "/onboarding", "/corrections/new",
  "/adoptions/new", "/marketplace/new", "/gatherings/new", "/care/new",
  "/lost-found/new", "/lost/new", "/posts/new", "/guest/posts/new",
  "/campaigns/neighborhood-map",
];
const SEARCH_PATHS = new Set(["/search", "/search/guest"]);
const UTILITY_NOINDEX_PATHS = new Set(["/terms", "/privacy"]);
const CANONICAL_ALIASES: Record<string, string> = {
  "/commercial": "/marketplace",
  "/users": "/members",
  "/lost/new": "/lost-found/new",
};

function upsertMeta(attribute: "name" | "property", key: string, content: string) {
  let element = document.head.querySelector<HTMLMetaElement>(`meta[${attribute}="${key}"]`);
  if (!element) {
    element = document.createElement("meta");
    element.setAttribute(attribute, key);
    document.head.appendChild(element);
  }
  element.content = content;
}

function applySeo(location: { pathname: string; search: string }) {
  const rawPath = location.pathname || "/";
  const canonicalPath = (Object.entries(CANONICAL_ALIASES).find(([alias]) => rawPath === alias || rawPath.startsWith(`${alias}/`))
    ? rawPath.replace(/^\/commercial(?=\/|$)/, "/marketplace").replace(/^\/users(?=\/|$)/, "/members").replace(/^\/lost\/new$/, "/lost-found/new")
    : rawPath).replace(/\/$/, "") || "/";
  const isPrivate = PRIVATE_ROUTE_PREFIXES.some((prefix) => rawPath === prefix || rawPath.startsWith(`${prefix}/`));
  const isSearch = SEARCH_PATHS.has(rawPath);
  const isUtility = UTILITY_NOINDEX_PATHS.has(rawPath);
  const hasQuery = new URLSearchParams(location.search).size > 0;
  const noindex = isPrivate || isSearch || isUtility || hasQuery || rawPath === "/404";
  const titleByPath: Array<[string, string, string]> = [
    ["/", "우리 동네 반려생활 정보", "TownPet 공개 커뮤니티"],
    ["/terms", "이용약관", "TownPet 이용약관"],
    ["/privacy", "개인정보 처리방침", "TownPet 개인정보 처리방침"],
    ["/best", "인기 게시글", "추천 게시글"],
    ["/boards", "공개 커뮤니티 게시판", "반려생활 공개 게시판"],
    ["/marketplace", "동네 거래", "반려동물 용품 거래·나눔"],
    ["/lost-found", "분실·목격", "반려동물 분실·목격 정보"],
    ["/guides", "지역 가이드", "지역 병원·산책·복지 정보"],
    ["/gatherings", "동네 모임", "지역 반려생활 모임"],
    ["/care", "이웃 돌봄", "반려동물 돌봄 정보"],
    ["/volunteer", "봉사 기회", "반려동물·지역 봉사 기회"],
    ["/hospital-reviews", "동물병원 후기", "동물병원 이용 후기"],
  ];
  const dynamicTitleByPath: Array<[RegExp, string, string]> = [
    [/^\/boards(?:\/|$)/, "공개 커뮤니티 게시판", "반려생활 공개 게시판"],
    [/^\/animals(?:\/|$)/, "반려동물 게시판", "동물별 커뮤니티 게시판"],
    [/^\/towns(?:\/|$)/, "지역 반려생활", "지역별 입양·거래·가이드"],
    [/^\/lounges\/breeds(?:\/|$)/, "품종 커뮤니티", "품종별 커뮤니티 게시글"],
    [/^\/posts\/[^/]+$/, "공개 게시글", "반려생활 커뮤니티 게시글"],
    [/^\/adoptions\/[^/]+$/, "입양 정보", "반려동물 입양 정보"],
    [/^\/marketplace\/[^/]+$/, "동네 거래 정보", "반려동물 용품 거래·나눔"],
    [/^\/lost-found\/[^/]+$/, "분실·목격 정보", "반려동물 분실·목격 정보"],
    [/^\/gatherings\/[^/]+$/, "동네 모임 정보", "지역 반려생활 모임"],
    [/^\/care\/[^/]+$/, "이웃 돌봄 정보", "반려동물 돌봄 요청"],
    [/^\/guides\/[^/]+$/, "지역 가이드", "지역 병원·산책·복지 정보"],
    [/^\/members\/[^/]+$/, "공개 회원 프로필", "TownPet 공개 회원 프로필"],
    [/^\/users\/[^/]+$/, "공개 회원 프로필", "TownPet 공개 회원 프로필"],
  ];
  const match = titleByPath.find(([path]) => rawPath === path || (path !== "/" && rawPath.startsWith(`${path}/`)));
  const dynamicMatch = dynamicTitleByPath.find(([path]) => path.test(rawPath));
  const title = match?.[1] ?? (noindex ? "TownPet" : "반려생활 커뮤니티");
  const resolvedTitle = dynamicMatch?.[1] ?? title;
  const description = dynamicMatch?.[2] ?? match?.[2] ?? "TownPet 공개 커뮤니티";
  document.title = `TownPet | ${resolvedTitle}`;
  upsertMeta("name", "description", description);
  upsertMeta("name", "robots", noindex ? "noindex,nofollow" : "index,follow");
  upsertMeta("property", "og:title", document.title);
  upsertMeta("property", "og:description", description);
  upsertMeta("property", "og:url", `${SEO_BASE_URL}${canonicalPath}`);
  upsertMeta("property", "og:locale", "ko_KR");
  upsertMeta("property", "og:image:alt", "TownPet 공개 반려생활 커뮤니티");
  upsertMeta("name", "twitter:card", "summary");
  upsertMeta("name", "twitter:title", document.title);
  upsertMeta("name", "twitter:description", description);
  upsertMeta("name", "twitter:image", `${SEO_BASE_URL}/townpet-logo.svg`);
  let canonical = document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
  if (!canonical) {
    canonical = document.createElement("link");
    canonical.rel = "canonical";
    document.head.appendChild(canonical);
  }
  canonical.href = `${SEO_BASE_URL}${canonicalPath}`;
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

  useEffect(() => applySeo(location), [location.pathname, location.search]);

  return (
    <div className="app-shell-bg">
      <a className="skip-link" href="#main-content">본문으로 바로가기</a>
      <Header />
      <div id="main-content" tabIndex={-1}>
      <Suspense fallback={<main className="page placeholder-page"><section className="surface-card" role="status">화면을 준비하는 중...</section></main>}>
        <ErrorBoundary>
          <RoutePerformanceProbe path={routeKey} startedAt={routeStartRef.current.startedAt} />
          <Routes>
        <Route path="/" element={<RootRoute />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/password/reset" element={<PasswordResetPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/admin/mfa" element={<ModeratorRoute><MfaPage /></ModeratorRoute>} />
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
        <Route path="/admin/comments" element={<ModeratorRoute><AdminCommentModerationPage /></ModeratorRoute>} />
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
      </ErrorBoundary>
      </Suspense>
      </div>
      <Footer />
    </div>
  );
}

export default function App() {
  return <AuthProvider><AppShell /></AuthProvider>;
}
