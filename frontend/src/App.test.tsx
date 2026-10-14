import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

afterEach(() => {
  window.localStorage.clear();
  vi.unstubAllGlobals();
});

describe("TownPet Vite shell", () => {

  it("does not render the moderator console for a member", async () => {
    vi.stubGlobal("fetch", vi.fn((input) => {
      const path = String(input);
      const body = path.endsWith("/api/v1/members/me")
        ? { id: "00000000-0000-4000-8000-000000000201", nickname: "demo-member-1", role: "MEMBER", bio: null, neighborhoodId: null, pets: [], showPublicPosts: true, showPublicComments: true, showPublicPets: true, showPublicReactions: true }
        : { items: [], page: { nextCursor: null, hasNext: false } };
      return Promise.resolve(new Response(JSON.stringify(body), { status: 200, headers: { "content-type": "application/json" } }));
    }));

    render(<MemoryRouter initialEntries={["/admin"]}><App /></MemoryRouter>);

    expect(await screen.findByRole("heading", { name: "HOT 글" })).toBeInTheDocument();
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

    expect(await screen.findByRole("heading", { name: "HOT 글" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "내 동네와 반려동물 설정" })).not.toBeInTheDocument();
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
