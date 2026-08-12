import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

afterEach(() => vi.unstubAllGlobals());

describe("TownPet Vite shell", () => {
  it("renders the legacy home identity and primary journeys", () => {
    render(
      <MemoryRouter initialEntries={["/"]}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole("heading", { name: "우리 동네 반려생활 정보" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "전체 피드" })).toHaveAttribute("href", "/feed/guest");
    expect(screen.getByRole("link", { name: "로그인" })).toHaveAttribute("href", "/login");
    expect(screen.getByRole("link", { name: "본문으로 바로가기" })).toHaveAttribute("href", "#main-content");
    expect(screen.getByRole("link", { name: "동물병원" })).toHaveAttribute("href", "/hospital-reviews");
    expect(screen.getByRole("link", { name: "산책코스" })).toHaveAttribute("href", "/guides?q=산책");
    expect(screen.getByRole("link", { name: "질문/답변" })).toHaveAttribute("href", "/gatherings");
  });

  it("surfaces popular community posts on the home page", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn((input) => {
        const path = String(input);
        const body = path.includes("/api/v1/members/me")
          ? { detail: "Unauthorized" }
          : {
              items: [
                {
                  id: "00000000-0000-4000-8000-000000000301",
                  title: "이번 주말 산책 코스 추천받아요",
                  body: "저녁에 걷기 좋은 조용한 코스를 찾고 있어요.",
                  createdAt: "2026-08-12T08:00:00Z",
                  recommendationCount: 4,
                },
              ],
            };
        const status = path.includes("/api/v1/members/me") ? 401 : 200;
        return Promise.resolve(new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } }));
      }),
    );

    render(<MemoryRouter initialEntries={["/"]}><App /></MemoryRouter>);

    expect(await screen.findByRole("heading", { name: "인기글" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "이번 주말 산책 코스 추천받아요" })).toHaveAttribute(
      "href",
      "/posts/00000000-0000-4000-8000-000000000301",
    );
    expect(screen.getByRole("link", { name: "인기글 전체 보기" })).toHaveAttribute("href", "/best");
  });

  it("updates the document title for direct routes", () => {
    render(<MemoryRouter initialEntries={["/marketplace"]}><App /></MemoryRouter>);
    expect(document.title).toBe("TownPet | 동네 거래");
  });

  it("keeps authenticated navigation when visiting public routes", async () => {
    vi.stubGlobal("fetch", vi.fn((input) => {
      const path = String(input);
      const body = path.includes("/api/v1/members/me")
        ? {
            id: "00000000-0000-4000-8000-000000000201",
            nickname: "demo-member-1",
            role: "MEMBER",
            bio: null,
            neighborhoodId: null,
            pets: [],
            showPublicPosts: true,
            showPublicComments: true,
            showPublicPets: true,
            showPublicReactions: true,
          }
        : { items: [], page: { nextCursor: null, hasNext: false } };
      return Promise.resolve(new Response(JSON.stringify(body), { status: 200, headers: { "content-type": "application/json" } }));
    }));

    render(<MemoryRouter initialEntries={["/best"]}><App /></MemoryRouter>);

    expect(await screen.findByRole("link", { name: "내 프로필" })).toHaveAttribute("href", "/profile");
    expect(screen.queryByTestId("header-login-link-home")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "이웃 활동" })).toBeInTheDocument();
    expect(screen.getByRole("menu", { name: "게시판 바로가기" })).toBeInTheDocument();
    const boardMenu = screen.getByRole("button", { name: /게시판/ });
    expect(boardMenu).toHaveAttribute("aria-expanded", "false");
    fireEvent.click(boardMenu);
    expect(boardMenu).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByRole("menuitem", { name: "내 피드" })).toBeVisible();

    fireEvent.keyDown(boardMenu, { key: "ArrowDown" });
    await waitFor(() => expect(screen.getByRole("menuitem", { name: "내 피드" })).toHaveFocus());
    fireEvent.keyDown(screen.getByRole("menuitem", { name: "내 피드" }), { key: "ArrowDown" });
    expect(screen.getByRole("menuitem", { name: "전체 공개 피드" })).toHaveFocus();

    fireEvent.keyDown(boardMenu, { key: "Escape" });
    expect(boardMenu).toHaveAttribute("aria-expanded", "false");
    fireEvent.click(boardMenu);
    fireEvent.mouseDown(document.body);
    expect(boardMenu).toHaveAttribute("aria-expanded", "false");
  });

  it("does not render the moderator console for a member", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() => Promise.resolve(new Response(JSON.stringify({
        id: "00000000-0000-4000-8000-000000000201",
        nickname: "demo-member-1",
        role: "MEMBER",
        bio: null,
        neighborhoodId: null,
        pets: [],
        showPublicPosts: true,
        showPublicComments: true,
        showPublicPets: true,
        showPublicReactions: true,
      }), { status: 200, headers: { "content-type": "application/json" } }))),
    );

    render(<MemoryRouter initialEntries={["/admin"]}><App /></MemoryRouter>);

    expect(await screen.findByRole("heading", { name: "우리 동네 반려생활 정보" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "운영 콘솔" })).not.toBeInTheDocument();
  });

  it("keeps moderator accounts out of member-only routes", async () => {
    vi.stubGlobal("fetch", vi.fn((input) => {
      const path = String(input);
      const body = path.endsWith("/api/v1/members/me")
        ? {
            id: "00000000-0000-4000-8000-000000000002",
            nickname: "moderator-user",
            role: "MODERATOR",
            bio: null,
            neighborhoodId: null,
            pets: [],
            showPublicPosts: true,
            showPublicComments: true,
            showPublicPets: true,
            showPublicReactions: true,
          }
        : { items: [], page: { nextCursor: null, hasNext: false } };
      return Promise.resolve(new Response(JSON.stringify(body), { status: 200, headers: { "content-type": "application/json" } }));
    }));

    render(<MemoryRouter initialEntries={["/onboarding"]}><App /></MemoryRouter>);

    expect(await screen.findByRole("heading", { name: "공개 반려생활 피드" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "내 동네와 반려동물 설정" })).not.toBeInTheDocument();
  });

  it("renders the identity vertical slice login form", () => {
    render(
      <MemoryRouter initialEntries={["/login"]}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole("heading", { name: "로그인" })).toBeInTheDocument();
    expect(screen.getByLabelText("이메일")).toHaveAttribute("type", "email");
    expect(screen.getByLabelText("비밀번호")).toHaveAttribute("type", "password");
    expect(screen.getAllByRole("link", { name: "비밀번호 재설정" })).toHaveLength(2);
    for (const link of screen.getAllByRole("link", { name: "비밀번호 재설정" })) {
      expect(link).toHaveAttribute("href", "/password/reset");
    }
    expect(screen.queryByRole("link", { name: "회원가입" })).not.toBeInTheDocument();
    expect(screen.queryByText(/카카오|네이버/)).not.toBeInTheDocument();
  });

  it("keeps the complete public board menu and interest preferences available to guests", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() => Promise.resolve(new Response(JSON.stringify({ detail: "Unauthorized" }), { status: 401 }))),
    );

    render(<MemoryRouter initialEntries={["/feed/guest"]}><App /></MemoryRouter>);

    expect(screen.getByRole("button", { name: "게시판" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "관심 동물" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "내 프로필" })).toHaveAttribute("href", "/profile");
    fireEvent.click(screen.getByRole("button", { name: "게시판" }));
    expect(screen.getByRole("menuitem", { name: "동물병원 후기" })).toBeInTheDocument();
    expect(screen.getByRole("menuitem", { name: "반려동물 자랑" })).toHaveAttribute("href", "/feed/guest?type=PET_SHOWCASE");

    fireEvent.click(screen.getByRole("button", { name: "관심 동물" }));
    expect(screen.getByRole("dialog", { name: "관심 동물 설정" })).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: "강아지" })).toBeChecked();
    fireEvent.click(screen.getByRole("checkbox", { name: "강아지" }));
    expect(screen.getByRole("checkbox", { name: "강아지" })).not.toBeChecked();
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    expect(await screen.findByText("관심 동물을 저장했습니다.")).toBeInTheDocument();
  });

  it("renders the reset and verification routes", () => {
    const { unmount } = render(
      <MemoryRouter initialEntries={["/password/reset"]}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole("heading", { name: "비밀번호 재설정" })).toBeInTheDocument();
    unmount();

    render(
      <MemoryRouter initialEntries={["/verify-email"]}>
        <App />
      </MemoryRouter>,
    );
    expect(screen.getByRole("heading", { name: "이메일 인증" })).toBeInTheDocument();
  });

  it("redirects an unauthenticated public profile URL to credentials login", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() => Promise.resolve(new Response(JSON.stringify({ detail: "Unauthorized" }), { status: 401 }))),
    );

    render(
      <MemoryRouter initialEntries={["/users/00000000-0000-4000-8000-000000000202"]}>
        <App />
      </MemoryRouter>,
    );

    expect(await screen.findByRole("heading", { name: "로그인" })).toBeInTheDocument();
  });
});
